package br.com.seudominio.foodrescue.core.utils;

import java.util.regex.Pattern;

/**
 * Small validation helpers reused by business validators.
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
public final class ValidationUtils {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private ValidationUtils() {
    }

    /**
     * Checks whether the given value is a syntactically valid email address.
     *
     * @param value the value to check
     * @return {@code true} if {@code value} is a valid email address
     */
    public static boolean isValidEmail(String value) {
        return value != null && EMAIL_PATTERN.matcher(value).matches();
    }
}
