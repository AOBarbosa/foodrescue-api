package br.com.seudominio.perishrescue.core.validation;

/**
 * The business operation a {@code BusinessValidator} is validating for,
 * allowing rules to differ between creating and updating an entity.
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
public enum BusinessOperation {

    CREATE,
    UPDATE
}
