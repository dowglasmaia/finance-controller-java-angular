package com.dowglasmaia.financecontroller.category.api;

import com.dowglasmaia.financecontroller.category.CategoryType;
import com.dowglasmaia.financecontroller.category.application.usecase.AddSubcategoryUseCase;
import com.dowglasmaia.financecontroller.category.application.usecase.CreateCategoryUseCase;
import com.dowglasmaia.financecontroller.category.application.usecase.DeleteCategoryUseCase;
import com.dowglasmaia.financecontroller.category.application.usecase.DeleteSubcategoryUseCase;
import com.dowglasmaia.financecontroller.category.application.usecase.GetCategoryUseCase;
import com.dowglasmaia.financecontroller.category.application.usecase.ListCategoriesUseCase;
import com.dowglasmaia.financecontroller.category.application.usecase.UpdateCategoryUseCase;
import com.dowglasmaia.financecontroller.generated.categories.api.CategoriesApi;
import com.dowglasmaia.financecontroller.generated.categories.model.Category;
import com.dowglasmaia.financecontroller.generated.categories.model.CategoryRequest;
import com.dowglasmaia.financecontroller.generated.categories.model.CreateCategoryRequest;
import com.dowglasmaia.financecontroller.generated.categories.model.Subcategory;
import com.dowglasmaia.financecontroller.generated.categories.model.SubcategoryRequest;
import com.dowglasmaia.financecontroller.shared.error.DomainException;
import com.dowglasmaia.financecontroller.shared.error.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Só mapeia e delega; as regras vivem nos casos de uso. */
@RestController
public class CategoriesController implements CategoriesApi {

    private final ListCategoriesUseCase listCategories;
    private final CreateCategoryUseCase createCategory;
    private final GetCategoryUseCase getCategory;
    private final UpdateCategoryUseCase updateCategory;
    private final DeleteCategoryUseCase deleteCategory;
    private final AddSubcategoryUseCase addSubcategory;
    private final DeleteSubcategoryUseCase deleteSubcategory;

    public CategoriesController(ListCategoriesUseCase listCategories, CreateCategoryUseCase createCategory,
                                GetCategoryUseCase getCategory, UpdateCategoryUseCase updateCategory,
                                DeleteCategoryUseCase deleteCategory, AddSubcategoryUseCase addSubcategory,
                                DeleteSubcategoryUseCase deleteSubcategory) {
        this.listCategories = listCategories;
        this.createCategory = createCategory;
        this.getCategory = getCategory;
        this.updateCategory = updateCategory;
        this.deleteCategory = deleteCategory;
        this.addSubcategory = addSubcategory;
        this.deleteSubcategory = deleteSubcategory;
    }

    @Override
    public ResponseEntity<List<Category>> listCategories(String type) {
        return ResponseEntity.ok(listCategories.execute(parseKind(type)).stream().map(CategoryApiMapper::toResponse).toList());
    }

    @Override
    public ResponseEntity<Category> createCategory(CreateCategoryRequest request) {
        var created = createCategory.execute(CategoryApiMapper.toCommand(request.getName(), request.getType(),
            request.getIcon(), request.getColor()), request.getSubcategories());
        return ResponseEntity.status(HttpStatus.CREATED).body(CategoryApiMapper.toResponse(created));
    }

    @Override
    public ResponseEntity<Category> getCategory(UUID categoryId) {
        return ResponseEntity.ok(CategoryApiMapper.toResponse(getCategory.execute(categoryId)));
    }

    @Override
    public ResponseEntity<Category> updateCategory(UUID categoryId, CategoryRequest request) {
        var updated = updateCategory.execute(categoryId, CategoryApiMapper.toCommand(request.getName(),
            request.getType(), request.getIcon(), request.getColor()));
        return ResponseEntity.ok(CategoryApiMapper.toResponse(updated));
    }

    @Override
    public ResponseEntity<Void> deleteCategory(UUID categoryId) {
        deleteCategory.execute(categoryId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Subcategory> addSubcategory(UUID categoryId, SubcategoryRequest request) {
        var created = addSubcategory.execute(categoryId, request.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(CategoryApiMapper.toResponse(created));
    }

    @Override
    public ResponseEntity<Void> deleteSubcategory(UUID categoryId, UUID subcategoryId) {
        deleteSubcategory.execute(categoryId, subcategoryId);
        return ResponseEntity.noContent().build();
    }

    private static CategoryType parseKind(String type) {
        if (type == null || type.isBlank()) {
            return null;
        }
        return switch (type) {
            case "INCOME" -> CategoryType.INCOME;
            case "EXPENSE" -> CategoryType.EXPENSE;
            default -> throw new DomainException(ErrorCode.VALIDATION_FAILED);
        };
    }
}
