package link.thingscloud.vertx.remoting.common;

import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import link.thingscloud.vertx.remoting.impl.VertxRemotingServer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author zhouhailin
 * @since 0.4.0
 */

public class RequestHandlerBuilder {

    private final Map<String, Map<HttpMethod, Callback>> callbackMap = new ConcurrentHashMap<>(16);

    private static final Response NOT_FOUND = new Response(404, "Not Found");

    private static final Logger LOG = LoggerFactory.getLogger(RequestHandlerBuilder.class);

    private void end(HttpServerRequest request, Response response) {
        request.response().setStatusCode(response.code);
        request.response().setStatusMessage(response.message);
        if (response.data != null) {
            request.response().end(response.data);
        } else {
            request.response().end();
        }
    }

    private Handler<HttpServerRequest> requestHandler = request -> {
        if (request.path() == null) {
            end(request, NOT_FOUND);
            return;
        }
        Map<HttpMethod, Callback> httpMethodCallbackMap = callbackMap.get(request.path());
        if (httpMethodCallbackMap == null) {
            end(request, NOT_FOUND);
            return;
        }
        Callback callback = httpMethodCallbackMap.get(request.method());
        if (callback == null) {
            end(request, NOT_FOUND);
            return;
        }
        try {
            request.bodyHandler(event -> {
                Response apply = callback.apply(request.headers(), request.params(), event);
                request.response().setStatusCode(apply.code);
                request.response().setStatusMessage(apply.message);
                end(request, apply);
            });
        } catch (Exception e) {
            end(request, new Response(500, e.getMessage()));
            LOG.error(String.format("handle response failed, uri %s, params : %s, cause : ", request.path(), request.params().toString()), e);
        }
    };

    private RequestHandlerBuilder() {
        request(VertxRemotingServer.HEALTH_CHECK, (EmptyCallback) () -> new Response("{\"status\":\"UP\"}"), HttpMethod.GET, HttpMethod.POST);
    }

    public static RequestHandlerBuilder newBuilder() {
        return new RequestHandlerBuilder();
    }

    public RequestHandlerBuilder request(String url, Callback callback, HttpMethod... methods) {
        Map<HttpMethod, Callback> httpMethodCallbackMap = callbackMap.computeIfAbsent(url, key -> new ConcurrentHashMap<>(4));
        for (HttpMethod method : methods) {
            httpMethodCallbackMap.putIfAbsent(method, callback);
        }
        return this;
    }


    public Handler<HttpServerRequest> build() {
        return requestHandler;
    }

    public interface EmptyCallback extends Callback{

        Response apply();

        @Override
        default Response apply(MultiMap headers, MultiMap params, Buffer body) {
            return apply();
        }

    }

    public interface Callback {


        Response apply(MultiMap headers, MultiMap params, Buffer body);

    }

    public static class Response {
        private final int code;
        private final String message;
        private final String data;

        public Response(int code, String message) {
            this(code, message, null);
        }

        public Response(String data) {
            this(200, "OK", data);
        }

        public Response(int code, String message, String data) {
            this.code = code;
            this.message = message;
            this.data = data;
        }
    }

}
