package io.rsocket.routing.client.spring;

import io.rsocket.routing.common.spring.MimeTypes;
import io.rsocket.routing.frames.RouteSetup;
import org.springframework.boot.autoconfigure.rsocket.RSocketMessageHandlerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.messaging.rsocket.DefaultMetadataExtractor;
import org.springframework.messaging.rsocket.RSocketConnectorConfigurer;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.util.MimeType;

import java.util.Map;
@Configuration
public class RSocketConfig {

    @Bean
    public RSocketMessageHandlerCustomizer messageHandlerCustomizer() {
        return customizer -> {
            if(customizer.getMetadataExtractor() instanceof DefaultMetadataExtractor) {
                DefaultMetadataExtractor extractor = (DefaultMetadataExtractor) customizer.getMetadataExtractor();

                extractor.metadataToExtract(
                        MimeType.valueOf("application/json"), Map.class, (value, result) -> {
                            result.putAll(value);
                        });
            }
        };
    }

    // Provide custom RoutingRSocketRequesterBuilder which will
    // use application/json instead of default application/cbor mime data format
    @Bean
    @Scope("prototype")
    public RoutingRSocketRequesterBuilder routingClientRSocketRequesterBuilder(RSocketConnectorConfigurer configurer, RSocketStrategies strategies, RoutingClientProperties properties) {
        RouteSetup.Builder routeSetup = RouteSetup.from(properties.getRouteId(), properties.getServiceName());
        properties.getTags().forEach((key, value) -> {
            if (key.getWellKnownKey() != null) {
                routeSetup.with(key.getWellKnownKey(), value);
            } else if (key.getKey() != null) {
                routeSetup.with(key.getKey(), value);
            }
        });

        RSocketRequester.Builder builder = RSocketRequester.builder().setupMetadata(routeSetup.build(), MimeTypes.ROUTING_FRAME_MIME_TYPE).rsocketStrategies(strategies).rsocketConnector(configurer);
        builder.dataMimeType(MimeType.valueOf("application/json"));

        return new RoutingRSocketRequesterBuilder(builder, properties, strategies.routeMatcher());
    }
}
