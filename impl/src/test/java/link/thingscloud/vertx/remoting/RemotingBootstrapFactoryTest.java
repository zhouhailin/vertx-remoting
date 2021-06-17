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

import link.thingscloud.vertx.remoting.api.AsyncHandler;
import link.thingscloud.vertx.remoting.api.RemotingClient;
import link.thingscloud.vertx.remoting.api.RemotingServer;
import link.thingscloud.vertx.remoting.api.RequestProcessor;
import link.thingscloud.vertx.remoting.api.channel.RemotingChannel;
import link.thingscloud.vertx.remoting.api.command.RemotingCommand;
import link.thingscloud.vertx.remoting.api.command.RemotingCommandFactory;
import link.thingscloud.vertx.remoting.config.RemotingClientConfig;
import link.thingscloud.vertx.remoting.config.RemotingServerConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author zhouhailin
 * @since 1.0.0
 */
class RemotingBootstrapFactoryTest {
    RemotingCommandFactory factory = RemotingBootstrapFactory.createRemotingCommandFactory();

    @BeforeEach
    void setUp() {
    }

    @AfterEach
    void tearDown() {
    }

    @org.junit.jupiter.api.Test
    void createRemotingClient() {
    }

    @org.junit.jupiter.api.Test
    void testCreateRemotingClient() {
    }

    @org.junit.jupiter.api.Test
    void testCreateRemotingClient1() throws InterruptedException {
        RemotingClient remotingClient = RemotingBootstrapFactory.createRemotingClient(new RemotingClientConfig());
        remotingClient.start();
        remotingClient.invokeAsync("localhost:8888", "/v1", factory.createRequest().cmdCode(1), new AsyncHandler() {
            @Override
            public void onFailure(RemotingCommand request, Throwable cause) {
                System.err.println(request);
                cause.printStackTrace();
            }

            @Override
            public void onSuccess(RemotingCommand response) {
                System.out.println(response);
            }
        }, 1000);
        remotingClient.invokeOneWay("localhost:8888", "/v1", factory.createRequest().cmdCode(2).property("xx", "zx"));
        new CountDownLatch(1).await();
    }

    @org.junit.jupiter.api.Test
    void createRemotingServer() throws InterruptedException {
        RemotingServer remotingServer = RemotingBootstrapFactory.createRemotingServer(new RemotingServerConfig());
        remotingServer.registerRequestProcessor("/v1", 1, new RequestProcessor() {
            @Override
            public RemotingCommand processRequest(RemotingChannel channel, RemotingCommand request) {
                System.out.println(request);
                return factory.createResponse(request).remark("xxxx");
            }
        });
        remotingServer.registerRequestProcessor("/v1", 2, new RequestProcessor() {
            @Override
            public RemotingCommand processRequest(RemotingChannel channel, RemotingCommand request) {
                System.out.println(request);
                return factory.createResponse(request).remark("xxxx");
            }
        });
        remotingServer.start();
        new CountDownLatch(1).await();
    }

    @org.junit.jupiter.api.Test
    void testCreateRemotingServer() {
    }

    @org.junit.jupiter.api.Test
    void testCreateRemotingServer1() {
    }
}