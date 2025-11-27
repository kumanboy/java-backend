package uz.itpu.teamwork.project.meal.product.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.itpu.teamwork.project.meal.product.dto.request.ModifierRequest;
import uz.itpu.teamwork.project.meal.product.dto.response.ModifierResponse;
import uz.itpu.teamwork.project.meal.product.model.Modifier;
import uz.itpu.teamwork.project.meal.product.repository.ModifierRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ModifierService {

    private final ModifierRepository modifierRepository;

    @Transactional(readOnly = true)
    public List<ModifierResponse> getAllModifiers() {
        return modifierRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ModifierResponse> getActiveModifiers() {
        return modifierRepository.findByIsActiveTrue().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ModifierResponse getModifierById(Long id) {
        Modifier modifier = modifierRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Modifier not found with id: " + id));
        return toResponse(modifier);
    }

    @Transactional
    public ModifierResponse createModifier(ModifierRequest request) {
        Modifier modifier = Modifier.builder()
                .name(request.getName())
                .description(request.getDescription())
                .priceAdjustment(request.getPriceAdjustment())
                .isActive(request.getIsActive())
                .build();

        Modifier saved = modifierRepository.save(modifier);
        return toResponse(saved);
    }

    @Transactional
    public ModifierResponse updateModifier(Long id, ModifierRequest request) {
        Modifier modifier = modifierRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Modifier not found with id: " + id));

        modifier.setName(request.getName());
        modifier.setDescription(request.getDescription());
        modifier.setPriceAdjustment(request.getPriceAdjustment());
        modifier.setIsActive(request.getIsActive());

        Modifier updated = modifierRepository.save(modifier);
        return toResponse(updated);
    }

    @Transactional
    public void deleteModifier(Long id) {
        if (!modifierRepository.existsById(id)) {
            throw new RuntimeException("Modifier not found with id: " + id);
        }
        modifierRepository.deleteById(id);
    }

    private ModifierResponse toResponse(Modifier m) {
        return ModifierResponse.builder()
                .id(m.getId())
                .name(m.getName())
                .description(m.getDescription())
                .priceAdjustment(m.getPriceAdjustment())
                .isActive(m.getIsActive())
                .createdAt(m.getCreatedAt())
                .build();
    }
}