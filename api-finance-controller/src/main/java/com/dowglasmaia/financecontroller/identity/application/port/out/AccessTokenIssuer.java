package com.dowglasmaia.financecontroller.identity.application.port.out;

import com.dowglasmaia.financecontroller.identity.domain.User;

public interface AccessTokenIssuer {

    /** Emite o token de acesso (JWT de vida curta) cujo sujeito é o identificador do utilizador. */
    String issue(User user);
}
