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

package link.thingscloud.vertx.remoting.impl.context;

import io.netty.channel.Channel;
import io.vertx.core.http.WebSocketBase;
import io.vertx.core.http.impl.WebSocketImplBase;
import link.thingscloud.vertx.remoting.api.channel.ChannelFuture;
import link.thingscloud.vertx.remoting.api.channel.ChannelFutureImpl;
import link.thingscloud.vertx.remoting.api.channel.ChannelListener;
import link.thingscloud.vertx.remoting.api.channel.RemotingChannel;
import link.thingscloud.vertx.remoting.api.command.RemotingCommand;
import link.thingscloud.vertx.remoting.impl.command.CodecHelper;

import java.net.SocketAddress;

/**
 * @author zhouhailin
 * @since 1.0.0
 */
public class VertxRemotingChannel implements RemotingChannel {

    private final WebSocketBase webSocketBase;
    private final Channel channel;

    public VertxRemotingChannel(WebSocketBase webSocketBase) {
        this.webSocketBase = webSocketBase;
        this.channel = ((WebSocketImplBase) webSocketBase).channelHandlerContext().channel();
    }

    @Override
    public SocketAddress localAddress() {
        return channel.localAddress();
    }

    @Override
    public SocketAddress remoteAddress() {
        return channel.remoteAddress();
    }

    @Override
    public boolean isWritable() {
        return channel.isWritable();
    }

    @Override
    public boolean isActive() {
        return channel.isActive();
    }

    @Override
    public void close() {
        webSocketBase.close();
    }

    @Override
    public void close(ChannelListener listener) {
        webSocketBase.closeHandler(event -> listener.operationComplete(ChannelFuture.SUCCEED));
    }

    @Override
    public void reply(RemotingCommand command) {
        webSocketBase.writeTextMessage(CodecHelper.encodeCommand(command));
    }

    @Override
    public void reply(RemotingCommand command, ChannelListener listener) {
        webSocketBase.writeTextMessage(CodecHelper.encodeCommand(command), ar -> listener.operationComplete(new ChannelFutureImpl(ar.succeeded(), ar.cause())));
    }

    public String toString() {
        return "NettyChannelImpl [channel=" + channel + "]";
    }

}
