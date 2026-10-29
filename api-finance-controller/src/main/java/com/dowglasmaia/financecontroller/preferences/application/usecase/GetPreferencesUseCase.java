package com.dowglasmaia.financecontroller.preferences.application.usecase;

import com.dowglasmaia.financecontroller.identity.CurrentOwner;
import com.dowglasmaia.financecontroller.preferences.application.port.out.PreferenceRepository;
import com.dowglasmaia.financecontroller.preferences.domain.Preference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Devolve o que o utilizador gravou, ou os padrões (BRL e DARK) sem gravar nada. */
@Service
public class GetPreferencesUseCase {

    private final PreferenceRepository preferences;
    private final CurrentOwner owner;

    GetPreferencesUseCase(PreferenceRepository preferences, CurrentOwner owner) {
        this.preferences = preferences;
        this.owner = owner;
    }

    @Transactional(readOnly = true)
    public Preference execute() {
        var ownerId = owner.id();
        return preferences.find(ownerId).orElseGet(() -> Preference.defaults(ownerId));
    }
}
