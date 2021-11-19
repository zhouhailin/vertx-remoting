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

package link.thingscloud.vertx.remoting.config;

/**
 * @author zhouhailin
 * @since 1.0.0
 */
public class RemotingServerConfig extends RemotingConfig {

    private int serverListenPort = 8888;

    /**
     * If server only listened 1 port, recommend to set the value to 1
     */
    private final int serverOnewayInvokeSemaphore = 256;
    private final int serverAsyncInvokeSemaphore = 64;

    public int getServerListenPort() {
        return serverListenPort;
    }

    public RemotingServerConfig setServerListenPort(int serverListenPort) {
        this.serverListenPort = serverListenPort;
        return this;
    }

    @Override
    public int getOnewayInvokeSemaphore() {
        return serverOnewayInvokeSemaphore;
    }

    @Override
    public int getAsyncInvokeSemaphore() {
        return serverAsyncInvokeSemaphore;
    }
}
