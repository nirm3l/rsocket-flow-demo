package com.flow.demo.gateway.filters;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.report.ValidationReport;
import com.flow.demo.gateway.configuration.OpenApiConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class OpenApiValidationRSocketFilter implements GlobalFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenApiValidationRSocketFilter.class);

    private final Map<String, OpenApiInteractionValidator> validatorsMap = new ConcurrentHashMap<>();

    private final Map<String, HttpStatus> statusMap = new HashMap<>();

    public OpenApiValidationRSocketFilter() {
        statusMap.put("validation.request.contentType.notAllowed", HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        statusMap.put("validation.request.contentType.invalid", HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        statusMap.put("validation.request.path.missing", HttpStatus.NOT_FOUND);
        statusMap.put("validation.request.accept.invalid", HttpStatus.NOT_ACCEPTABLE);
        statusMap.put("validation.request.operation.notAllowed", HttpStatus.METHOD_NOT_ALLOWED);
    }

    private boolean shouldBeValidated(ServerHttpRequest request) {
        String contentType = request.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);

        if(contentType != null) {
            return !contentType.startsWith("multipart/form-data");
        }

        return true;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if(!shouldBeValidated(exchange.getRequest())) {
            LOGGER.info("Skip early stage validation for request: {}", exchange.getRequest().getPath().value());

            return chain.filter(exchange);
        }

        return ServerWebExchangeUtils.cacheRequestBody(exchange, serverHttpRequest -> {
            Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);

            return Mono.defer(() -> {
                if(route != null) {
                    String body = getBody(exchange);

                    SimpleRequest.Builder builder = createOpenApiRequestBuilder(
                            exchange.getRequest(), body);

                    if(builder != null) {
                        SimpleRequest request = builder.build();

                        ValidationReport report = validateRequest(route.getId(), request);

                        if (report.hasErrors()) {
                            return throwException(report.getMessages());
                        }
                    }
                }

                return chain.filter(exchange.mutate().request(serverHttpRequest).build());
            }).subscribeOn(Schedulers.boundedElastic());
        });
    }

    private String getBody(ServerWebExchange exchange) {
        DataBuffer buffer = exchange.getAttribute(ServerWebExchangeUtils.CACHED_REQUEST_BODY_ATTR);

        return buffer != null ? StandardCharsets.UTF_8.decode(buffer.asByteBuffer()).toString() : null;
    }

    private ValidationReport validateRequest(String routeId, SimpleRequest request) {
        OpenApiInteractionValidator validator = validatorsMap.get(routeId);

        if(validator == null) {
            validator = OpenApiInteractionValidator.createFor(
                    OpenApiConfig.SPECIFICATIONS.get(routeId)).build();

            validatorsMap.put(routeId, validator);
        }

        return validator.validateRequest(request);
    }

    private Mono<Void> throwException(List<ValidationReport.Message> messageList) {
        List<String> errors = new ArrayList<>();

        HttpStatus httpStatus = null;

        for (ValidationReport.Message message : messageList) {
            if(message.getLevel() == ValidationReport.Level.ERROR) {
                errors.add(message.getMessage());

                errors.addAll(message.getNestedMessages().stream().filter(
                        m -> m.getLevel() == ValidationReport.Level.ERROR).map(
                                ValidationReport.Message::getMessage).collect(Collectors.toList()));

                errors.addAll(message.getAdditionalInfo());

                if(statusMap.containsKey(message.getKey())) {
                    httpStatus = statusMap.get(message.getKey());
                }
            }
        }

        if(httpStatus == null) {
            httpStatus = HttpStatus.BAD_REQUEST;
        }

        return Mono.error(new ResponseStatusException(httpStatus, String.join(". ", errors)));
    }

    private SimpleRequest.Builder createOpenApiRequestBuilder(ServerHttpRequest request, String body) {
        if(request.getMethod() == null) {
            return null;
        }

        Request.Method method = Request.Method.valueOf(request.getMethod().name());

        SimpleRequest.Builder builder = new SimpleRequest.Builder(method, request.getPath().value());
        Set<String> headerNames = request.getHeaders().keySet();

        request.getQueryParams().forEach(builder::withQueryParam);

        headerNames.forEach(value -> builder.withHeader(value, request.getHeaders().getValuesAsList(value)));

        if(body != null) {
            builder.withBody(body);
        }

        return builder;
    }
}
