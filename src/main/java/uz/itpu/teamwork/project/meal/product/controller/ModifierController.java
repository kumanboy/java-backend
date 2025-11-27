package uz.itpu.teamwork.project.meal.product.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.itpu.teamwork.project.meal.product.dto.request.ModifierRequest;
import uz.itpu.teamwork.project.meal.product.dto.response.ModifierResponse;
import uz.itpu.teamwork.project.meal.product.service.ModifierService;

import java.util.List;

@RestController
@RequestMapping("/api/modifiers")
@RequiredArgsConstructor
public class ModifierController {

    private final ModifierService modifierService;

    @GetMapping
    public List<ModifierResponse> getAllModifiers() {
        return modifierService.getAllModifiers();
    }

    @GetMapping("/active")
    public List<ModifierResponse> getActiveModifiers() {
        return modifierService.getActiveModifiers();
    }

    @GetMapping("/{id}")
    public ModifierResponse getModifierById(@PathVariable Long id) {
        return modifierService.getModifierById(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ModifierResponse> createModifier(@Valid @RequestBody ModifierRequest request) {
        ModifierResponse response = modifierService.createModifier(request);
        return ResponseEntity.status(201).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ModifierResponse updateModifier(
            @PathVariable Long id,
            @Valid @RequestBody ModifierRequest request
    ) {
        return modifierService.updateModifier(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> deleteModifier(@PathVariable Long id) {
        modifierService.deleteModifier(id);
        return ResponseEntity.noContent().build();
    }
}