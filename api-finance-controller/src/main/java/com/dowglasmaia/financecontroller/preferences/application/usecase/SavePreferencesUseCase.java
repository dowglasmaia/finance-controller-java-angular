package com.dowglasmaia.financecontroller.preferences.application.usecase;

import com.dowglasmaia.financecontroller.identity.CurrentOwner;
import com.dowglasmaia.financecontroller.preferences.application.port.out.PreferenceRepository;
import com.dowglasmaia.financecontroller.preferences.domain.Preference;
import com.dowglasmaia.financecontroller.preferences.domain.ThemeMode;
import com.dowglasmaia.financecontroller.shared.money.Currency;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SavePreferencesUseCase {

    private final PreferenceRepository preferences;
    private final CurrentOwner owner;

    SavePreferencesUseCase(PreferenceRepository preferences, CurrentOwner owner) {
        this.preferences = preferences;
        this.owner = owner;
    }

    @Transactional
    public Preference execute(Currency currency, ThemeMode theme) {
        return preferences.save(new Preference(owner.id(), currency, theme));
    }
}
