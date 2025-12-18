package me.dodo.readingnotes.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    private static final String BEARER = "BearerAuth";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("Reading Notes API").version("1.0.0"))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes(BEARER,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                        )
                )
                // 전역 보안 요구: BearerAuth
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }

    @Bean
    public OpenApiCustomizer applySecurityToAll() {
        final String BEARER = "BearerAuth";
        final var REQ = java.util.List.of(new SecurityRequirement().addList(BEARER));

        // 공개로 둘 경로(필요 시 수정)
        final var fullOpen = java.util.Set.of("/auth/login");
        return openApi -> {
            if (openApi.getPaths() == null) return;

            openApi.getPaths().forEach((path, item) -> {
                // 전체 공개 경로
                if (fullOpen.contains(path)) {
                    item.readOperations().forEach(op -> op.setSecurity(java.util.List.of()));
                    return;
                }
                // 기본: 잠금(=Bearer 요구)
                item.readOperations().forEach(op -> op.setSecurity(REQ));

                // 예: /users는 POST(회원가입)만 공개
                if ("/users".equals(path) && item.getPost() != null) {
                    item.getPost().setSecurity(java.util.List.of());
                }
            });
        };
    }
}
