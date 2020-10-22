package com.flow.demo.gateway.configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.ServerVariable;
import io.swagger.v3.parser.OpenAPIV3Parser;
import org.springdoc.core.GroupedOpenApi;
import org.springdoc.core.customizers.OpenApiCustomiser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.*;

@Configuration
@OpenAPIDefinition(servers = {
        @Server(url = "http://{host}/api", description = "Flow Demo API"),
})
public class OpenApiConfig {

    public final static Map<String, String> SPECIFICATIONS = new HashMap<>();

    static {
        SPECIFICATIONS.put("user-service", "user-service/src/main/resources/api.yaml");
        SPECIFICATIONS.put("task-service", "task-service/src/main/resources/api.yaml");
    }

    @Value("${application.url}")
    private String applicationUrl;

    @Bean
    public GroupedOpenApi userServiceApi() {
        return GroupedOpenApi.builder()
                .setGroup("user-service")
                .addOpenApiCustomiser(
                        new InternalOpenApiCustomiser(SPECIFICATIONS.get("user-service"), applicationUrl))
                .build();
    }

    @Bean
    public GroupedOpenApi taskCleaningServiceApi() {
        return GroupedOpenApi.builder()
                .setGroup("task-service")
                .addOpenApiCustomiser(
                        new InternalOpenApiCustomiser(SPECIFICATIONS.get("task-service"), applicationUrl))
                .build();
    }

    public static class InternalOpenApiCustomiser implements OpenApiCustomiser {

        private String fileLocation;

        private String applicationUrl;

        public InternalOpenApiCustomiser(String fileLocation, String applicationUrl) {
            this.fileLocation = fileLocation;
            this.applicationUrl = applicationUrl;
        }

        @Override
        public void customise(OpenAPI openApi) {
            OpenAPI api = new OpenAPIV3Parser().read(fileLocation);

            openApi.setComponents(api.getComponents());
            openApi.setPaths(api.getPaths());
            openApi.setExtensions(api.getExtensions());
            openApi.setInfo(api.getInfo());
            openApi.setExternalDocs(api.getExternalDocs());
            openApi.setOpenapi(api.getOpenapi());
            openApi.setTags(api.getTags());

            appendSecurity(openApi);

            //Set server variable to current host
            if(openApi.getServers() != null) {
                openApi.getServers().forEach(server -> {
                    ServerVariable variable = new ServerVariable();
                    variable.setDefault(applicationUrl);

                    if(server.getVariables() != null) {
                        server.getVariables().addServerVariable("host", variable);
                    }
                });
            }
        }

        private void appendSecurity(OpenAPI openApi) {
            SecurityScheme scheme = new SecurityScheme();
            scheme.setType(SecurityScheme.Type.HTTP);
            scheme.setBearerFormat("JWT");
            scheme.setScheme("bearer");

            openApi.getComponents().addSecuritySchemes("bearerAuth", scheme);

            SecurityRequirement requirement = new SecurityRequirement();
            requirement.put("bearerAuth", new ArrayList<>());

            openApi.setSecurity(Collections.singletonList(requirement));
        }
    }
}
