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

package link.thingscloud.vertx.remoting;


import link.thingscloud.vertx.remoting.api.RemotingClient;
import link.thingscloud.vertx.remoting.api.RemotingServer;
import link.thingscloud.vertx.remoting.api.command.RemotingCommandFactory;
import link.thingscloud.vertx.remoting.config.RemotingClientConfig;
import link.thingscloud.vertx.remoting.config.RemotingServerConfig;
import link.thingscloud.vertx.remoting.impl.VertxRemotingClient;
import link.thingscloud.vertx.remoting.impl.VertxRemotingServer;
import link.thingscloud.vertx.remoting.impl.command.RemotingCommandFactoryImpl;
import link.thingscloud.vertx.remoting.internal.BeanUtils;
import link.thingscloud.vertx.remoting.internal.PropertyUtils;

import java.util.Properties;

/**
 * Remoting Bootstrap entrance.
 *
 * @author zhouhailin
 */
public final class RemotingBootstrapFactory {

    public static RemotingClient createRemotingClient(final RemotingClientConfig config) {
        return new VertxRemotingClient(config);
    }

    public static RemotingClient createRemotingClient(final String fileName) {
        Properties prop = PropertyUtils.loadProps(fileName);
        RemotingClientConfig config = BeanUtils.populate(prop, RemotingClientConfig.class);
        return new VertxRemotingClient(config);
    }

    public static RemotingClient createRemotingClient(final Properties properties) {
        RemotingClientConfig config = BeanUtils.populate(properties, RemotingClientConfig.class);
        return new VertxRemotingClient(config);
    }

    public static RemotingServer createRemotingServer(final String fileName) {
        Properties prop = PropertyUtils.loadProps(fileName);
        RemotingServerConfig config = BeanUtils.populate(prop, RemotingServerConfig.class);
        return new VertxRemotingServer(config);
    }

    public static RemotingServer createRemotingServer(final Properties properties) {
        RemotingServerConfig config = BeanUtils.populate(properties, RemotingServerConfig.class);
        return new VertxRemotingServer(config);
    }

    public static RemotingServer createRemotingServer(final RemotingServerConfig config) {
        return new VertxRemotingServer(config);
    }

    public static RemotingCommandFactory createRemotingCommandFactory() {
        return new RemotingCommandFactoryImpl();
    }
}
