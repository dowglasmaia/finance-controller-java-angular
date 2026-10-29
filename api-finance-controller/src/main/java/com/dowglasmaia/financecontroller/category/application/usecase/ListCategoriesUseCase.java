package com.dowglasmaia.financecontroller.category.application.usecase;

import com.dowglasmaia.financecontroller.category.CategoryType;
import com.dowglasmaia.financecontroller.category.application.port.out.CategoryRepository;
import com.dowglasmaia.financecontroller.category.domain.Category;
import com.dowglasmaia.financecontroller.identity.CurrentOwner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Collator;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class ListCategoriesUseCase {

    private final CategoryRepository categories;
    private final CurrentOwner owner;

    ListCategoriesUseCase(CategoryRepository categories, CurrentOwner owner) {
        this.categories = categories;
        this.owner = owner;
    }

    /** Ordem alfabética sem distinguir caixa nem acentos, fixada no Java (independente do collation do banco). */
    @Transactional(readOnly = true)
    public List<Category> execute(CategoryType acceptedKind) {
        Collator collator = Collator.getInstance(Locale.forLanguageTag("pt-BR"));
        collator.setStrength(Collator.PRIMARY);
        return categories.findAll(owner.id(), acceptedKind).stream()
            .sorted(Comparator.comparing(Category::name, collator))
            .toList();
    }
}
