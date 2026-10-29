package com.dowglasmaia.financecontroller.identity.application.port.out;

/** Porta de saída de e-mail. A implementação tolera falhas: o chamador nunca vê erro de envio. */
public interface MailSender {

    void send(MailMessage message);
}
