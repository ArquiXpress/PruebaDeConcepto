package com.arquixpress.marketplace.sellers;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.util.List;

public record SellerApplicationRequest(
        @NotBlank String sellerType,
        @NotBlank String legalDocumentType,
        @NotBlank String legalDocumentNumber,
        String documentFileName,
        String documentFileContent,
        String documentFileMimeType,
        String companyName,
        String companyDescription,
        String contactPhone,
        @NotBlank String category,
        List<ProductDraftRequest> products) {

    public record ProductDraftRequest(
            @NotBlank String title,
            @NotBlank String description,
            String imageUrl,
            List<String> imageUrls,
            BigDecimal price,
            Integer stockAvailable) {
    }
}
