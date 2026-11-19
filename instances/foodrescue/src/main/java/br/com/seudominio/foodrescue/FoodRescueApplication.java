package br.com.seudominio.foodrescue;

import br.com.seudominio.perishrescue.rest.boot.EnablePerishRescue;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Locale;

/**
 * FoodRescue: the first instance of the PerishRescue framework, for food and
 * ready meals rescued before the offerer closes.
 *
 * <p>{@link EnablePerishRescue} plugs the framework kernel in; everything the
 * instance customizes lives under {@code br.com.seudominio.foodrescue}.</p>
 *
 * @author Clovis Medeiros
 * @since 1.0.0
 */
@SpringBootApplication
@EnablePerishRescue
public class FoodRescueApplication {

    public static void main(String[] args) {
        // Keeps validation messages in English regardless of the host machine's locale.
        Locale.setDefault(Locale.US);
        SpringApplication.run(FoodRescueApplication.class, args);
    }

}
