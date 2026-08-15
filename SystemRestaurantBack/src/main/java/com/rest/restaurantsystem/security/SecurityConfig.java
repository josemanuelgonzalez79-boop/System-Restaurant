package com.rest.restaurantsystem.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final SecurityProblemWriter problemWriter;

    public SecurityConfig(SecurityProblemWriter problemWriter) {
        this.problemWriter = problemWriter;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.spa())
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/api/health",
                                "/api/v1/auth/csrf",
                                "/api/v1/auth/setup-status",
                                "/api/v1/auth/setup",
                                "/api/v1/auth/login"
                        ).permitAll()
                        .requestMatchers("/api/v1/users/**").hasRole("OWNER")
                        .requestMatchers("/api/v1/cash-registers/**")
                        .hasAnyRole("OWNER", "ADMIN", "MANAGER", "CASHIER")
                        .requestMatchers("/api/v1/branches/*/assignments")
                        .hasAnyRole("OWNER", "ADMIN")
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/categories/**",
                                "/api/v1/products/**",
                                "/api/v1/modifier-groups/**",
                                "/api/v1/modifier-options/**",
                                "/api/v1/branches/**",
                                "/api/v1/areas/**",
                                "/api/v1/service-points/**"
                        ).authenticated()
                        .requestMatchers(
                                "/api/v1/settings/**",
                                "/api/v1/categories/**",
                                "/api/v1/products/**",
                                "/api/v1/modifier-groups/**",
                                "/api/v1/modifier-options/**",
                                "/api/v1/branches/**",
                                "/api/v1/areas/**",
                                "/api/v1/service-points/**"
                        ).hasAnyRole("OWNER", "ADMIN")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginProcessingUrl("/api/v1/auth/login")
                        .successHandler((request, response, authentication) ->
                                response.setStatus(HttpServletResponse.SC_NO_CONTENT)
                        )
                        .failureHandler((request, response, exception) ->
                                problemWriter.write(
                                        response,
                                        HttpStatus.UNAUTHORIZED.value(),
                                        "No autorizado",
                                        "El usuario o la contraseña son incorrectos."
                                )
                        )
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/api/v1/auth/logout")
                        .deleteCookies("JSESSIONID")
                        .logoutSuccessHandler((request, response, authentication) ->
                                response.setStatus(HttpServletResponse.SC_NO_CONTENT)
                        )
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) ->
                                problemWriter.write(
                                        response,
                                        HttpStatus.UNAUTHORIZED.value(),
                                        "No autorizado",
                                        "Debes iniciar sesión para continuar."
                                )
                        )
                        .accessDeniedHandler((request, response, exception) ->
                                problemWriter.write(
                                        response,
                                        HttpStatus.FORBIDDEN.value(),
                                        "Acceso denegado",
                                        "Tu cuenta no tiene permiso para realizar esta operación."
                                )
                        )
                );

        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
