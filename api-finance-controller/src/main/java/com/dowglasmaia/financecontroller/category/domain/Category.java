package com.dowglasmaia.financecontroller.category.domain;

import com.dowglasmaia.financecontroller.category.CategoryType;
import com.dowglasmaia.financecontroller.shared.error.DomainException;
import com.dowglasmaia.financecontroller.shared.error.ErrorCode;
import com.dowglasmaia.financecontroller.shared.error.ResourceNotFoundException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

/** Categoria com as suas subcategorias; nomes únicos ignorando caixa e espaços, e sempre uma "Outros" protegida. */
public record Category(UUID id, UUID ownerId, String name, CategoryType type, String icon, String color,
                       List<Subcategory> subcategories) {

    public static final String OTHERS = "Outros";
    private static final int MAX_NAME = 60;
    private static final int MAX_ICON = 40;
    private static final Pattern COLOR = Pattern.compile("^#[0-9A-Fa-f]{6}$");

    public Category {
        name = requireName(name);
        if (ownerId == null || id == null || type == null || icon == null || icon.isBlank() || icon.length() > MAX_ICON
                || color == null || !COLOR.matcher(color).matches()) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED);
        }
        subcategories = List.copyOf(subcategories);
    }

    /** Nova categoria com "Outros" e as subcategorias informadas ("Outros" e repetidas são ignoradas). */
    public static Category create(UUID ownerId, String name, CategoryType type, String icon, String color,
                                  List<String> subcategoryNames) {
        var subs = new ArrayList<Subcategory>();
        subs.add(new Subcategory(UUID.randomUUID(), OTHERS, true));
        if (subcategoryNames != null) {
            for (String raw : subcategoryNames) {
                var sub = new Subcategory(UUID.randomUUID(), raw, false);
                if (subs.stream().noneMatch(s -> key(s.name()).equals(key(sub.name())))) {
                    subs.add(sub);
                }
            }
        }
        return new Category(UUID.randomUUID(), ownerId, name, type, icon, color, subs);
    }

    public Category withDetails(String newName, CategoryType newType, String newIcon, String newColor) {
        return new Category(id, ownerId, newName, newType, newIcon, newColor, subcategories);
    }

    /** @throws DomainException {@code SUBCATEGORY_NAME_DUPLICATED} se o nome já existe nesta categoria */
    public Category addSubcategory(String subcategoryName) {
        var sub = new Subcategory(UUID.randomUUID(), subcategoryName, false);
        if (subcategories.stream().anyMatch(s -> key(s.name()).equals(key(sub.name())))) {
            throw new DomainException(ErrorCode.SUBCATEGORY_NAME_DUPLICATED);
        }
        var all = new ArrayList<>(subcategories);
        all.add(sub);
        return new Category(id, ownerId, name, type, icon, color, all);
    }

    /**
     * @throws ResourceNotFoundException se a subcategoria não pertence à categoria
     * @throws DomainException {@code SUBCATEGORY_PROTECTED} para "Outros"
     */
    public Category removeSubcategory(UUID subcategoryId) {
        var target = subcategories.stream().filter(s -> s.id().equals(subcategoryId)).findFirst()
            .orElseThrow(ResourceNotFoundException::new);
        if (target.isProtected()) {
            throw new DomainException(ErrorCode.SUBCATEGORY_PROTECTED);
        }
        return new Category(id, ownerId, name, type, icon, color,
            subcategories.stream().filter(s -> !s.id().equals(subcategoryId)).toList());
    }

    /** Chave de unicidade: aparada e em minúsculas, como a coluna {@code name_key}. */
    public static String key(String value) {
        return value.strip().toLowerCase(Locale.ROOT);
    }

    static String requireName(String raw) {
        if (raw == null || raw.isBlank() || raw.strip().length() > MAX_NAME) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED);
        }
        return raw.strip();
    }
}
