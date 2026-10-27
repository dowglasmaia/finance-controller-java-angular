package com.dowglasmaia.financecontroller.identity;

import java.util.UUID;

/**
 * API pública do módulo: o proprietário autenticado da requisição, vindo do token (nunca de parâmetro ou corpo).
 * Os repositórios dos demais módulos filtram por este identificador (P-01).
 */
public interface CurrentOwner {

    /** @throws com.dowglasmaia.financecontroller.shared.error.DomainException {@code UNAUTHENTICATED} sem autenticação */
    UUID id();
}
