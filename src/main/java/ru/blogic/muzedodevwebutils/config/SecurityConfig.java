package ru.blogic.muzedodevwebutils.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;

import javax.sql.DataSource;
import java.util.List;

@EnableWebSecurity
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {
    final DataSource dataSource;
    final SecurityConfigConfig securityConfigConfig;

    @PostConstruct
    public void postConstruct() {
        SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(cust ->
                cust
                    //.requestMatchers(new AntPathRequestMatcher("/api/info/**"))
                    //.permitAll()
                    .anyRequest()
                    .authenticated()
            ).formLogin(cust ->
                cust.defaultSuccessUrl("/", true)
            ).rememberMe(cust ->
                cust.key("idE61wAMkY")
                    .tokenRepository(persistentTokenRepository())
            ).csrf(AbstractHttpConfigurer::disable)
            .cors(AbstractHttpConfigurer::disable);
        return http.build();
    }

    public PersistentTokenRepository persistentTokenRepository() {
        final JdbcTokenRepositoryImpl tokenRepository = new JdbcTokenRepositoryImpl();
        tokenRepository.setDataSource(dataSource);
        //tokenRepository.setCreateTableOnStartup(true);
        return tokenRepository;
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return new InMemoryUserDetailsManager(
            securityConfigConfig.users
                .stream()
                .map(name -> User.withDefaultPasswordEncoder()
                    .username(name)
                    //.passwordEncoder(passwordEncoder().encode(pw))
                    .password("1")
                    .roles("USER")
                    .build())
                .toList()
        );
    }

//    @Bean
//    public PasswordEncoder passwordEncoder() {
//        return new BCryptPasswordEncoder();
//    }

    @ConfigurationProperties(prefix = "app.security")
    @RequiredArgsConstructor
    protected static class SecurityConfigConfig{
        final List<String> users;
    }
}
