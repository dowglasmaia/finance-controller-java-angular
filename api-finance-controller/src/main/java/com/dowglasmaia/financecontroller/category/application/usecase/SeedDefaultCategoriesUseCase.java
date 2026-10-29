package com.dowglasmaia.financecontroller.category.application.usecase;

import com.dowglasmaia.financecontroller.category.CategoryType;
import com.dowglasmaia.financecontroller.category.application.port.out.CategoryRepository;
import com.dowglasmaia.financecontroller.category.domain.Category;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Semeia as 11 categorias padrão do utilizador (DESC-SEE-01). Idempotente: categorias que já existem pelo nome
 * são preservadas. O proprietário é explícito porque o cadastro ainda não tem utilizador autenticado.
 */
@Service
public class SeedDefaultCategoriesUseCase {

    private record Seed(String name, CategoryType type, String icon, String color, List<String> subcategories) {
    }

    private static final List<Seed> DEFAULTS = List.of(
        new Seed("Carro", CategoryType.EXPENSE, "directions_car", "#FF9800",
            List.of("Combustível", "Manutenção", "Seguro", "Multas")),
        new Seed("Alimentação", CategoryType.EXPENSE, "restaurant", "#4CAF50",
            List.of("Supermercado", "Restaurante", "Delivery", "Padaria")),
        new Seed("Moradia", CategoryType.EXPENSE, "home", "#2196F3",
            List.of("Aluguel", "Condomínio", "Energia", "Água", "Internet")),
        new Seed("Saúde", CategoryType.EXPENSE, "favorite", "#F44336",
            List.of("Médico", "Farmácia", "Plano de Saúde", "Academia")),
        new Seed("Lazer", CategoryType.EXPENSE, "sports_esports", "#9C27B0",
            List.of("Cinema", "Viagem", "Streaming", "Esporte")),
        new Seed("Educação", CategoryType.EXPENSE, "school", "#3F51B5",
            List.of("Curso", "Livros", "Assinatura")),
        new Seed("Vestuário", CategoryType.EXPENSE, "checkroom", "#795548",
            List.of("Roupas", "Calçados", "Acessórios")),
        new Seed("Salário", CategoryType.INCOME, "work", "#009688",
            List.of("CLT", "PJ", "Freelance")),
        new Seed("Vale Alimentação", CategoryType.INCOME, "lunch_dining", "#8BC34A",
            List.of("VA", "VR")),
        new Seed("Investimento", CategoryType.INCOME, "trending_up", "#FFC107",
            List.of("Dividendos", "Renda Fixa", "FIIs")),
        new Seed("Bônus", CategoryType.INCOME, "card_giftcard", "#00BCD4",
            List.of("PLR", "Comissão")));

    private final CategoryRepository categories;

    SeedDefaultCategoriesUseCase(CategoryRepository categories) {
        this.categories = categories;
    }

    @Transactional
    public void execute(UUID ownerId) {
        for (Seed seed : DEFAULTS) {
            if (!categories.existsByName(ownerId, seed.name(), null)) {
                categories.save(Category.create(ownerId, seed.name(), seed.type(), seed.icon(), seed.color(),
                    seed.subcategories()));
            }
        }
    }
}
