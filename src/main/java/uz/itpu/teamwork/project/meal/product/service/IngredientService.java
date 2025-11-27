package uz.itpu.teamwork.project.meal.product.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.itpu.teamwork.project.meal.product.dto.request.IngredientRequest;
import uz.itpu.teamwork.project.meal.product.dto.response.IngredientResponse;
import uz.itpu.teamwork.project.meal.product.model.Ingredient;
import uz.itpu.teamwork.project.meal.product.repository.IngredientRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IngredientService {

    private final IngredientRepository ingredientRepository;

    @Transactional(readOnly = true)
    public List<IngredientResponse> getAllIngredients() {
        return ingredientRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public IngredientResponse getIngredientById(Long id) {
        Ingredient ingredient = ingredientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ingredient not found with id: " + id));
        return toResponse(ingredient);
    }

    @Transactional(readOnly = true)
    public List<IngredientResponse> getAllergens() {
        return ingredientRepository.findByIsAllergenTrue().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public IngredientResponse createIngredient(IngredientRequest request) {
        if (ingredientRepository.existsByName(request.getName())) {
            throw new RuntimeException("Ingredient with name '" + request.getName() + "' already exists");
        }

        Ingredient ingredient = Ingredient.builder()
                .name(request.getName())
                .isAllergen(request.getIsAllergen())
                .build();

        Ingredient saved = ingredientRepository.save(ingredient);
        return toResponse(saved);
    }

    @Transactional
    public IngredientResponse updateIngredient(Long id, IngredientRequest request) {
        Ingredient ingredient = ingredientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ingredient not found with id: " + id));

        if (!ingredient.getName().equals(request.getName()) &&
                ingredientRepository.existsByName(request.getName())) {
            throw new RuntimeException("Ingredient with name '" + request.getName() + "' already exists");
        }

        ingredient.setName(request.getName());
        ingredient.setIsAllergen(request.getIsAllergen());

        Ingredient updated = ingredientRepository.save(ingredient);
        return toResponse(updated);
    }

    @Transactional
    public void deleteIngredient(Long id) {
        if (!ingredientRepository.existsById(id)) {
            throw new RuntimeException("Ingredient not found with id: " + id);
        }
        ingredientRepository.deleteById(id);
    }

    private IngredientResponse toResponse(Ingredient i) {
        return IngredientResponse.builder()
                .id(i.getId())
                .name(i.getName())
                .isAllergen(i.getIsAllergen())
                .createdAt(i.getCreatedAt())
                .build();
    }
}