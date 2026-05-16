package com.dianjinshou.config;

import com.dianjinshou.common.security.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors()
            .and()
            .csrf().disable()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .authorizeRequests()
                .antMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .antMatchers("/api/v1/health").permitAll()
                .antMatchers("/api/v1/auth/login", "/api/v1/auth/login/sms",
                             "/api/v1/auth/register", "/api/v1/auth/sms/send",
                             "/api/v1/auth/refresh", "/api/v1/auth/init-org").permitAll()
                .antMatchers("/api/v1/admin-auth/login").permitAll()
                .antMatchers("/swagger-ui/**", "/swagger-ui.html",
                             "/v3/api-docs/**", "/swagger-resources/**",
                             "/webjars/**").permitAll()
                .antMatchers("/actuator/**").permitAll()
                .antMatchers("/api/v1/share/**").permitAll()
                .antMatchers("/api/v1/public/**").permitAll()
                // 客户端异常上报：允许未登录（token 过期/主进程启动前/autoUpdater 失败）
                .antMatchers(HttpMethod.POST, "/api/v1/obs/error").permitAll()
                // 内置静态网页（all-streamers / my-streamers）
                .antMatchers("/", "/index.html",
                             "/all-streamers.html", "/my-streamers.html",
                             "/assets/**", "/favicon.ico", "/*.css", "/*.js").permitAll()
                // /api/v1/admin/config is open to any authenticated user (third-party 接入配置)
                .antMatchers("/api/v1/admin/config", "/api/v1/admin/config/**").authenticated()
                .antMatchers("/api/v1/admin/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "ADMIN_SUPER", "ADMIN_NORMAL")
                .anyRequest().authenticated()
            .and()
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .httpBasic().disable()
            .formLogin().disable();

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }
}
