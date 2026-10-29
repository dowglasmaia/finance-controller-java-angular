package com.dowglasmaia.financecontroller.identity;

import java.util.UUID;

/**
 * API pública do módulo: publicado, dentro da transação do cadastro, quando um utilizador é criado. Os módulos de
 * dados reagem a ele (por exemplo, a semente de categorias) sem que {@code identity} dependa deles.
 */
public record UserRegistered(UUID userId) {
}
