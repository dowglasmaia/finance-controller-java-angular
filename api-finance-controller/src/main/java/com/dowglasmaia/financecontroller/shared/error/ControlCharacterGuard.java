package com.dowglasmaia.financecontroller.shared.error;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.module.SimpleModule;

/**
 * Recusa, em qualquer texto recebido, caracteres de controle que não fazem sentido em dados de utilizador (NUL, sino,
 * escape…; tabulação e quebras de linha continuam válidas). Sem isto o PostgreSQL recusa o NUL só na gravação e o
 * cliente recebia um erro interno (QUALITY-SPEC-05); agora é um 400 de validação, como qualquer outro valor inválido.
 */
@Configuration(proxyBeanMethods = false)
class ControlCharacterGuard {

    @Bean
    JacksonModule controlCharacterRejectingStrings() {
        var module = new SimpleModule("control-character-guard");
        module.addDeserializer(String.class, new GuardedStringDeserializer());
        return module;
    }

    static boolean hasForbiddenControl(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            boolean allowedWhitespace = c == '\t' || c == '\n' || c == '\r';
            if ((c < 0x20 && !allowedWhitespace) || c == 0x7F) {
                return true;
            }
        }
        return false;
    }

    static final class GuardedStringDeserializer extends StdDeserializer<String> {
        GuardedStringDeserializer() {
            super(String.class);
        }

        @Override
        public String deserialize(JsonParser parser, DeserializationContext context) throws JacksonException {
            String value = parser.getValueAsString();
            if (value == null) {
                return (String) context.handleUnexpectedToken(String.class, parser);
            }
            if (hasForbiddenControl(value)) {
                throw context.weirdStringException("<redacted>", String.class, "control characters are not allowed");
            }
            return value;
        }
    }
}
