package udehnih.report.config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import udehnih.report.filter.JwtAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import udehnih.report.filter.CorsFilter;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.Collections;
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    @PostConstruct
    public void enableAuthenticationContextOnSpawnedThreads() {
        SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
    }
    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @Autowired
    private CorsFilter corsFilter;
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/error").permitAll()
                .requestMatchers("/api/test/public").permitAll()
                .requestMatchers("/api/test/authenticated").authenticated()
                .requestMatchers("/api/test/student").hasRole("STUDENT")
                .requestMatchers("/api/test/staff").hasRole("STAFF")
                .requestMatchers("/api/test/tutor").hasRole("TUTOR")
                .requestMatchers("/api/reports", "/api/reports/**").permitAll()
                .requestMatchers("/api/staff/reports").permitAll() 
                .requestMatchers("/api/staff/**").hasRole("STAFF")
                .anyRequest().authenticated()
            )
            .securityMatcher("/**")
            .addFilterBefore(corsFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(jwtAuthenticationFilter, CorsFilter.class)
            .exceptionHandling(handling -> handling
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\": \"Unauthorized: " + authException.getMessage() + "\"}");
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\": \"Access Denied: " + accessDeniedException.getMessage() + "\"}");
                })
            )
            .csrf(csrf -> csrf.disable());
        return http.build();
    }
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Collections.singletonList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization", 
            "Content-Type", 
            "Accept", 
            "X-Requested-With", 
            "Cache-Control", 
            "Access-Control-Allow-Origin",
            "Access-Control-Allow-Headers"
        ));
        configuration.setExposedHeaders(Arrays.asList(
            "Authorization", 
            "x-auth-token", 
            "X-Auth-Token", 
            "X-Auth-Status", 
            "X-Auth-Username", 
            "X-Auth-Role", 
            "X-Auth-Name",
            "X-User-Email",
            "X-User-Role",
            "Access-Control-Allow-Origin",
            "Access-Control-Allow-Credentials"
        ));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
} 