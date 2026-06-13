package com.example.agriculturetrace.config;

import com.example.agriculturetrace.entity.User;
import com.example.agriculturetrace.service.UserService;
import com.example.agriculturetrace.util.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security Session 认证配置。
 *
 * 登录成功后浏览器保存 JSESSIONID Cookie；勾选 remember-me 时额外写入持久 Cookie，
 * 后续 API 仍由 Spring Security 根据 Cookie 恢复用户身份。
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final ObjectMapper objectMapper;
    private final UserService userService;
    private final UserDetailsService userDetailsService;

    public SecurityConfig(ObjectMapper objectMapper,
                          UserService userService,
                          UserDetailsService userDetailsService) {
        this.objectMapper = objectMapper;
        this.userService = userService;
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {
                })
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/user/login", "/api/trace/**", "/uploads/**").permitAll()
                        .requestMatchers("/api/users/**").hasRole("ADMIN")
                        .requestMatchers("/api/dashboard/reports").hasAnyRole("ADMIN", "INSPECTOR")
                        .anyRequest().authenticated()
                )
                .formLogin(login -> login
                        .loginProcessingUrl("/api/user/login")
                        .successHandler((request, response, authentication) -> {
                            User user = userService.getByUsername(authentication.getName());
                            writeJson(response, Result.success(userService.toUserInfo(user)));
                        })
                        .failureHandler((request, response, exception) ->
                                writeJson(response, Result.error(401, "用户名或密码错误")))
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/api/user/logout")
                        .logoutSuccessHandler((request, response, authentication) ->
                                writeJson(response, Result.success(null)))
                )
                .rememberMe(remember -> remember
                        .rememberMeParameter("remember-me")
                        .rememberMeCookieName("AGRICULTURE_TRACE_REMEMBER_ME")
                        .tokenValiditySeconds(7 * 24 * 60 * 60)
                        .key("agriculture-trace-remember-me-key")
                        .userDetailsService(userDetailsService)
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) ->
                                writeJson(response, Result.error(401, "未登录或登录已过期")))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                writeJson(response, Result.error(401, "权限不足")))
                );
        return http.build();
    }

    private void writeJson(HttpServletResponse response, Result<?> result) throws java.io.IOException {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
