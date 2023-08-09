package net.friendly_bets.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.friendly_bets.dto.StandardResponseDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;

@RequiredArgsConstructor
@EnableWebSecurity
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@EnableGlobalMethodSecurity(securedEnabled = true, prePostEnabled = true, proxyTargetClass = true)
public class SecurityConfig {

    private final UserDetailsService userDetailsServiceImpl;

    private final PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                .cors().configurationSource(corsConfigurationSource());

        httpSecurity.csrf().disable()
                .headers().frameOptions().disable().and()
                .authorizeRequests()
                .antMatchers("/swagger-ui.html/**").permitAll()
                .and()
                .formLogin()
                .successHandler((request, response, authentication) -> {
                    fillResponse(response, 200, "Успешная авторизация");
                })
                .failureHandler((request, response, exception) ->
                        fillResponse(response, 401, "Неверный логин или пароль"))
                .and()
                .exceptionHandling()
                .defaultAuthenticationEntryPointFor((request, response, authException) ->
                                fillResponse(response, 403, "Пользователь не аутентифицирован"),
                        new AntPathRequestMatcher("/api/**"))
                .and()
                .logout().logoutSuccessHandler((request, response, authentication) ->
                        fillResponse(response, 200, "Выход выполнен успешно"));

        return httpSecurity.build();
    }

    @Autowired
    public void bindUserDetailsServiceAndPasswordEncoder(AuthenticationManagerBuilder builder) throws Exception {
        builder.userDetailsService(userDetailsServiceImpl).passwordEncoder(passwordEncoder);
    }

    private void fillResponse(HttpServletResponse response, int statusCode, String message) {
        response.setStatus(statusCode);
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        StandardResponseDto standardResponseDto = StandardResponseDto.builder()
                .message(message)
                .status(statusCode)
                .build();

        try {
            response.getWriter().write(objectMapper.writeValueAsString(standardResponseDto));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:3000", "https://friendly-bets.net", "https://www.friendly-bets.net", "http://friendly-bets.net", "http://www.friendly-bets.net", "https://friendly-bets.up.railway.app", "https://alexdiz88.github.io/"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
        configuration.setAllowedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/login", configuration);
        source.registerCorsConfiguration("/logout", configuration);
        source.registerCorsConfiguration("/**/register", configuration);

        return source;
    }

}
