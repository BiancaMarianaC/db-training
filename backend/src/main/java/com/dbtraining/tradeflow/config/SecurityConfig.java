package com.dbtraining.tradeflow.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * ============================================================================
 * SecurityConfig — TICKET-I076 + TICKET-I077
 * ============================================================================
 * WHAT:    Spring Security HTTP rules + in-memory user store.
 * HOW:     Single SecurityFilterChain @Bean + InMemoryUserDetailsManager.
 * WHY:     Day 6 needs role-based protection on every endpoint.
 * OBSERVE: After Day-6 work is wired, GET /api/v1/trades without auth → 401.
 * ============================================================================
 *
 *  DAY-1 DEFAULT (below): everything is `permitAll`. This lets the frontend
 *  load on Day 1 without an auth UI. TICKET-I076 + I077 replace this with
 *  proper role-based auth (admin / trader / viewer).
 * ============================================================================
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        // ====================================================================
        // Day-1 permissive default — replace with TICKET-I076 + I077 rules.
        // ====================================================================
        // TODO(TICKET-I076 + I077): the production-shaped filter chain looks
        //   roughly like this. Uncomment and remove the permitAll() block
        //   below when you tackle Day 6.
        //
        //   return http
        //       .csrf(csrf -> csrf.disable())
        //       .authorizeHttpRequests(auth -> auth
        //           .requestMatchers("/actuator/health", "/swagger-ui/**", "/v3/api-docs/**", "/h2-console/**").permitAll()
        //           .requestMatchers(HttpMethod.GET, "/api/v1/**").hasRole("VIEWER")
        //           .requestMatchers("/api/v1/**").hasRole("TRADER")
        //           .requestMatchers("/actuator/**").hasRole("ADMIN")
        //           .anyRequest().authenticated())
        //       .httpBasic(Customizer.withDefaults())
        //       .build();
        // ====================================================================

        return http
                .csrf(csrf -> csrf.disable())
                .headers(h -> h.frameOptions(f -> f.disable())) // allow /h2-console in dev
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .build();
    }

    // TODO(TICKET-I076): @Bean PasswordEncoder (BCrypt).
    // TODO(TICKET-I076): @Bean InMemoryUserDetailsManager with admin/trader/viewer.
    // TODO(TICKET-I077): @Bean RoleHierarchy if you want ADMIN > TRADER > VIEWER.
}
