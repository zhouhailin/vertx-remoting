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

import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import link.thingscloud.vertx.remoting.RemotingBootstrapFactory;
import link.thingscloud.vertx.remoting.api.AsyncHandler;
import link.thingscloud.vertx.remoting.api.RemotingClient;
import link.thingscloud.vertx.remoting.api.RemotingService;
import link.thingscloud.vertx.remoting.api.command.Payload;
import link.thingscloud.vertx.remoting.api.command.RemotingCommand;
import link.thingscloud.vertx.remoting.common.RequestHandlerBuilder;
import link.thingscloud.vertx.remoting.config.RemotingClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class AbstractBenchmark {

    private static final String URI = "/uri/v1";

    protected static final Logger LOG = LoggerFactory.getLogger(AbstractBenchmark.class);

    public static void main(String[] args) throws InterruptedException {
        RequestHandlerBuilder builder = RequestHandlerBuilder.newBuilder();
        builder.request("/todo", new RequestHandlerBuilder.Callback() {
            @Override
            public RequestHandlerBuilder.Response apply(MultiMap headers, MultiMap params, Buffer body) {
                System.out.println(params);
                System.out.println(body);
                return new RequestHandlerBuilder.Response("ok");
            }
        }, HttpMethod.GET, HttpMethod.POST);


//        RemotingServer server = RemotingBootstrapFactory.createRemotingServer(new RemotingServerConfig());
//
//        server.registerRequestProcessor(URI, 1, (channel, request) -> {
//            RemotingCommand response = server.commandFactory().createResponse(request);
//            response.payload(Payload.of("zhangyimou"));
//            System.out.println(request.payload());
//            return response;
//        });
//
//        server.setRequestHandler(builder.build());
//        server.start();

        RemotingClient client = RemotingBootstrapFactory.createRemotingClient(new RemotingClientConfig());
        client.start();

        RemotingCommand request = client.commandFactory().createRequest();
        request.cmdCode(12);
        request.cmdVersion(1);
        request.payload(Payload.of("hello"));
        client.invokeAsync("127.0.0.1:9888", RemotingService.DEFAULT_URI, request, new AsyncHandler() {
            @Override
            public void onFailure(RemotingCommand request, Throwable cause) {
                System.out.println(request);
                cause.printStackTrace();
            }

            @Override
            public void onSuccess(RemotingCommand response) {
                System.out.println(response.payload());
            }
        }, 1000);

//        client.stop();
//        server.stop();
    }

    /**
     * Standard message sizes.
     */
    public enum MessageSize {
        SMALL(16), MEDIUM(1024), LARGE(65536), JUMBO(1048576);

        private final int bytes;

        MessageSize(int bytes) {
            this.bytes = bytes;
        }

        public int bytes() {
            return bytes;
        }
    }

    /**
     * Support channel types.
     */
    public enum ChannelType {
        NIO, LOCAL
    }
}
