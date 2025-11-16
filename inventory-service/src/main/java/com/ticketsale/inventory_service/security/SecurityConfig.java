package com.ticketsale.inventory_service.security; // <-- Correct package

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                // --- THIS IS THE CORS FIX ---
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // --------------------------
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Allow ANYONE to GET events (browsing)
                        .requestMatchers(HttpMethod.GET, "/api/inventory/events/**").permitAll()

                        // Allow authenticated users (like the booking-service) to reserve/release
                        .requestMatchers(HttpMethod.POST, "/api/inventory/events/*/reserve").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/inventory/events/*/release").authenticated()

                        // ALL other methods (POST, DELETE, PUT) must be authenticated (admin)
                        .requestMatchers(HttpMethod.POST, "/api/inventory/events").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/inventory/events/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/inventory/events/**").authenticated()

                        .anyRequest().permitAll()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Allow your frontend's ports
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000", "http://localhost:5173"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(false);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}