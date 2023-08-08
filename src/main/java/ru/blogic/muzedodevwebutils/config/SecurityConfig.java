package ru.blogic.muzedodevwebutils.config;

import io.vavr.collection.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.security.config.annotation.web.AbstractRequestMatcherRegistry;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;

import javax.sql.DataSource;

import static org.springframework.security.config.Customizer.withDefaults;
import static org.springframework.security.config.http.MatcherType.mvc;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    final DataSource dataSource;

    public SecurityConfig(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(cust ->
                cust.anyRequest().authenticated())
            .formLogin(withDefaults())
            .rememberMe(cust -> cust
                .key("idE61wAMkY")
                .tokenRepository(persistentTokenRepository()))
            .csrf(AbstractHttpConfigurer::disable)
            .cors(AbstractHttpConfigurer::disable);

        return http.build();
    }

    @Bean
    public PersistentTokenRepository persistentTokenRepository() {
        final var tokenRepository = new JdbcTokenRepositoryImpl();
        tokenRepository.setDataSource(dataSource);
        tokenRepository.setCreateTableOnStartup(true);
        return tokenRepository;
    }

    @Bean
    public UserDetailsService userDetailsService() {
        final var users = List.of(
                "Багрянцев Дмитрий",
                "Власов Павел",
                "Дымко Андрей",
                "Ермолаева Екатерина",
                "Ермош Константин",
                "Кравцов Павел",
                "Скворцов Демьян",
                "Чанчиков Сергей",
                "Саитова Ильсияр",
                "Каюмов Камиль",
                "Климов Максим",
                "Беркутов Алмаз",
                "Габдуллина Гузель",
                "Чурсин Владимир",
                "Смирнова Наталья"
            ).map(name -> User.withDefaultPasswordEncoder()
                .username(name)
                //.passwordEncoder(passwordEncoder().encode(pw))
                .password("1")
                .roles("USER")
                .build())
            .toJavaList();
        return new InMemoryUserDetailsManager(users);
    }

//    @Bean
//    public PasswordEncoder passwordEncoder() {
//        return new BCryptPasswordEncoder();
//    }
}
