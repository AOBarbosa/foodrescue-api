package br.com.seudominio.foodrescue.core.utils;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

/**
 * Utility for resolving user-facing messages from the application's message bundle.
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
@Component
public class MessageUtils {

    private final MessageSource messageSource;

    /**
     * Constructs a new MessageUtils with the given message source.
     *
     * @param messageSource the Spring message source
     */
    public MessageUtils(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    /**
     * Resolves a message by its code, using the current locale.
     *
     * @param code the message code
     * @param args the message arguments
     * @return the resolved message
     */
    public String getMessage(String code, Object... args) {
        return messageSource.getMessage(code, args, LocaleContextHolder.getLocale());
    }
}
