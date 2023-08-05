package ru.blogic.muzedodevwebutils;

import io.vavr.collection.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests((authorizeHttpRequests) ->
                authorizeHttpRequests
                    .requestMatchers("/**").hasRole("USER")
            )
            .formLogin(withDefaults())
            .csrf(AbstractHttpConfigurer::disable)
            .cors(AbstractHttpConfigurer::disable);

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        java.util.List<UserDetails> users = List.of(
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
