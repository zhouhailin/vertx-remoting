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

import io.netty.channel.Channel;
import link.thingscloud.vertx.remoting.api.channel.RemotingChannel;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class NettyChannelEvent {
    private final RemotingChannel channel;
    private final NettyChannelEventType type;
    private final Throwable cause;

    public NettyChannelEvent(NettyChannelEventType type, RemotingChannel channel) {
        this(type, channel, null);
    }

    public NettyChannelEvent(NettyChannelEventType type, RemotingChannel channel, Throwable cause) {
        this.type = type;
        this.channel = channel;
        this.cause = cause;
    }

    public NettyChannelEventType getType() {
        return type;
    }

    public RemotingChannel getChannel() {
        return channel;
    }

    public Throwable getCause() {
        return cause;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.NO_CLASS_NAME_STYLE);
    }
}
