# Vertx Remoting

[![Codacy Badge](https://api.codacy.com/project/badge/Grade/bc80abd17a444f0ba0d94ec807e07843)](https://app.codacy.com/manual/zhouhailin/vertx-remoting?utm_source=github.com&utm_medium=referral&utm_content=zhouhailin/vertx-remoting&utm_campaign=Badge_Grade_Settings)
[![Jdk Version](https://img.shields.io/badge/JDK-1.8-green.svg)](https://img.shields.io/badge/JDK-1.8-green.svg)
[![License](https://img.shields.io/badge/license-Apache%202-4EB1BA.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/link.thingscloud/vertx-remoting/badge.svg)](https://maven-badges.herokuapp.com/maven-central/link.thingscloud/vertx-remoting/)

## vertx-remoting

```xml

<dependency>
    <groupId>link.thingscloud</groupId>
    <artifactId>vertx-remoting-impl</artifactId>
    <version>${vertx-remoting.version}</version>
</dependency>
```

## vertx-remoting-spring-boot-starter

```xml

<dependency>
    <groupId>link.thingscloud</groupId>
    <artifactId>vertx-remoting-spring-boot-starter</artifactId>
    <version>${vertx-remoting.version}</version>
</dependency>
```

```properties
# RemotingServerProperties or RemotingClientProperties
vertx.remoting.server.serverListenPort=9888
server.shutdown=graceful
```

```java

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
```

## License

[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html) Copyright (C) Apache Software Foundation
