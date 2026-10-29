package com.dowglasmaia.financecontroller.identity.application.port.out;

/** E-mail a enviar, com texto simples e HTML. Nunca é registado em log. */
public record MailMessage(String to, String subject, String text, String html) {

    @Override
    public String toString() {
        return "MailMessage[redacted]";
    }
}
