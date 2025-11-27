package uz.itpu.teamwork.project.meal.product.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import uz.itpu.teamwork.project.meal.common.storage.FileStorage;
import uz.itpu.teamwork.project.meal.product.dto.response.ProductResponse;
import uz.itpu.teamwork.project.meal.product.service.ProductService;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductImageController {

    private final ProductService productService;
    private final FileStorage fileStorage;

    @PostMapping("/{id}/image")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ProductResponse> uploadProductImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file
    ) {
        try {
            String imageUrl = fileStorage.uploadFile(file, "products");
            ProductResponse response = productService.updateProductImage(id, imageUrl);
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload image: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}/image")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Map<String, String>> deleteProductImage(@PathVariable Long id) {
        ProductResponse product = productService.getProductById(id);

        if (product.getImageUrl() != null) {
            try {
                fileStorage.deleteFile(product.getImageUrl());
                productService.updateProductImage(id, null);
            } catch (IOException e) {
                throw new RuntimeException("Failed to delete image: " + e.getMessage());
            }
        }

        Map<String, String> response = new HashMap<>();
        response.put("message", "Image deleted successfully");
        return ResponseEntity.ok(response);
    }
}