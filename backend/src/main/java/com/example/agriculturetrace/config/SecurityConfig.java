package com.example.agriculturetrace.config;

import com.example.agriculturetrace.entity.User;
import com.example.agriculturetrace.service.UserService;
import com.example.agriculturetrace.util.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.DisabledException;
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

    /**
     * 定义整套 Web 安全过滤链：关闭 CSRF、开启 CORS、配置接口权限、登录登出、
     * remember-me 和异常响应格式。这个 Bean 是 Spring Security 处理所有 HTTP
     * 请求时的核心规则入口。
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 前端是独立 Vite 应用，接口依赖 Session Cookie；这里关闭 CSRF，
                // 同时在 CorsConfig 中允许携带凭证，避免跨域登录后 Cookie 不生效。
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {
                })
                .authorizeHttpRequests(auth -> auth
                        // 登录、公开溯源页、上传头像静态资源不需要登录。
                        .requestMatchers("/api/user/login", "/api/trace/**", "/uploads/**").permitAll()
                        // 用户管理只给管理员；数据指纹和审计日志给管理员、监管员。
                        .requestMatchers("/api/users/**").hasRole("ADMIN")
                        .requestMatchers("/api/dashboard/reports").hasAnyRole("ADMIN", "INSPECTOR")
                        .requestMatchers("/api/blockchain/**").hasAnyRole("ADMIN", "INSPECTOR")
                        .requestMatchers("/api/integrity/fingerprints", "/api/integrity/products", "/api/integrity/root-hash").hasAnyRole("ADMIN", "INSPECTOR")
                        .anyRequest().authenticated()
                )
                .formLogin(login -> login
                        .loginProcessingUrl("/api/user/login")
                        .successHandler((request, response, authentication) -> {
                            // Spring Security 完成认证后立即返回前端需要缓存的用户和角色信息。
                            User user = userService.getByUsername(authentication.getName());
                            writeJson(response, Result.success(userService.toUserInfo(user)));
                        })
                        .failureHandler((request, response, exception) ->
                                writeJson(response, Result.error(401, loginFailureMessage(request, exception))))
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/api/user/logout")
                        .logoutSuccessHandler((request, response, authentication) ->
                                writeJson(response, Result.success(null)))
                )
                .rememberMe(remember -> remember
                        // 前端 login(data) 会提交 remember-me=true，Spring Security 据此写入持久 Cookie。
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

    /**
     * 将认证成功、认证失败和权限异常统一写成项目自己的 Result JSON。
     * 这样前端 axios 拦截器只需要识别 code/message，不必兼容 Spring Security 默认页面。
     */
    private void writeJson(HttpServletResponse response, Result<?> result) throws java.io.IOException {
        // 认证成功、失败、权限异常都返回统一 Result，前端拦截器不用区分 Spring 默认响应格式。
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }

    /**
     * 根据登录失败原因生成更友好的提示：不存在、被禁用和密码错误分别返回不同文案。
     * 这里不会暴露密码细节，只帮助用户理解账号状态。
     */
    private String loginFailureMessage(HttpServletRequest request, Exception exception) {
        String username = request.getParameter("username");
        if (username != null && !username.isBlank()) {
            return userService.findByUsername(username)
                    .map(user -> Boolean.FALSE.equals(user.getEnabled())
                            ? "账号已被禁用，请联系管理员"
                            : "用户名或密码错误")
                    .orElse("账号不存在，请联系管理员注册");
        }
        if (exception instanceof DisabledException) {
            return "账号已被禁用，请联系管理员";
        }
        return "用户名或密码错误";
    }
}
