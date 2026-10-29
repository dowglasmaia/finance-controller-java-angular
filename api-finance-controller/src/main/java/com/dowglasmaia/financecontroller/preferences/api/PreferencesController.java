package com.dowglasmaia.financecontroller.preferences.api;

import com.dowglasmaia.financecontroller.generated.preferences.api.PreferencesApi;
import com.dowglasmaia.financecontroller.generated.preferences.model.Currency;
import com.dowglasmaia.financecontroller.generated.preferences.model.Preferences;
import com.dowglasmaia.financecontroller.generated.preferences.model.Theme;
import com.dowglasmaia.financecontroller.preferences.application.usecase.GetPreferencesUseCase;
import com.dowglasmaia.financecontroller.preferences.application.usecase.SavePreferencesUseCase;
import com.dowglasmaia.financecontroller.preferences.domain.Preference;
import com.dowglasmaia.financecontroller.preferences.domain.ThemeMode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** Só mapeia e delega; as regras vivem nos casos de uso. */
@RestController
public class PreferencesController implements PreferencesApi {

    private final GetPreferencesUseCase getPreferences;
    private final SavePreferencesUseCase savePreferences;

    public PreferencesController(GetPreferencesUseCase getPreferences, SavePreferencesUseCase savePreferences) {
        this.getPreferences = getPreferences;
        this.savePreferences = savePreferences;
    }

    @Override
    public ResponseEntity<Preferences> getPreferences() {
        return ResponseEntity.ok(toResponse(getPreferences.execute()));
    }

    @Override
    public ResponseEntity<Preferences> savePreferences(Preferences request) {
        var saved = savePreferences.execute(
            com.dowglasmaia.financecontroller.shared.money.Currency.valueOf(request.getCurrency().getValue()),
            ThemeMode.valueOf(request.getTheme().getValue()));
        return ResponseEntity.ok(toResponse(saved));
    }

    private static Preferences toResponse(Preference p) {
        return new Preferences(Currency.fromValue(p.currency().name()), Theme.fromValue(p.theme().name()));
    }
}
