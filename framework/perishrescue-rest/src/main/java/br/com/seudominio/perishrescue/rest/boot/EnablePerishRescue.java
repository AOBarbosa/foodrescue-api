package br.com.seudominio.perishrescue.rest.boot;

import org.springframework.context.annotation.Import;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Plugs the PerishRescue framework kernel into an instance application.
 *
 * <p>Put it next to {@code @SpringBootApplication} on the instance's main
 * class. The instance keeps its own package (e.g.
 * {@code br.com.seudominio.foodrescue}) for the classes that fill the hot
 * spots; the framework's components, entities and repositories are found
 * through {@link PerishRescueConfiguration}.</p>
 *
 * @author Clovis Medeiros
 * @since 1.0.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(PerishRescueConfiguration.class)
public @interface EnablePerishRescue {
}
