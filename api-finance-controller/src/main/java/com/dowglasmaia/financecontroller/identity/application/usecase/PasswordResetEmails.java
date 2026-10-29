package com.dowglasmaia.financecontroller.identity.application.usecase;

import com.dowglasmaia.financecontroller.identity.application.port.out.MailMessage;

import java.time.Duration;

/** Modelos dos e-mails (pt-BR), em texto e HTML simples. Nunca levam a senha. */
final class PasswordResetEmails {

    private PasswordResetEmails() {
    }

    static MailMessage resetLink(String to, String link, Duration ttl) {
        String validity = minutes(ttl);
        String text = "Olá,\n\n"
            + "Recebemos um pedido para redefinir a senha da sua conta no Finance Controller.\n\n"
            + "Para criar uma nova senha, abra o link abaixo (válido por " + validity + " e de uso único):\n"
            + link + "\n\n"
            + "Se não foi você, ignore esta mensagem: nada será alterado.\n\n"
            + "Finance Controller";
        String html = "<p>Olá,</p>"
            + "<p>Recebemos um pedido para redefinir a senha da sua conta no Finance Controller.</p>"
            + "<p>Para criar uma nova senha, use o link abaixo (válido por " + validity + " e de uso único):</p>"
            + "<p><a href=\"" + escape(link) + "\">Redefinir minha senha</a></p>"
            + "<p>Se não foi você, ignore esta mensagem: nada será alterado.</p>"
            + "<p>Finance Controller</p>";
        return new MailMessage(to, "Redefinição de senha - Finance Controller", text, html);
    }

    static MailMessage passwordChanged(String to) {
        String text = "Olá,\n\n"
            + "A senha da sua conta no Finance Controller foi alterada e todas as sessões abertas foram encerradas.\n\n"
            + "Se não foi você, peça um novo link em \"Esqueci minha senha\" imediatamente.\n\n"
            + "Finance Controller";
        String html = "<p>Olá,</p>"
            + "<p>A senha da sua conta no Finance Controller foi alterada e todas as sessões abertas foram encerradas.</p>"
            + "<p>Se não foi você, peça um novo link em &quot;Esqueci minha senha&quot; imediatamente.</p>"
            + "<p>Finance Controller</p>";
        return new MailMessage(to, "Sua senha foi alterada - Finance Controller", text, html);
    }

    private static String minutes(Duration ttl) {
        long minutes = Math.max(1, ttl.toMinutes());
        return minutes + (minutes == 1 ? " minuto" : " minutos");
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
