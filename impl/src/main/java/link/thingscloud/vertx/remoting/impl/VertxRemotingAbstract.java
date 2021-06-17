/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package link.thingscloud.vertx.remoting.impl;

import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import link.thingscloud.vertx.remoting.api.*;
import link.thingscloud.vertx.remoting.api.channel.ChannelEventListener;
import link.thingscloud.vertx.remoting.api.channel.RemotingChannel;
import link.thingscloud.vertx.remoting.api.command.RemotingCommand;
import link.thingscloud.vertx.remoting.api.command.RemotingCommandFactory;
import link.thingscloud.vertx.remoting.api.command.TrafficType;
import link.thingscloud.vertx.remoting.api.exception.RemotingAccessException;
import link.thingscloud.vertx.remoting.api.exception.RemotingRuntimeException;
import link.thingscloud.vertx.remoting.api.exception.RemotingTimeoutException;
import link.thingscloud.vertx.remoting.api.exception.SemaphoreExhaustedException;
import link.thingscloud.vertx.remoting.api.interceptor.Interceptor;
import link.thingscloud.vertx.remoting.api.interceptor.InterceptorGroup;
import link.thingscloud.vertx.remoting.api.interceptor.RequestContext;
import link.thingscloud.vertx.remoting.api.interceptor.ResponseContext;
import link.thingscloud.vertx.remoting.common.ChannelEventListenerGroup;
import link.thingscloud.vertx.remoting.common.Pair;
import link.thingscloud.vertx.remoting.common.ResponseFuture;
import link.thingscloud.vertx.remoting.common.SemaphoreReleaseOnlyOnce;
import link.thingscloud.vertx.remoting.config.RemotingConfig;
import link.thingscloud.vertx.remoting.external.ThreadUtils;
import link.thingscloud.vertx.remoting.impl.command.RemotingCommandFactoryImpl;
import link.thingscloud.vertx.remoting.impl.command.RemotingSysResponseCode;
import link.thingscloud.vertx.remoting.internal.RemotingUtil;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * @author zhouhailin
 * @since 1.0.0
 */
public abstract class VertxRemotingAbstract implements RemotingService {


    /**
     * Semaphore to limit maximum number of on-going one-way requests, which protects system memory footprint.
     */
    private final Semaphore semaphoreOneway;

    /**
     * Semaphore to limit maximum number of on-going asynchronous requests, which protects system memory footprint.
     */
    private final Semaphore semaphoreAsync;

    /**
     * Executor to execute RequestProcessor without specific executor.
     */
    private final ExecutorService publicExecutor;
    /**
     * Invoke the async handler in this executor when process response.
     */
    private final ExecutorService asyncHandlerExecutor;

    private final RemotingCommandFactory remotingCommandFactory;

    /**
     * This scheduled executor provides the ability to govern on-going response table.
     */
    protected ScheduledExecutorService houseKeepingService = ThreadUtils.newSingleThreadScheduledExecutor("HouseKeepingService", true);

    /**
     * Executor to feed netty events to user defined {@link ChannelEventListener}.
     */
    protected final ChannelEventExecutor channelEventExecutor = new ChannelEventExecutor("ChannelEventExecutor");

    /**
     * Provides custom interceptor at the occurrence of beforeRequest and afterResponseReceived event.
     */
    private final InterceptorGroup interceptorGroup = new InterceptorGroup();

    /**
     * Provides listener mechanism to handle netty channel events.
     */
    private ChannelEventListenerGroup channelEventListenerGroup = new ChannelEventListenerGroup();

    /**
     * This map caches all on-going requests.
     */
    private final Map<Integer, ResponseFuture> ackTables = new ConcurrentHashMap<Integer, ResponseFuture>(256);

    /**
     * This container holds all processors per request code, aka, for each incoming request, we may look up the
     * responding processor in this map to handle the request.
     */
    private final Map<String, Map<Integer, Pair<RequestProcessor, ExecutorService>>> processorTables = new ConcurrentHashMap<>();

    /**
     * Remoting logger instance.
     */
    protected static final Logger LOG = LoggerFactory.getLogger(VertxRemotingAbstract.class);

    public VertxRemotingAbstract(RemotingConfig config) {
        this.semaphoreOneway = new Semaphore(config.getOnewayInvokeSemaphore(), true);
        this.semaphoreAsync = new Semaphore(config.getAsyncInvokeSemaphore(), true);

        this.publicExecutor = ThreadUtils.newFixedThreadPool(config.getPublicExecutorThreads(), 10000, "Remoting-PublicExecutor", true);
        this.asyncHandlerExecutor = ThreadUtils.newFixedThreadPool(config.getAsyncHandlerExecutorThreads(), 10000, "Remoting-AsyncExecutor", true);
        this.remotingCommandFactory = new RemotingCommandFactoryImpl();
    }

    protected void putNettyEvent(final NettyChannelEvent event) {
        if (channelEventListenerGroup != null && channelEventListenerGroup.size() != 0) {
            this.channelEventExecutor.putNettyEvent(event);
        }
    }


    @Override
    public void registerChannelEventListener(ChannelEventListener listener) {
        channelEventListenerGroup.registerChannelEventListener(listener);
    }

    @Override
    public void start() {
        startUpHouseKeepingService();

        if (this.channelEventListenerGroup.size() > 0) {
            this.channelEventExecutor.start();
        }
    }

    @Override
    public void stop() {
        ThreadUtils.shutdownGracefully(houseKeepingService, 3000, TimeUnit.MILLISECONDS);
        ThreadUtils.shutdownGracefully(publicExecutor, 2000, TimeUnit.MILLISECONDS);
        ThreadUtils.shutdownGracefully(asyncHandlerExecutor, 2000, TimeUnit.MILLISECONDS);
        ThreadUtils.shutdownGracefully(channelEventExecutor);
    }

    @Override
    public void registerInterceptor(Interceptor interceptor) {
        interceptorGroup.registerInterceptor(interceptor);
    }

    @Override
    public void registerRequestProcessor(String uri, int requestCode, RequestProcessor processor) {
        this.registerRequestProcessor(uri, requestCode, processor, publicExecutor);
    }

    @Override
    public void registerRequestProcessor(String uri, int requestCode, RequestProcessor processor, ExecutorService executor) {
        Map<Integer, Pair<RequestProcessor, ExecutorService>> tables = processorTables.computeIfAbsent(uri, s -> new ConcurrentHashMap<>(8));
        if (!tables.containsKey(requestCode)) {
            tables.put(requestCode, new Pair<>(processor, executor));
        }
    }

    @Override
    public void unregisterRequestProcessor(String uri, int requestCode) {
        Map<Integer, Pair<RequestProcessor, ExecutorService>> tables = processorTables.get(uri);
        if (tables != null) {
            tables.remove(requestCode);
        }
    }

    @Override
    public Map<Integer, Pair<RequestProcessor, ExecutorService>> processor(String uri) {
        return processorTables.getOrDefault(uri, Collections.emptyMap());
    }

    @Override
    public Pair<RequestProcessor, ExecutorService> processor(String uri, int requestCode) {
        return processorTables.getOrDefault(uri, Collections.emptyMap()).get(requestCode);
    }

    @Override
    public RemotingCommandFactory commandFactory() {
        return remotingCommandFactory;
    }

    public void invokeAsyncWithInterceptor(final RemotingChannel channel, final RemotingCommand request, final AsyncHandler asyncHandler, long timeoutMillis) {
        request.trafficType(TrafficType.REQUEST_ASYNC);
        final String remoteAddr = RemotingUtil.extractRemoteAddress(channel);
        this.interceptorGroup.beforeRequest(new RequestContext(RemotingEndPoint.REQUEST, remoteAddr, request));
        this.invokeAsync0(remoteAddr, channel, request, asyncHandler, timeoutMillis);
    }

    private void invokeAsync0(final String remoteAddr, final RemotingChannel channel, final RemotingCommand request, final AsyncHandler asyncHandler, final long timeoutMillis) {
        boolean acquired = this.semaphoreAsync.tryAcquire();
        if (acquired) {
            final int requestID = request.requestID();
            SemaphoreReleaseOnlyOnce once = new SemaphoreReleaseOnlyOnce(this.semaphoreAsync);
            final ResponseFuture responseFuture = new ResponseFuture(requestID, timeoutMillis, asyncHandler, once);
            responseFuture.setRequestCommand(request);
            responseFuture.setRemoteAddr(remoteAddr);

            this.ackTables.put(requestID, responseFuture);
            try {
                channel.reply(request, future -> {
                    responseFuture.setSendRequestOK(future.succeeded());
                    if (future.succeeded()) {
                        return;
                    }
                    requestFail(requestID, new RemotingAccessException(RemotingUtil.extractRemoteAddress(channel), future.cause()));
                    LOG.warn(String.format("Send request command to channel %s failed.", remoteAddr));
                });
            } catch (Exception e) {
                requestFail(requestID, new RemotingAccessException(RemotingUtil.extractRemoteAddress(channel), e));
                LOG.error("Send request command to channel " + channel + " error !", e);
            }
        } else {
            String info = String.format("No available async semaphore to issue the request request %s", request.toString());
            requestFail(new ResponseFuture(request.requestID(), timeoutMillis, asyncHandler, null), new SemaphoreExhaustedException(info));
            LOG.error(info);
        }
    }

    public void invokeOnewayWithInterceptor(final RemotingChannel channel, final RemotingCommand request) {
        request.trafficType(TrafficType.REQUEST_ONEWAY);

        this.interceptorGroup.beforeRequest(new RequestContext(RemotingEndPoint.REQUEST, RemotingUtil.extractRemoteAddress(channel), request));
        this.invokeOneway0(channel, request);
    }

    private void invokeOneway0(final RemotingChannel channel, final RemotingCommand request) {
        boolean acquired = this.semaphoreOneway.tryAcquire();
        if (acquired) {
            final SemaphoreReleaseOnlyOnce once = new SemaphoreReleaseOnlyOnce(this.semaphoreOneway);
            try {
                final SocketAddress socketAddress = channel.remoteAddress();

                channel.reply(request, future -> {
                    once.release();
                    if (!future.succeeded()) {
                        LOG.warn(String.format("Send request command to channel %s failed !", socketAddress));
                    }
                });
            } catch (Exception e) {
                once.release();
                LOG.error("Send request command to channel " + channel + " error !", e);
            }
        } else {
            String info = String.format("No available oneway semaphore to issue the request %s", request.toString());
            LOG.error(info);
        }
    }

    protected void processMessageReceived(RemotingHandlerContext ctx, RemotingCommand command) {
        if (command != null) {
            switch (command.trafficType()) {
                case REQUEST_ONEWAY:
                case REQUEST_ASYNC:
                case REQUEST_SYNC:
                    processRequestCommand(ctx, command);
                    break;
                case RESPONSE:
                    processResponseCommand(ctx, command);
                    break;
                default:
                    LOG.warn(String.format("The traffic type %s is NOT supported!", command.trafficType()));
                    break;
            }
        }
    }

    private void startUpHouseKeepingService() {
        this.houseKeepingService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                scanResponseTable();
            }
        }, 3000, 1000, TimeUnit.MICROSECONDS);
    }

    private void scanResponseTable() {
        final List<Integer> rList = new ArrayList<>();

        for (final Map.Entry<Integer, ResponseFuture> next : this.ackTables.entrySet()) {
            ResponseFuture responseFuture = next.getValue();

            if ((responseFuture.getBeginTimestamp() + responseFuture.getTimeoutMillis()) <= System.currentTimeMillis()) {
                rList.add(responseFuture.getRequestId());
            }
        }

        for (Integer requestID : rList) {
            ResponseFuture rf = this.ackTables.remove(requestID);

            if (rf != null) {
                LOG.warn("Removes timeout request " + rf.getRequestCommand());
                rf.setCause(new RemotingTimeoutException(String.format("Request to %s timeout", rf.getRemoteAddr()), rf.getTimeoutMillis()));
                executeAsyncHandler(rf);
            }
        }
    }

    public void processRequestCommand(final RemotingHandlerContext ctx, final RemotingCommand cmd) {
        Map<Integer, Pair<RequestProcessor, ExecutorService>> tables = processorTables.get(ctx.uri());
        if (tables == null) {
            final RemotingCommand response = commandFactory().createResponse(cmd);
            response.opCode(RemotingSysResponseCode.REQUEST_CODE_NOT_SUPPORTED);
            ctx.reply(response);
            LOG.warn(String.format("The uri %s is NOT supported!", ctx.uri()));
            return;
        }
        Pair<RequestProcessor, ExecutorService> processorExecutorPair = tables.get(cmd.cmdCode());
        if (processorExecutorPair == null) {
            final RemotingCommand response = commandFactory().createResponse(cmd);
            response.opCode(RemotingSysResponseCode.REQUEST_CODE_NOT_SUPPORTED);
            ctx.reply(response);
            LOG.warn(String.format("The command code %s is NOT supported!", cmd.cmdCode()));
            return;
        }

//        RemotingChannel channel = new NettyChannelImpl(ctx.channel());

        Runnable run = buildProcessorTask(ctx, cmd, processorExecutorPair);

        try {
            processorExecutorPair.getRight().submit(run);
        } catch (RejectedExecutionException e) {
            LOG.warn(String.format("Request %s from %s is rejected by server executor %s !", cmd, RemotingUtil.extractRemoteAddress(ctx.channel()), processorExecutorPair.getRight().toString()));

            if (cmd.trafficType() != TrafficType.REQUEST_ONEWAY) {
                RemotingCommand response = remotingCommandFactory.createResponse(cmd);
                response.opCode(RemotingSysResponseCode.SYSTEM_BUSY);
                response.remark("SYSTEM_BUSY");
                ctx.reply(response);
            }
        }
    }

    private void processResponseCommand(RemotingHandlerContext ctx, RemotingCommand response) {
        final ResponseFuture responseFuture = ackTables.remove(response.requestID());
        if (responseFuture != null) {
            responseFuture.setResponseCommand(response);
            responseFuture.release();

            this.interceptorGroup.afterResponseReceived(new ResponseContext(RemotingEndPoint.REQUEST, RemotingUtil.extractRemoteAddress(ctx.channel()), responseFuture.getRequestCommand(), response));

            if (responseFuture.getAsyncHandler() != null) {
                executeAsyncHandler(responseFuture);
            } else {
                responseFuture.putResponse(response);
                responseFuture.release();
            }
        } else {
            LOG.warn(String.format("Response %s from %s doesn't have a matched request!", response, RemotingUtil.extractRemoteAddress(ctx.channel())));
        }
    }

    private Runnable buildProcessorTask(final RemotingHandlerContext ctx, final RemotingCommand cmd, final Pair<RequestProcessor, ExecutorService> processorExecutorPair) {
        return () -> {
            try {
                interceptorGroup.beforeRequest(new RequestContext(RemotingEndPoint.RESPONSE, RemotingUtil.extractRemoteAddress(ctx.channel()), cmd));
                RemotingCommand response = processorExecutorPair.getLeft().processRequest(ctx.channel(), cmd);
                interceptorGroup.afterResponseReceived(new ResponseContext(RemotingEndPoint.RESPONSE, RemotingUtil.extractRemoteAddress(ctx.channel()), cmd, response));
                handleResponse(response, cmd, ctx);
            } catch (Throwable e) {
                LOG.error(String.format("Process request %s error !", cmd.toString()), e);
                handleException(e, cmd, ctx);
            }
        };
    }

    /**
     * Execute callback in callback executor. If callback executor is null, run directly in current thread
     */
    private void executeAsyncHandler(final ResponseFuture responseFuture) {
        boolean runInThisThread = false;
        ExecutorService executor = asyncHandlerExecutor;
        if (executor != null) {
            try {
                executor.submit(() -> {
                    try {
                        responseFuture.executeAsyncHandler();
                    } catch (Throwable e) {
                        LOG.warn("Execute async handler in specific executor exception, ", e);
                    } finally {
                        responseFuture.release();
                    }
                });
            } catch (Throwable e) {
                runInThisThread = true;
                LOG.warn("Execute async handler in executor exception, maybe the executor is busy now", e);
            }
        } else {
            runInThisThread = true;
        }

        if (runInThisThread) {
            try {
                responseFuture.executeAsyncHandler();
            } catch (Throwable e) {
                LOG.warn("Execute async handler in current thread exception", e);
            } finally {
                responseFuture.release();
            }
        }
    }

    private void requestFail(final int requestID, final RemotingRuntimeException cause) {
        ResponseFuture responseFuture = ackTables.remove(requestID);
        if (responseFuture != null) {
            responseFuture.setSendRequestOK(false);
            responseFuture.putResponse(null);
            responseFuture.setCause(cause);
            executeAsyncHandler(responseFuture);
        }
    }

    private void requestFail(final ResponseFuture responseFuture, final RemotingRuntimeException cause) {
        responseFuture.setCause(cause);
        executeAsyncHandler(responseFuture);
    }

    private void handleResponse(RemotingCommand response, RemotingCommand cmd, RemotingHandlerContext ctx) {
        if (cmd.trafficType() != TrafficType.REQUEST_ONEWAY) {
            if (response != null) {
                try {
                    ctx.reply(response);
                } catch (Throwable e) {
                    LOG.error(String.format("Process request %s success, but transfer response %s failed !", cmd, response), e);
                }
            }
        }

    }

    private void handleException(Throwable e, RemotingCommand cmd, RemotingHandlerContext ctx) {
        if (cmd.trafficType() != TrafficType.REQUEST_ONEWAY) {
            RemotingCommand response = remotingCommandFactory.createResponse(cmd);
            response.opCode(RemotingSysResponseCode.SYSTEM_ERROR);
            response.remark("SYSTEM_ERROR");
            ctx.reply(response);
        }
    }

    class ChannelEventExecutor extends Thread {
        private final static int MAX_SIZE = 10000;
        private final LinkedBlockingQueue<NettyChannelEvent> eventQueue = new LinkedBlockingQueue<NettyChannelEvent>();
        private String name;

        public ChannelEventExecutor(String nettyEventExector) {
            super(nettyEventExector);
            this.name = nettyEventExector;
        }

        public void putNettyEvent(final NettyChannelEvent event) {
            if (this.eventQueue.size() <= MAX_SIZE) {
                this.eventQueue.add(event);
            } else {
                LOG.warn(String.format("Event queue size[%s] meets the limit, so drop this event %s", this.eventQueue.size(), event.toString()));
            }
        }

        @Override
        public void run() {
            LOG.info(this.name + " service started");
            ChannelEventListenerGroup listener = VertxRemotingAbstract.this.channelEventListenerGroup;
            while (true) {
                try {
                    NettyChannelEvent event = this.eventQueue.poll(3000, TimeUnit.MILLISECONDS);
                    if (event != null && listener != null) {
                        LOG.info(String.format("Dispatch received channel event, %s", event));
                        switch (event.getType()) {
                            case CLOSE:
                                listener.onChannelClose(event.getChannel());
                                break;
                            case CONNECT:
                                listener.onChannelConnect(event.getChannel());
                                break;
                            case EXCEPTION:
                                listener.onChannelException(event.getChannel(), event.getCause());
                                break;
                            default:
                                break;
                        }
                    }
                } catch (Exception e) {
                    LOG.warn("Exception thrown when dispatching channel event", e);
                    break;
                }
            }
        }

    }

}
