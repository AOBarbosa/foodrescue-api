package br.com.seudominio.perishrescue.rest.boot;

import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Kernel configuration imported by {@link EnablePerishRescue}.
 *
 * <ul>
 *   <li>{@link ComponentScan} registers the framework's services, validators,
 *       mappers, schedulers, controllers and security beans.</li>
 *   <li>{@link AutoConfigurationPackage} adds the framework package to the
 *       auto-configuration packages, so Spring Boot scans the framework's JPA
 *       entities and repositories <em>in addition to</em> the instance's own
 *       package. An explicit {@code @EntityScan}/{@code @EnableJpaRepositories}
 *       here would replace the instance's package instead of adding to it.</li>
 *   <li>{@link EnableScheduling} turns on the kernel's periodic jobs.</li>
 * </ul>
 *
 * @author Clovis Medeiros
 * @since 1.0.0
 */
@Configuration(proxyBeanMethods = false)
@AutoConfigurationPackage(basePackages = PerishRescueConfiguration.FRAMEWORK_PACKAGE)
@ComponentScan(PerishRescueConfiguration.FRAMEWORK_PACKAGE)
@EnableScheduling
public class PerishRescueConfiguration {

    /**
     * Root package of every framework module.
     */
    public static final String FRAMEWORK_PACKAGE = "br.com.seudominio.perishrescue";
}
