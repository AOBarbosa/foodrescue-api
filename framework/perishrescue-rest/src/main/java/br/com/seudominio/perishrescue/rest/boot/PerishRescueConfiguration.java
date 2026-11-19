package br.com.seudominio.perishrescue.rest.boot;

import org.springframework.boot.autoconfigure.AutoConfigurationExcludeFilter;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
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
 * <p>The scan skips the framework's auto-configuration classes (the default
 * hot spot implementations): Spring Boot processes those after the
 * instance's beans, which is what lets an instance override a default just
 * by declaring its own bean.</p>
 *
 * @author Clovis Medeiros
 * @since 1.0.0
 */
@Configuration(proxyBeanMethods = false)
@AutoConfigurationPackage(basePackages = PerishRescueConfiguration.FRAMEWORK_PACKAGE)
@ComponentScan(value = PerishRescueConfiguration.FRAMEWORK_PACKAGE, excludeFilters = {
        @ComponentScan.Filter(type = FilterType.CUSTOM, classes = TypeExcludeFilter.class),
        @ComponentScan.Filter(type = FilterType.CUSTOM, classes = AutoConfigurationExcludeFilter.class)
})
@EnableScheduling
public class PerishRescueConfiguration {

    /**
     * Root package of every framework module.
     */
    public static final String FRAMEWORK_PACKAGE = "br.com.seudominio.perishrescue";
}
