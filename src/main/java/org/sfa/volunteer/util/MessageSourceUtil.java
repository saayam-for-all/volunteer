package org.sfa.volunteer.util;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class MessageSourceUtil {

    private static MessageSource messageSource;

    public MessageSourceUtil(MessageSource messageSource) {
        MessageSourceUtil.messageSource = messageSource;
    }

    public static String getMessage(String code, Object[] args) {
        Locale locale = LocaleContextHolder.getLocale();
        return messageSource.getMessage(code, args, locale);
    }
}
