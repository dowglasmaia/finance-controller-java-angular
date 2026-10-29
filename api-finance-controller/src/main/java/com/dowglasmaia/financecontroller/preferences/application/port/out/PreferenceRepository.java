package com.dowglasmaia.financecontroller.preferences.application.port.out;

import com.dowglasmaia.financecontroller.preferences.domain.Preference;

import java.util.Optional;
import java.util.UUID;

/** Porta de saída; toda operação é do proprietário (P-01). */
public interface PreferenceRepository {

    Optional<Preference> find(UUID ownerId);

    /** Grava ou substitui as preferências do proprietário; dois pedidos simultâneos não geram erro. */
    Preference save(Preference preference);
}
