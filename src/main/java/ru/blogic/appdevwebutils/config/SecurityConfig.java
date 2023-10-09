package ru.blogic.appdevwebutils.config;

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
import org.springframework.security.web.authentication.rememberme.PersistentTokenBasedRememberMeServices;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;

import javax.sql.DataSource;

@EnableWebSecurity
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {
    private static final String REMEMBER_ME = "remember-me";
    final DataSource dataSource;
    final SecurityConfigConfig securityConfigConfig;
    private static final String REMEMBER_ME_KEY = "KEY";

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
                cust.rememberMeServices(persistentTokenBasedRememberMeServices())
            ).csrf(AbstractHttpConfigurer::disable)
            .cors(AbstractHttpConfigurer::disable);
        return http.build();
    }

    @Bean
    public PersistentTokenBasedRememberMeServices persistentTokenBasedRememberMeServices() {
        PersistentTokenBasedRememberMeServices rememberMeServices =
            new PersistentTokenBasedRememberMeServices(
                REMEMBER_ME_KEY,
                userDetailsService(),
                persistentTokenRepository());
        rememberMeServices.setParameter(REMEMBER_ME);
        rememberMeServices.setCookieName(REMEMBER_ME);
        rememberMeServices.setTokenValiditySeconds(60 * 60 * securityConfigConfig.tokenValidityHours);
        rememberMeServices.setUseSecureCookie(true);
        return rememberMeServices;
    }

    @Bean
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
    protected static class SecurityConfigConfig {
        final java.util.List<String> users;
        final int tokenValidityHours = 200;
    }
}
