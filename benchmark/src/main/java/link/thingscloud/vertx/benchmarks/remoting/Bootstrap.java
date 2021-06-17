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

package link.thingscloud.vertx.benchmarks.remoting;

import link.thingscloud.vertx.remoting.api.AsyncHandler;
import link.thingscloud.vertx.remoting.api.RemotingClient;
import link.thingscloud.vertx.remoting.api.RemotingServer;
import link.thingscloud.vertx.remoting.api.RequestProcessor;
import link.thingscloud.vertx.remoting.api.channel.ChannelEventListener;
import link.thingscloud.vertx.remoting.api.channel.RemotingChannel;
import link.thingscloud.vertx.remoting.api.command.RemotingCommand;
import link.thingscloud.vertx.remoting.api.command.RemotingCommandFactory;
import link.thingscloud.vertx.remoting.api.interceptor.Interceptor;
import link.thingscloud.vertx.remoting.api.interceptor.RequestContext;
import link.thingscloud.vertx.remoting.api.interceptor.ResponseContext;
import link.thingscloud.vertx.remoting.spring.boot.starter.EnableRemotingClientAutoConfiguration;
import link.thingscloud.vertx.remoting.spring.boot.starter.EnableRemotingServerAutoConfiguration;
import link.thingscloud.vertx.remoting.spring.boot.starter.annotation.RemotingChannelEventListener;
import link.thingscloud.vertx.remoting.spring.boot.starter.annotation.RemotingInterceptor;
import link.thingscloud.vertx.remoting.spring.boot.starter.annotation.RemotingRequestProcessor;
import link.thingscloud.vertx.remoting.spring.boot.starter.annotation.RemotingType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PostConstruct;

/**
 * @author : zhouhailin
 * @version 0.5.0
 */
@EnableRemotingClientAutoConfiguration
@EnableRemotingServerAutoConfiguration
@SpringBootApplication
public class Bootstrap {

    private static final Logger LOG = LoggerFactory.getLogger(Bootstrap.class);

    public static void main(String[] args) {
        SpringApplication.run(Bootstrap.class, args);
    }

    @Autowired
    RemotingClient remotingClient;
    @Autowired
    RemotingServer remotingServer;
    @Autowired
    RemotingCommandFactory factory;

    private static final String URI = "/uri/v1";

    @PostConstruct
    public void start() {
        RemotingCommand request = factory.createRequest();
        request.cmdCode((short) 13);
        remotingClient.invokeOneWay("127.0.0.1:9888", URI, request);
        remotingClient.invokeAsync("127.0.0.1:9888", URI, request, new AsyncHandler() {
            @Override
            public void onFailure(RemotingCommand request, Throwable cause) {
                LOG.info("invokeAsync onFailure : {}, cause : ", request, cause);
            }

            @Override
            public void onSuccess(RemotingCommand response) {
                LOG.info("invokeAsync onSuccess : {}", response);

            }
        }, 1000);
        remotingClient.invokeOneWay("127.0.0.1:9888", URI, request);
        remotingClient.invokeAsync("127.0.0.1:9888", URI, request, new AsyncHandler() {
            @Override
            public void onFailure(RemotingCommand request, Throwable cause) {
                LOG.info("onFailure response : {}", request, cause);

            }

            @Override
            public void onSuccess(RemotingCommand response) {
                LOG.info("onSuccess response : {}", response);
            }
        }, 1000);

    }

    @RemotingRequestProcessor(uri = URI, code = 12, type = RemotingType.CLIENT)
    class RequestProcessorImpl1 implements RequestProcessor {
        @Override
        public RemotingCommand processRequest(RemotingChannel channel, RemotingCommand request) {
            LOG.info("processRequest : {}", request);
            return factory.createResponse(request);
        }
    }

    @RemotingRequestProcessor(uri = URI, code = 13, type = RemotingType.SERVER)
    class RequestProcessorImpl2 implements RequestProcessor {
        @Override
        public RemotingCommand processRequest(RemotingChannel channel, RemotingCommand request) {
            LOG.info("processRequest : {}", request);
            return factory.createResponse(request);
        }
    }

    @RemotingRequestProcessor(uri = URI, code = 14)
    class RequestProcessorImpl3 implements RequestProcessor {
        @Override
        public RemotingCommand processRequest(RemotingChannel channel, RemotingCommand request) {
            LOG.info("processRequest : {}", request);
            return factory.createResponse(request);
        }
    }

    @RemotingInterceptor
    class InterceptorImpl implements Interceptor {

        @Override
        public void beforeRequest(RequestContext context) {
            LOG.info("beforeRequest : {}", context);
        }

        @Override
        public void afterResponseReceived(ResponseContext context) {
            LOG.info("afterResponseReceived : {}", context);
        }
    }

    @RemotingChannelEventListener
    class ChannelEventListenerImpl implements ChannelEventListener {

        @Override
        public void onChannelConnect(RemotingChannel channel) {
            LOG.info("onChannelConnect : {}", channel);
        }

        @Override
        public void onChannelClose(RemotingChannel channel) {
            LOG.info("onChannelClose : {}", channel);
        }

        @Override
        public void onChannelException(RemotingChannel channel, Throwable cause) {
            LOG.error("onChannelException : {}", channel, cause);
        }
    }
}
