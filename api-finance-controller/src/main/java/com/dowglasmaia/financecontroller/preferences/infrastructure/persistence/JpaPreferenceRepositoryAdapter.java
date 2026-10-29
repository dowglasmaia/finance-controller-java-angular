package com.dowglasmaia.financecontroller.preferences.infrastructure.persistence;

import com.dowglasmaia.financecontroller.preferences.application.port.out.PreferenceRepository;
import com.dowglasmaia.financecontroller.preferences.domain.Preference;
import com.dowglasmaia.financecontroller.preferences.domain.ThemeMode;
import com.dowglasmaia.financecontroller.shared.money.Currency;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

@Repository
class JpaPreferenceRepositoryAdapter implements PreferenceRepository {

    private final SpringDataPreferenceRepository preferences;
    private final Clock clock;

    JpaPreferenceRepositoryAdapter(SpringDataPreferenceRepository preferences, Clock clock) {
        this.preferences = preferences;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Preference> find(UUID ownerId) {
        return preferences.find(ownerId).map(JpaPreferenceRepositoryAdapter::toDomain);
    }

    @Override
    @Transactional
    public Preference save(Preference preference) {
        preferences.upsert(UUID.randomUUID(), preference.ownerId(), preference.currency().name(),
            preference.theme().name(), clock.instant());
        return preferences.find(preference.ownerId()).map(JpaPreferenceRepositoryAdapter::toDomain).orElseThrow();
    }

    private static Preference toDomain(PreferenceEntity e) {
        return new Preference(e.getOwnerId(), Currency.valueOf(e.getCurrency()), ThemeMode.valueOf(e.getTheme()));
    }
}
