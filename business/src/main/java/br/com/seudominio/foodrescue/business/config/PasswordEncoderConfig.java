package br.com.seudominio.foodrescue.business.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Provides the {@link PasswordEncoder} bean shared by every service that
 * hashes or checks passwords (establishments and consumers alike).
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
@Configuration
public class PasswordEncoderConfig {

    /**
     * Creates the application's {@link PasswordEncoder}.
     *
     * @return a {@link BCryptPasswordEncoder}
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
