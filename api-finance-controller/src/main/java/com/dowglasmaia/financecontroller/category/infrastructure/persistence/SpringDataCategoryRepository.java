package com.dowglasmaia.financecontroller.category.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Toda consulta recebe o proprietário (P-01). */
interface SpringDataCategoryRepository extends JpaRepository<CategoryEntity, UUID> {

    @Query("select distinct c from CategoryEntity c left join fetch c.subcategories "
        + "where c.ownerId = :ownerId and c.type in :types")
    List<CategoryEntity> findAllByOwnerAndTypes(@Param("ownerId") UUID ownerId, @Param("types") Collection<String> types);

    @Query("select c from CategoryEntity c left join fetch c.subcategories where c.ownerId = :ownerId and c.id = :id")
    Optional<CategoryEntity> findByOwnerAndId(@Param("ownerId") UUID ownerId, @Param("id") UUID id);

    @Query("select distinct c from CategoryEntity c left join fetch c.subcategories where c.ownerId = :ownerId and c.id in :ids")
    List<CategoryEntity> findAllByOwnerAndIds(@Param("ownerId") UUID ownerId, @Param("ids") Collection<UUID> ids);

    @Query("select count(c) > 0 from CategoryEntity c where c.ownerId = :ownerId and c.nameKey = :nameKey")
    boolean existsByOwnerAndNameKey(@Param("ownerId") UUID ownerId, @Param("nameKey") String nameKey);

    @Query("select count(c) > 0 from CategoryEntity c "
        + "where c.ownerId = :ownerId and c.nameKey = :nameKey and c.id <> :excludingId")
    boolean existsByOwnerAndNameKeyExcluding(@Param("ownerId") UUID ownerId, @Param("nameKey") String nameKey,
                                             @Param("excludingId") UUID excludingId);
}
