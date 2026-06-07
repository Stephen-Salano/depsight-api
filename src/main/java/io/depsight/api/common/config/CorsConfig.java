package io.depsight.api.common.config;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfig {

  @Value("${spring.application.frontend-url}")
  private String frontendUrl;

  private final Environment environment;

  public CorsConfig(Environment environment) {
    this.environment = environment;
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

    // Main API CORS Configuration
    CorsConfiguration configuration = new CorsConfiguration();

    // Determine allowed origins based on environ
    List<String> allowedOrigins = getAllowedOrigins();
    configuration.setAllowedOriginPatterns(allowedOrigins);
    configuration.setAllowedMethods(
        Arrays.asList(
            "GET",
            "POST",
            "PUT",
            "DELETE",
            "OPTIONS" // options is the HTTP method the browser uses to send the preflight request
            // itself
            ));

    configuration.setAllowedHeaders(
        Arrays.asList(
            "Content-Type",
            "Accept",
            "Origin",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers",
            "X-Requested-With",
            "Cache-Control",
            "X-Trace-ID"));

    configuration.setExposedHeaders(Arrays.asList("X-Trace-ID"));
    configuration.setAllowCredentials(false);
    configuration.setMaxAge(3600L);

    // Apply to Api
    source.registerCorsConfiguration("/api/**", configuration);

    return source;
  }

  private List<String> getAllowedOrigins() {
    String activeProfile = Arrays.stream(environment.getActiveProfiles()).findFirst().orElse("dev");

    return switch (activeProfile) {
      case "prod" -> List.of(frontendUrl);
      default -> List.of("http://localhost:5173", "http://127.0.0.1:5173");
    };
  }
}
