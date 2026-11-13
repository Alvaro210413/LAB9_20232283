package com.stfgames.puzzle.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * WS Spring Security: el Web Service REST (/api/**) solo responde a clientes autenticados
 * (HTTP Basic, rol WS). La pagina web y el cliente (/juego/**) son publicos; el cliente
 * Spring Boot es quien se autentica al consumir el WS.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**", "/juego/**"))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/**").hasRole("WS")
                .requestMatchers("/", "/index.html", "/css/**", "/js/**", "/img/**", "/juego/**", "/error").permitAll()
                .anyRequest().denyAll())
            .httpBasic(Customizer.withDefaults())
            .formLogin(form -> form.disable());
        return http.build();
    }

    @Bean
    public UserDetailsService users(@Value("${ws.user}") String user,
                                    @Value("${ws.password}") String password,
                                    PasswordEncoder encoder) {
        return new InMemoryUserDetailsManager(
                User.withUsername(user).password(encoder.encode(password)).roles("WS").build());
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
