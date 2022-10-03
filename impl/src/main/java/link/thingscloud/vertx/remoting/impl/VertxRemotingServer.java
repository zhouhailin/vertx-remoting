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

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import link.thingscloud.vertx.remoting.api.AsyncHandler;
import link.thingscloud.vertx.remoting.api.RemotingServer;
import link.thingscloud.vertx.remoting.api.channel.RemotingChannel;
import link.thingscloud.vertx.remoting.api.command.RemotingCommand;
import link.thingscloud.vertx.remoting.config.RemotingServerConfig;
import link.thingscloud.vertx.remoting.impl.command.CodecHelper;
import link.thingscloud.vertx.remoting.impl.context.VertxRemotingHandlerContext;

/**
 * @author zhouhailin
 * @since 1.0.0
 */
public class VertxRemotingServer extends VertxRemotingAbstract implements RemotingServer {

    private final RemotingServerConfig config;
    private final Vertx vertx = Vertx.vertx(new VertxOptions().setWorkerPoolSize(40));
    private final HttpServerOptions httpServerOptions = new HttpServerOptions().setMaxWebSocketFrameSize(1000000);
    private final HttpServer httpServer = vertx.createHttpServer(httpServerOptions);

    public static final String HEALTH_CHECK = "/health/check";

    private Handler<HttpServerRequest> requestHandler = request -> {
        if (HEALTH_CHECK.equals(request.path())) {
            request.response().end("{\"status\":\"UP\"}");
        } else {
            request.response().setStatusCode(404);
            request.response().end();
        }
    };

    private static final Logger LOG = LoggerFactory.getLogger(VertxRemotingServer.class);

    public VertxRemotingServer(RemotingServerConfig config) {
        super(config);
        this.config = config;
        httpServer.close();
    }

    @Override
    public void start() {
        super.start();
        httpServer
                .requestHandler(requestHandler)
                .webSocketHandler(serverWebSocket -> {
                    String uri = serverWebSocket.uri();
                    VertxRemotingHandlerContext ctx = new VertxRemotingHandlerContext(uri, serverWebSocket);
                    serverWebSocket.accept();
                    LOG.info(String.format("Channel %s became active, remote address %s.", ctx.channel(), ctx.channel().remoteAddress()));
                    putNettyEvent(new NettyChannelEvent(NettyChannelEventType.CONNECT, ctx.channel()));
                    serverWebSocket
                            .frameHandler(event -> {
                                if (event.isText()) {
                                    RemotingCommand cmd = CodecHelper.decode(event.textData());
                                    if (cmd == null) {
                                        LOG.warn(String.format("Decode error %s failed.", event.textData()));
                                        return;
                                    }
                                    processMessageReceived(ctx, CodecHelper.decode(event.textData()));
                                }
                            })
                            .closeHandler(event -> {
                                LOG.info(String.format("Channel %s became inactive, remote address %s.", ctx.channel(), ctx.channel().remoteAddress()));
                                putNettyEvent(new NettyChannelEvent(NettyChannelEventType.CLOSE, ctx.channel()));
                            })
                            .exceptionHandler(cause -> {
                                LOG.info(String.format("Close channel %s because of error  ", ctx.channel()), cause);
                                putNettyEvent(new NettyChannelEvent(NettyChannelEventType.EXCEPTION, ctx.channel(), cause));
                            });
                })
                .listen(config.getServerListenPort(), event -> LOG.info(String.format("Vertx Remoting initialized with port(s): %d (websocket)", config.getServerListenPort())));
    }

    @Override
    public void stop() {
        httpServer.close();
        super.stop();
    }

    @Override
    public int localListenPort() {
        return httpServer.actualPort();
    }

    @Override
    public void setRequestHandler(Handler<HttpServerRequest> handler) {
        if (handler != null) {
            this.requestHandler = handler;
        }
    }

    @Override
    public void invokeAsync(RemotingChannel remotingChannel, RemotingCommand request, AsyncHandler asyncHandler, long timeoutMillis) {
        invokeAsyncWithInterceptor(remotingChannel, request, asyncHandler, timeoutMillis);
    }

    @Override
    public void invokeOneWay(RemotingChannel remotingChannel, RemotingCommand request) {
        invokeOnewayWithInterceptor(remotingChannel, request);
    }
}
