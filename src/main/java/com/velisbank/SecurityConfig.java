package com.velisbank;

import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
    @Bean UserDetailsService users(CustomerRepository customers) {
        return username -> customers.findByUsername(username.trim().toLowerCase(java.util.Locale.ROOT))
            .map(c -> User.withUsername(c.username).password(c.passwordHash).roles(c.role).build())
            .orElseThrow(() -> new UsernameNotFoundException("Incorrect username or password."));
    }
    @Bean SecurityFilterChain security(HttpSecurity http, LoginAttempts attempts) throws Exception {
        return http.authorizeHttpRequests(a -> a
            .requestMatchers("/", "/index.html", "/app.js", "/styles.css", "/vendor/**", "/favicon.svg", "/api/csrf", "/api/session", "/api/register", "/api/login", "/error").permitAll()
            .requestMatchers("/api/admin/**").hasRole("ADMIN")
            .requestMatchers("/api/pin", "/api/pin/**", "/api/account", "/api/transactions/**", "/api/money/**").hasRole("CUSTOMER")
            .anyRequest().authenticated())
            .addFilterBefore(attempts.filter(), org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)
            .formLogin(f -> f.loginProcessingUrl("/api/login")
                .successHandler((req,res,auth) -> { attempts.onSuccess(req); res.setContentType("application/json"); res.getWriter().write("{\"ok\":true}"); })
                .failureHandler((req,res,e) -> attempts.onFailure(req,res)))
            .logout(l -> l.logoutUrl("/api/logout").logoutSuccessHandler((req,res,auth) -> res.setStatus(204)))
            .exceptionHandling(e -> e.authenticationEntryPoint((req,res,err) -> { res.setStatus(401); res.setContentType("application/json"); res.getWriter().write("{\"message\":\"Please log in to continue.\"}"); })
                .accessDeniedHandler((req,res,err) -> { res.setStatus(403); res.setContentType("application/json"); res.getWriter().write("{\"message\":\"This action is unavailable. Refresh the page and try again.\"}"); }))
            .build();
    }
}
