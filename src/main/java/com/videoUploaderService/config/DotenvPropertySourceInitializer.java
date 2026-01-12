package com.videoUploaderService.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * Inicializador que carrega variáveis do arquivo .env antes do Spring Boot inicializar
 */
public class DotenvPropertySourceInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        try {
            Dotenv dotenv = Dotenv.configure()
                    .ignoreIfMissing()
                    .load();

            ConfigurableEnvironment environment = applicationContext.getEnvironment();
            Map<String, Object> envMap = new HashMap<>();

            dotenv.entries().forEach(entry -> {
                envMap.put(entry.getKey(), entry.getValue());
            });

            MapPropertySource dotenvPropertySource = new MapPropertySource("dotenv", envMap);
            environment.getPropertySources().addFirst(dotenvPropertySource);
        } catch (Exception e) {
            // Arquivo .env não encontrado, usar variáveis de ambiente do sistema
        }
    }
}


