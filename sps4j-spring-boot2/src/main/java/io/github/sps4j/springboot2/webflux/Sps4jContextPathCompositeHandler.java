package io.github.sps4j.springboot2.webflux;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;

import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;


@Getter
public class Sps4jContextPathCompositeHandler implements HttpHandler {

    private final ConcurrentNavigableMap<String, HttpHandler> handlerMap;


    public Sps4jContextPathCompositeHandler(Map<String, ? extends HttpHandler> handlerMap) {
        Assert.notEmpty(handlerMap, "Handler map must not be empty");
        this.handlerMap = initHandlers(handlerMap);
    }

    private static ConcurrentNavigableMap<String, HttpHandler> initHandlers(Map<String, ? extends HttpHandler> map) {
        map.keySet().forEach(Sps4jContextPathCompositeHandler::assertValidContextPath);
        ConcurrentNavigableMap<String, HttpHandler> m = new ConcurrentSkipListMap<>(((Comparator<String>) String::compareTo).reversed());
        m.putAll(map);
        return m;
    }

    private static void assertValidContextPath(String contextPath) {
        Assert.hasText(contextPath, "Context path must not be empty");
        if (contextPath.equals("/")) {
            return;
        }
        Assert.isTrue(contextPath.startsWith("/"), "Context path must begin with '/'");
        Assert.isTrue(!contextPath.endsWith("/"), "Context path must not end with '/'");
    }


    @Override
    public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
        String path = request.getPath().value();
        Set<Map.Entry<String, HttpHandler>> entries = handlerMap.entrySet();
        for (Map.Entry<String, HttpHandler> entry : entries) {
            String key = entry.getKey();
            if (path.startsWith(key)) {
                request = request.mutate()
                        .contextPath("/")
                        .build();
                return entry.getValue().handle(request, response);
            }
        }
        response.setStatusCode(HttpStatus.NOT_FOUND);
        return response.setComplete();
    }


}
