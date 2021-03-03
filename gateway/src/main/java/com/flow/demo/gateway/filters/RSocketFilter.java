package com.flow.demo.gateway.filters;

import io.rsocket.routing.client.spring.RoutingRSocketRequester;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Component
public class RSocketFilter implements GlobalFilter {

    private final RoutingRSocketRequester requester;

    public RSocketFilter(RoutingRSocketRequester requester) {
        this.requester = requester;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);

        if(route != null) {
            ServerHttpRequest request = exchange.getRequest();

            RoutingRSocketRequester.RoutingRequestSpec spec = requester
                    .route(request.getPath().value()).address(route.getId());

            DataBuffer buffer = exchange.getAttribute(ServerWebExchangeUtils.CACHED_REQUEST_BODY_ATTR);

            if(buffer != null) {
                spec.data(buffer);
            }

            exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

            return exchange.getResponse()
                    .writeWith(
                            spec.metadata(metadataSpec ->
                                    metadataSpec.metadata(getMetadata(request), MimeType.valueOf("application/json")))
                                    .retrieveMono(DataBuffer.class));
        }

        return chain.filter(exchange.mutate().request(exchange.getRequest()).build());
    }

    private Map<String, String> getMetadata(ServerHttpRequest request) {
        Map<String, String> map = new HashMap<>();

        if(request.getMethod() != null) {
            map.put("method", request.getMethod().name());
        }

        return map;
    }
}
