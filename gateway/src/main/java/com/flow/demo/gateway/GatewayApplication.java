package com.flow.demo.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

@SpringBootApplication
@EnableDiscoveryClient
public class GatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(GatewayApplication.class, args);
	}

	@Bean
	public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
		RouteLocatorBuilder.Builder routesBuilder = builder.routes();

		addRoute(routesBuilder, "user-service", "user-service",
				"/api/users/**");
		addRoute(routesBuilder, "task-service", "task-service",
				"/api/tasks/**");

		return routesBuilder.build();
	}

	private void addRoute(
			RouteLocatorBuilder.Builder routesBuilder, String routeId, String serviceName, String... patterns) {
		routesBuilder.route(routeId, r ->
				r.path(patterns)
						.filters(f -> f.rewritePath("/api/(?<segment>.*)", "/${segment}"))
						.uri("lb://".concat(serviceName)));
	}


	@Bean(name = "loadBalancedWebClientBuilder")
	@LoadBalanced
	public WebClient.Builder loadBalancedWebClientBuilder() {
		return WebClient.builder();
	}
}
