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

import io.netty.channel.ChannelHandlerContext;
import io.vertx.core.http.WebSocketBase;
import io.vertx.core.http.impl.WebSocketImplBase;
import link.thingscloud.vertx.remoting.api.RemotingHandlerContext;
import link.thingscloud.vertx.remoting.api.channel.RemotingChannel;
import link.thingscloud.vertx.remoting.api.command.RemotingCommand;

/**
 * @author zhouhailin
 * @since 1.0.0
 */
public class VertxRemotingHandlerContext implements RemotingHandlerContext {

    private final String id;
    private final String uri;
    private final WebSocketBase webSocketBase;
    private final ChannelHandlerContext context;
    private VertxRemotingChannel channel;

    public VertxRemotingHandlerContext(String uri, WebSocketBase webSocketBase) {
        this.uri = uri;
        this.webSocketBase = webSocketBase;
        this.id = webSocketBase.textHandlerID().replace("__vertx.ws.", "");
        this.context = ((WebSocketImplBase) webSocketBase).channelHandlerContext();
        this.channel = new VertxRemotingChannel(webSocketBase);
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String uri() {
        return uri;
    }

    @Override
    public boolean isActive() {
        return false;
    }

    @Override
    public RemotingChannel channel() {
        return channel;
    }

    @Override
    public void reply(RemotingCommand response) {
        channel.reply(response);
    }
}
