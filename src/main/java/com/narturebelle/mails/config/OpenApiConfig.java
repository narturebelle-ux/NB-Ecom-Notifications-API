package com.narturebelle.mails.config;

import com.narturebelle.mails.security.InternalTokenFilter;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI's description of this API. Click "Authorize" and fill in the internal token for /internal/** calls,
 * or a Firebase ID token for the /api/** calls a signed-in user makes.
 */
@Configuration
public class OpenApiConfig {

    static final String INTERNAL_TOKEN = "internalToken";
    static final String FIREBASE = "firebase";

    @Bean
    public OpenAPI notificationsOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("NartureBelle™ Notifications API")
                        .description("Sends NartureBelle™ email through Resend. /internal/** is for the Care API "
                                + "and needs the private " + InternalTokenFilter.HEADER + " header.")
                        .version("1.0.0"))
                .components(new Components()
                        .addSecuritySchemes(INTERNAL_TOKEN, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name(InternalTokenFilter.HEADER)
                                .description("The INTERNAL_API_TOKEN shared with the Care API."))
                        .addSecuritySchemes(FIREBASE, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("A Firebase ID token from a signed-in user.")))
                .addSecurityItem(new SecurityRequirement().addList(INTERNAL_TOKEN))
                .addSecurityItem(new SecurityRequirement().addList(FIREBASE));
    }
}
