package com.stfgames.puzzle.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/** Cliente que consume el WS REST enviando las credenciales HTTP Basic. */
@Configuration
public class RestClientConfig {

    @Bean
    public RestTemplate wsRestTemplate(RestTemplateBuilder builder,
                                       @Value("${ws.user}") String user,
                                       @Value("${ws.password}") String password,
                                       @Value("${ws.base-url}") String baseUrl) {
        return builder.rootUri(baseUrl).basicAuthentication(user, password).build();
    }
}
