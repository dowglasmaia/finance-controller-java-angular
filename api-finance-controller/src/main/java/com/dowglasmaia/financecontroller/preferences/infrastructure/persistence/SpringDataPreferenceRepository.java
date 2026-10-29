package com.dowglasmaia.financecontroller.preferences.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Toda consulta recebe o proprietário (P-01). */
interface SpringDataPreferenceRepository extends JpaRepository<PreferenceEntity, UUID> {

    @Query("select p from Preference p where p.ownerId = :ownerId")
    Optional<PreferenceEntity> find(@Param("ownerId") UUID ownerId);

    /** Um só comando: dois PUT simultâneos do mesmo utilizador não colidem na chave única. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "insert into preference (id, owner_id, currency, theme, updated_at) "
        + "values (:id, :ownerId, :currency, :theme, :now) "
        + "on conflict (owner_id) do update set currency = excluded.currency, theme = excluded.theme, "
        + "updated_at = excluded.updated_at", nativeQuery = true)
    void upsert(@Param("id") UUID id, @Param("ownerId") UUID ownerId, @Param("currency") String currency,
                @Param("theme") String theme, @Param("now") Instant now);
}
