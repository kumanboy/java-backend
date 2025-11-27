package uz.itpu.teamwork.project.meal.product.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.itpu.teamwork.project.meal.product.dto.request.IngredientRequest;
import uz.itpu.teamwork.project.meal.product.dto.response.IngredientResponse;
import uz.itpu.teamwork.project.meal.product.service.IngredientService;

import java.util.List;

@RestController
@RequestMapping("/api/ingredients")
@RequiredArgsConstructor
public class IngredientController {

    private final IngredientService ingredientService;

    @GetMapping
    public List<IngredientResponse> getAllIngredients() {
        return ingredientService.getAllIngredients();
    }

    @GetMapping("/{id}")
    public IngredientResponse getIngredientById(@PathVariable Long id) {
        return ingredientService.getIngredientById(id);
    }

    @GetMapping("/allergens")
    public List<IngredientResponse> getAllergens() {
        return ingredientService.getAllergens();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<IngredientResponse> createIngredient(@Valid @RequestBody IngredientRequest request) {
        IngredientResponse response = ingredientService.createIngredient(request);
        return ResponseEntity.status(201).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public IngredientResponse updateIngredient(
            @PathVariable Long id,
            @Valid @RequestBody IngredientRequest request
    ) {
        return ingredientService.updateIngredient(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> deleteIngredient(@PathVariable Long id) {
        ingredientService.deleteIngredient(id);
        return ResponseEntity.noContent().build();
    }
}