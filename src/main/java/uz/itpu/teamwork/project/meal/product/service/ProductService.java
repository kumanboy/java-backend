package uz.itpu.teamwork.project.meal.product.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.itpu.teamwork.project.meal.category.model.Category;
import uz.itpu.teamwork.project.meal.category.repository.CategoryRepository;
import uz.itpu.teamwork.project.meal.country.model.Country;
import uz.itpu.teamwork.project.meal.country.repository.CountryRepository;
import uz.itpu.teamwork.project.exception.ResourceNotFoundException;
import uz.itpu.teamwork.project.meal.product.dto.request.ProductIngredientRequest;
import uz.itpu.teamwork.project.meal.product.dto.request.ProductRequest;
import uz.itpu.teamwork.project.meal.product.dto.response.ProductResponse;
import uz.itpu.teamwork.project.meal.product.model.*;
import uz.itpu.teamwork.project.meal.product.repository.IngredientRepository;
import uz.itpu.teamwork.project.meal.product.repository.ModifierRepository;
import uz.itpu.teamwork.project.meal.product.repository.ProductRepository;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final IngredientRepository ingredientRepository;
    private final ModifierRepository modifierRepository;
    private final CountryRepository countryRepository;

    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getProducts(Pageable pageable) {
        return productRepository.findAll(pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
        return toResponse(product);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getProductsByCategory(Long categoryId) {
        return productRepository.findByCategoryId(categoryId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getActiveProducts() {
        return productRepository.findByIsActiveTrue().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> searchProducts(String searchTerm) {
        return productRepository.searchProducts(searchTerm).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> filterProducts(
            Long categoryId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String countryCode,
            Boolean isActive
    ) {
        Specification<Product> spec = null;

        if (categoryId != null) {
            spec = byCategory(categoryId);
        }

        if (minPrice != null && maxPrice != null) {
            spec = (spec == null ? byPriceRange(minPrice, maxPrice) : spec.and(byPriceRange(minPrice, maxPrice)));
        }

        if (isActive != null) {
            spec = (spec == null ? byActive(isActive) : spec.and(byActive(isActive)));
        }

        List<Product> products = (spec == null)
                ? productRepository.findAll()
                : productRepository.findAll(spec);

        if (countryCode != null) {
            products = products.stream()
                    .filter(p -> isProductAvailableInCountry(p, countryCode))
                    .toList();
        }

        return products.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private Specification<Product> byCategory(Long categoryId) {
        return (root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId);
    }

    private Specification<Product> byPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        return (root, query, cb) -> cb.between(root.get("basePrice"), minPrice, maxPrice);
    }

    private Specification<Product> byActive(Boolean isActive) {
        return (root, query, cb) -> cb.equal(root.get("isActive"), isActive);
    }


    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setBasePrice(request.getBasePrice());
        product.setIsActive(request.getIsActive());

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));
            product.setCategory(category);
        }

        Product savedProduct = productRepository.save(product);

        if (request.getIngredients() != null) {
            addIngredientsToProduct(savedProduct, request.getIngredients());
        }

        if (request.getAvailability() != null) {
            addAvailabilityToProduct(savedProduct, request.getAvailability());
        }

        if (request.getModifierIds() != null) {
            Set<Modifier> modifiers = new HashSet<>(
                    modifierRepository.findAllById(request.getModifierIds())
            );
            if (modifiers.size() != request.getModifierIds().size()) {
                throw new ResourceNotFoundException("One or more modifiers not found");
            }
            savedProduct.setModifiers(modifiers);
            savedProduct = productRepository.save(savedProduct);
        }

        return toResponse(savedProduct);
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setBasePrice(request.getBasePrice());
        product.setIsActive(request.getIsActive());

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));
            product.setCategory(category);
        } else {
            product.setCategory(null);
        }

        if (request.getIngredients() != null) {
            product.getProductIngredients().clear();
            productRepository.saveAndFlush(product);
            addIngredientsToProduct(product, request.getIngredients());
        }

        if (request.getAvailability() != null) {
            product.getProductAvailabilities().clear();
            productRepository.saveAndFlush(product);
            addAvailabilityToProduct(product, request.getAvailability());
        }

        if (request.getModifierIds() != null) {
            Set<Modifier> modifiers = new HashSet<>(
                    modifierRepository.findAllById(request.getModifierIds())
            );
            product.setModifiers(modifiers);
        }

        Product updated = productRepository.save(product);
        return toResponse(updated);
    }

    @Transactional
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product", "id", id);
        }
        productRepository.deleteById(id);
    }

    @Transactional
    public ProductResponse updateProductImage(Long id, String imageUrl) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));

        product.setImageUrl(imageUrl);
        Product updated = productRepository.save(product);
        return toResponse(updated);
    }

    private void addIngredientsToProduct(Product product, List<ProductIngredientRequest> ingredientRequests) {
        for (ProductIngredientRequest req : ingredientRequests) {
            Ingredient ingredient = ingredientRepository.findById(req.getIngredientId())
                    .orElseThrow(() -> new ResourceNotFoundException("Ingredient", "id", req.getIngredientId()));

            ProductIngredient productIngredient = ProductIngredient.builder()
                    .product(product)
                    .ingredient(ingredient)
                    .quantity(req.getQuantity())
                    .build();

            product.getProductIngredients().add(productIngredient);
        }
    }

    private void addAvailabilityToProduct(Product product, Map<String, Boolean> availability) {
        for (Map.Entry<String, Boolean> entry : availability.entrySet()) {
            Country country = countryRepository.findByCode(entry.getKey())
                    .orElseThrow(() -> new ResourceNotFoundException("Country", "code", entry.getKey()));

            ProductAvailability productAvailability = ProductAvailability.builder()
                    .product(product)
                    .country(country)
                    .isAvailable(entry.getValue())
                    .build();

            product.getProductAvailabilities().add(productAvailability);
        }
    }

    private boolean isProductAvailableInCountry(Product product, String countryCode) {
        return product.getProductAvailabilities().stream()
                .anyMatch(pa -> pa.getCountry().getCode().equals(countryCode) && pa.getIsAvailable());
    }

    private ProductResponse toResponse(Product p) {
        Map<String, ProductResponse.AvailabilityInfo> availabilityMap = p.getProductAvailabilities().stream()
                .collect(Collectors.toMap(
                        pa -> pa.getCountry().getCode(),
                        pa -> ProductResponse.AvailabilityInfo.builder()
                                .isAvailable(pa.getIsAvailable())
                                .stockQuantity(pa.getStockQuantity())
                                .countryName(pa.getCountry().getName())
                                .build()
                ));

        List<ProductResponse.IngredientInfo> ingredients = p.getProductIngredients().stream()
                .map(pi -> ProductResponse.IngredientInfo.builder()
                        .id(pi.getIngredient().getId())
                        .name(pi.getIngredient().getName())
                        .quantity(pi.getQuantity())
                        .isAllergen(pi.getIngredient().getIsAllergen())
                        .build())
                .collect(Collectors.toList());

        List<ProductResponse.ModifierInfo> modifiers = p.getModifiers().stream()
                .map(m -> ProductResponse.ModifierInfo.builder()
                        .id(m.getId())
                        .name(m.getName())
                        .description(m.getDescription())
                        .priceAdjustment(m.getPriceAdjustment())
                        .isActive(m.getIsActive())
                        .build())
                .collect(Collectors.toList());

        return ProductResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .description(p.getDescription())
                .categoryId(p.getCategory() != null ? p.getCategory().getId() : null)
                .categoryName(p.getCategory() != null ? p.getCategory().getName() : null)
                .basePrice(p.getBasePrice())
                .imageUrl(p.getImageUrl())
                .isActive(p.getIsActive())
                .rating(p.getRating() != null ? p.getRating() : BigDecimal.ZERO)
                .ratingCount(p.getRatingCount() != null ? p.getRatingCount() : 0)

                .ingredients(ingredients)
                .availability(availabilityMap)
                .modifiers(modifiers)
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }

}