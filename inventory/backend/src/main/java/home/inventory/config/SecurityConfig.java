package home.inventory.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    private static final String[] PUBLIC_PATHS = {
            "/actuator/health/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html"
    };

    /** Default: every API call needs a valid JWT issued by Keycloak. */
    @Bean
    @ConditionalOnProperty(name = "inventory.security.enabled", havingValue = "true", matchIfMissing = true)
    SecurityFilterChain jwtSecurity(HttpSecurity http) throws Exception {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setPrincipalClaimName("preferred_username");
        return base(http)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(rs -> rs.jwt(jwt -> jwt.jwtAuthenticationConverter(converter)))
                .build();
    }

    /** Only for local development without Keycloak ({@code inventory.security.enabled=false}). */
    @Bean
    @ConditionalOnProperty(name = "inventory.security.enabled", havingValue = "false")
    SecurityFilterChain openSecurity(HttpSecurity http) throws Exception {
        return base(http)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .build();
    }

    private static HttpSecurity base(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    }
}
