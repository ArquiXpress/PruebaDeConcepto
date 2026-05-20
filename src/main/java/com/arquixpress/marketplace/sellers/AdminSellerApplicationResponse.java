package com.arquixpress.marketplace.sellers;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AdminSellerApplicationResponse(
        UUID id,
        UUID userId,
        String applicantName,
        String applicantEmail,
        String sellerType,
        String legalDocumentType,
        String legalDocumentNumber,
        String documentFileName,
        String documentFileContent,
        String documentFileMimeType,
        String companyName,
        String companyDescription,
        String contactPhone,
        String category,
        List<SellerApplicationRequest.ProductDraftRequest> products,
        String status,
        UUID reviewedBy,
        Instant reviewedAt,
        String reviewNote,
        int approvedProductCount,
        Instant createdAt) {
}
