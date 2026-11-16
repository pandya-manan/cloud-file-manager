
package com.manan.cloudfilemanager.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI cloudFileManagerOpenAPI() {

        return new OpenAPI()
                .info(new Info()
                        .title("Cloud File Manager API")
                        .description("""
                                REST APIs for the Cloud File Management System.

                                Features:
                                - User registration and authentication
                                - File upload and management
                                - File download and sharing
                                - Admin dashboard and management
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Cloud File Manager")));
    }
}
