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

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.WebSocket;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import link.thingscloud.vertx.remoting.api.AsyncHandler;
import link.thingscloud.vertx.remoting.api.RemotingClient;
import link.thingscloud.vertx.remoting.api.RemotingHandlerContext;
import link.thingscloud.vertx.remoting.api.channel.RemotingChannel;
import link.thingscloud.vertx.remoting.api.command.RemotingCommand;
import link.thingscloud.vertx.remoting.config.RemotingClientConfig;
import link.thingscloud.vertx.remoting.impl.command.CodecHelper;
import link.thingscloud.vertx.remoting.impl.context.VertxRemotingHandlerContext;
import link.thingscloud.vertx.remoting.internal.RemotingUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/**
 * @author zhouhailin
 * @since 1.0.0
 */
public class VertxRemotingClient extends VertxRemotingAbstract implements RemotingClient {

    private final RemotingClientConfig config;

    private final Vertx vertx = Vertx.vertx(new VertxOptions().setWorkerPoolSize(40));
    private final HttpClientOptions httpClientOptions = new HttpClientOptions().setMaxWebSocketFrameSize(1000000);
    private final HttpClient httpClient = vertx.createHttpClient(httpClientOptions);

    private final Map<String, Map<String, RemotingHandlerContext>> channelTables = new ConcurrentHashMap<>();
    private final Lock lockChannelTables = new ReentrantLock();

    private static final long LOCK_TIMEOUT_MILLIS = 3000;

    private static final Logger LOG = LoggerFactory.getLogger(VertxRemotingClient.class);

    public VertxRemotingClient(RemotingClientConfig config) {
        super(config);
        this.config = config;
    }

    @Override
    public void start() {
        super.start();
    }

    @Override
    public void stop() {
        httpClient.close();
        super.stop();
    }

    @Override
    public void invokeAsync(String address, String uri, RemotingCommand request, AsyncHandler asyncHandler, long timeoutMillis) {
        createIfAbsent(address, uri, channel -> {
            if (channel != null && channel.isActive()) {
                invokeAsyncWithInterceptor(channel, request, asyncHandler, timeoutMillis);
            } else {
                closeChannel(address, uri, channel);
            }
        });
    }

    @Override
    public void invokeOneWay(String address, String uri, RemotingCommand request) {
        createIfAbsent(address, uri, channel -> {
            if (channel != null && channel.isActive()) {
                invokeOnewayWithInterceptor(channel, request);
            } else {
                closeChannel(address, uri, channel);
            }
        });
    }

    private void createIfAbsent(final String addr, final String uri, final Consumer<RemotingChannel> executor) {
        Map<String, RemotingHandlerContext> channelTable = channelTables.computeIfAbsent(addr, s -> new ConcurrentHashMap<>(8));
        RemotingHandlerContext context = channelTable.get(uri);
        if (context != null && context.channel() != null && context.channel().isActive()) {
            executor.accept(context.channel());
            return;
        }
        create(addr, uri, channelTable, executor);
    }

    private void create(final String addr, final String uri, final Map<String, RemotingHandlerContext> channelTable, final Consumer<RemotingChannel> executor) {
        try {
            if (lockChannelTables.tryLock(LOCK_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
                try {
                    RemotingHandlerContext context = channelTable.get(uri);
                    if (context != null) {
                        executor.accept(context.channel());
                    } else {
                        String[] s = addr.split(":");
                        httpClient.webSocket(Integer.parseInt(s[1]), s[0], uri, ar -> {
                            if (ar.succeeded()) {
                                WebSocket webSocket = ar.result();
                                RemotingHandlerContext ctx = new VertxRemotingHandlerContext(uri, webSocket);
                                LOG.info(String.format("Connected from %s to %s:%s", ctx.channel().localAddress(), ctx.channel().remoteAddress(), ctx.uri()));
                                webSocket
                                        .frameHandler(event -> {
                                            if (event.isText()) {
                                                RemotingCommand cmd = CodecHelper.decode(event.textData());
                                                if (cmd == null) {
                                                    return;
                                                }
                                                processMessageReceived(ctx, CodecHelper.decode(event.textData()));
                                            }
                                        })
                                        .closeHandler(event -> {
                                            LOG.info(String.format("Remote address %s close channel %s ", ctx.channel().remoteAddress(), ctx.channel()));
                                            closeChannel(addr, ctx.uri(), ctx.channel());
                                            putNettyEvent(new NettyChannelEvent(NettyChannelEventType.CLOSE, ctx.channel()));
                                        })
                                        .exceptionHandler(cause -> {
                                            LOG.info(String.format("Close channel %s because of error ", ctx.channel()), cause);
                                            closeChannel(addr, ctx.uri(), ctx.channel());
                                            putNettyEvent(new NettyChannelEvent(NettyChannelEventType.EXCEPTION, ctx.channel(), cause));
                                        });
                                executor.accept(ctx.channel());
                            } else {
                                LOG.warn("createChannel: connect remote host[" + addr + "] failed, and destroy the channel", ar.cause());
                            }
                        });
                    }
                } catch (Exception e) {
                    LOG.error("createChannel: create channel exception", e);
                } finally {
                    this.lockChannelTables.unlock();
                }
            }
        } catch (InterruptedException e) {
        }
    }

    private void closeChannel(String addr, String uri, RemotingChannel channel) {
        Map<String, RemotingHandlerContext> channelTable = channelTables.computeIfAbsent(addr, s -> new ConcurrentHashMap<>(8));
        final String remoteAddr = null == addr ? RemotingUtil.extractRemoteAddress(channel) : addr;
        try {
            if (this.lockChannelTables.tryLock(LOCK_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
                try {
                    boolean removeItemFromTable = true;
                    RemotingHandlerContext prevCW = channelTable.get(uri);
                    //Workaround for null
                    if (null == prevCW) {
                        return;
                    }

                    LOG.info(String.format("Begin to close the remote address %s channel %s", remoteAddr, prevCW));

                    if (prevCW.channel() != channel) {
                        LOG.info(String.format("Channel %s has been closed,this is a new channel %s", prevCW.channel(), channel));
                        removeItemFromTable = false;
                    }

                    if (removeItemFromTable) {
                        this.channelTables.remove(remoteAddr);
                        LOG.info(String.format("Channel %s has been removed !", remoteAddr));
                    }

                    channel.close(future -> LOG.warn(String.format("Close channel %s %s", channel, future.succeeded())));
                } catch (Exception e) {
                    LOG.error("Close channel error !", e);
                } finally {
                    this.lockChannelTables.unlock();
                }
            } else {
                LOG.warn(String.format("Can not lock channel table in %s ms", LOCK_TIMEOUT_MILLIS));
            }
        } catch (InterruptedException e) {
            LOG.error("Close channel error !", e);
        }
    }


}
