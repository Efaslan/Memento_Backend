package com.emiraslan.memento.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                // general info about the api
                .info(new Info()
                        .title("Memento Backend API")
                        .version("1.0.0")
                        .description("RESTful API documentation for TUBITAK 2209-A: AI-Powered Gerontology Mobile Assistant.")
                        .contact(new Contact()
                                .name("Emir Faik Aslan")
                                .email("efaslan11@gmail.com")
                                .url("https://github.com/Efaslan/Memento_Backend"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("http://springdoc.org")))
                .addTagsItem(new Tag().name("01 - Authentication").description("Receive your JWT after login, and enter it into the Authorize section (above-right)."))
                .addTagsItem(new Tag().name("02 - Profiles").description("Patient and Doctor profile information."))
                .addTagsItem(new Tag().name("03 - Relationships").description("Relationships between Patients and their Doctors and/or Relatives"))
                .addTagsItem(new Tag().name("04 - Saved Locations").description("Saved locations of Patient users."))
                .addTagsItem(new Tag().name("05 - General Reminders").description("Non-medical reminders of Patient users."))
                .addTagsItem(new Tag().name("06 - Daily Logs").description("Food and water consumption logs of Patient users."))
                .addTagsItem(new Tag().name("07 - Alerts").description("Critical situations (such as fall detections) of Patient users."))
                .addTagsItem(new Tag().name("08 - Medication Schedules").description("Medication schedules of Patient users."))
                .addTagsItem(new Tag().name("09 - Medication Logs").description("Medication intake logs of Patient users."))
                .addTagsItem(new Tag().name("10 - Notifications").description("Saving FCM tokens of user devices."))

                // JWT Authorize button config
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter your JWT here after login to utilize endpoints allowed for your role.")));
    }
}