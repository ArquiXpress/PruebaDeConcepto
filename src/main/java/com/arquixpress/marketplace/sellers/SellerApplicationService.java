package com.arquixpress.marketplace.sellers;

import com.arquixpress.marketplace.catalog.ProductStatus;
import com.arquixpress.marketplace.identity.AppUser;
import com.arquixpress.marketplace.identity.AppUserRepository;
import com.arquixpress.marketplace.identity.CurrentUser;
import com.arquixpress.marketplace.identity.Role;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class SellerApplicationService {
    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final AppUserRepository userRepository;

    public SellerApplicationService(NamedParameterJdbcTemplate jdbc, ObjectMapper mapper,
            AppUserRepository userRepository) {
        this.jdbc = jdbc;
        this.mapper = mapper;
        this.userRepository = userRepository;
    }

    public SellerApplicationResponse create(CurrentUser user, SellerApplicationRequest request) {
        String sellerType = request.sellerType().trim().toUpperCase();
        int minimumProducts = sellerType.equals("JURIDICA") ? 3 : 1;
        if (request.products().size() < minimumProducts) {
            throw new IllegalArgumentException("Debes cargar al menos " + minimumProducts + " producto(s)");
        }
        if (sellerType.equals("JURIDICA") && !StringUtils.hasText(request.companyName())) {
            throw new IllegalArgumentException("El nombre de la empresa es obligatorio");
        }
        request.products().forEach(product -> {
            if (productImages(product).isEmpty()) {
                throw new IllegalArgumentException("Cada producto debe tener al menos una foto");
            }
        });

        UUID id = UUID.randomUUID();
        Instant createdAt = Instant.now();
        String productsJson = toJson(request.products());
        jdbc.update("""
                insert into seller_application (
                    id, user_id, seller_type, legal_document_type, legal_document_number,
                    document_file_name, document_file_content, document_file_mime_type, company_name, company_description, contact_phone,
                    category, products_json, status, created_at
                ) values (
                    :id, :userId, :sellerType, :legalDocumentType, :legalDocumentNumber,
                    :documentFileName, :documentFileContent, :documentFileMimeType, :companyName, :companyDescription, :contactPhone,
                    :category, :productsJson, 'PENDING_REVIEW', :createdAt
                )
                """,
                new MapSqlParameterSource()
                        .addValue("id", id)
                        .addValue("userId", user.id())
                        .addValue("sellerType", sellerType)
                        .addValue("legalDocumentType", request.legalDocumentType())
                        .addValue("legalDocumentNumber", request.legalDocumentNumber())
                        .addValue("documentFileName", clean(request.documentFileName()))
                        .addValue("documentFileContent", clean(request.documentFileContent()))
                        .addValue("documentFileMimeType", clean(request.documentFileMimeType()))
                        .addValue("companyName", clean(request.companyName()))
                        .addValue("companyDescription", clean(request.companyDescription()))
                        .addValue("contactPhone", clean(request.contactPhone()))
                        .addValue("category", request.category().trim().toLowerCase())
                        .addValue("productsJson", productsJson)
                        .addValue("createdAt", Timestamp.from(createdAt)));
        return new SellerApplicationResponse(id, user.id(), sellerType, request.category().trim().toLowerCase(),
                "PENDING_REVIEW", createdAt);
    }

    public List<AdminSellerApplicationResponse> listForReview() {
        return jdbc.query("""
                select sa.id, sa.user_id, u.display_name, u.email, sa.seller_type, sa.legal_document_type,
                    sa.legal_document_number, sa.document_file_name, sa.document_file_mime_type, sa.company_name,
                    sa.company_description, sa.contact_phone, sa.category, sa.products_json, sa.status,
                    sa.reviewed_by, sa.reviewed_at, sa.review_note, sa.approved_product_count, sa.created_at
                from seller_application sa
                join app_user u on u.id = sa.user_id
                order by
                    case when sa.status = 'PENDING_REVIEW' then 0 else 1 end,
                    sa.created_at desc
                """, new MapSqlParameterSource(), this::mapAdminResponse);
    }

    @Transactional
    public AdminSellerApplicationResponse approve(UUID applicationId, CurrentUser reviewer,
            SellerApplicationDecisionRequest request) {
        AdminSellerApplicationResponse application = findForReview(applicationId);
        if (!"PENDING_REVIEW".equals(application.status())) {
            throw new IllegalArgumentException("Esta solicitud ya fue revisada");
        }

        AppUser user = userRepository.findById(application.userId())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        Set<Role> roles = user.roleSet();
        roles.add(Role.SELLER);
        user.setRoles(roles.stream().map(Enum::name).sorted().collect(Collectors.joining(",")));
        userRepository.save(user);

        Instant reviewedAt = Instant.now();
        insertApprovedProducts(application);
        jdbc.update("""
                update seller_application
                set status = 'APPROVED',
                    reviewed_by = :reviewedBy,
                    reviewed_at = :reviewedAt,
                    review_note = :reviewNote,
                    approved_product_count = :approvedProductCount
                where id = :id
                """,
                new MapSqlParameterSource()
                        .addValue("id", applicationId)
                        .addValue("reviewedBy", reviewer.id())
                        .addValue("reviewedAt", Timestamp.from(reviewedAt))
                        .addValue("reviewNote", clean(request == null ? null : request.note()))
                        .addValue("approvedProductCount", application.products().size()));
        return findForReview(applicationId);
    }

    @Transactional
    public AdminSellerApplicationResponse reject(UUID applicationId, CurrentUser reviewer,
            SellerApplicationDecisionRequest request) {
        AdminSellerApplicationResponse application = findForReview(applicationId);
        if (!"PENDING_REVIEW".equals(application.status())) {
            throw new IllegalArgumentException("Esta solicitud ya fue revisada");
        }
        Instant reviewedAt = Instant.now();
        jdbc.update("""
                update seller_application
                set status = 'REJECTED',
                    reviewed_by = :reviewedBy,
                    reviewed_at = :reviewedAt,
                    review_note = :reviewNote
                where id = :id
                """,
                new MapSqlParameterSource()
                        .addValue("id", applicationId)
                        .addValue("reviewedBy", reviewer.id())
                        .addValue("reviewedAt", Timestamp.from(reviewedAt))
                        .addValue("reviewNote", clean(request == null ? null : request.note())));
        return findForReview(applicationId);
    }

    private AdminSellerApplicationResponse findForReview(UUID applicationId) {
        return jdbc.queryForObject("""
                select sa.id, sa.user_id, u.display_name, u.email, sa.seller_type, sa.legal_document_type,
                    sa.legal_document_number, sa.document_file_name, sa.document_file_mime_type, sa.company_name,
                    sa.company_description, sa.contact_phone, sa.category, sa.products_json, sa.status,
                    sa.reviewed_by, sa.reviewed_at, sa.review_note, sa.approved_product_count, sa.created_at
                from seller_application sa
                join app_user u on u.id = sa.user_id
                where sa.id = :id
                """, new MapSqlParameterSource("id", applicationId), this::mapAdminResponse);
    }

    private void insertApprovedProducts(AdminSellerApplicationResponse application) {
        for (SellerApplicationRequest.ProductDraftRequest product : application.products()) {
            List<String> imageUrls = productImages(product);
            jdbc.update("""
                    insert into product (
                        id, seller_id, title, description, category, image_url, image_urls, price,
                        stock_available, status, created_at
                    ) values (
                        :id, :sellerId, :title, :description, :category, :imageUrl, :imageUrls, :price,
                        :stockAvailable, :status, :createdAt
                    )
                    """,
                    new MapSqlParameterSource()
                            .addValue("id", UUID.randomUUID())
                            .addValue("sellerId", application.userId())
                            .addValue("title", clean(product.title()))
                            .addValue("description", clean(product.description()))
                            .addValue("category", application.category())
                            .addValue("imageUrl", imageUrls.get(0))
                            .addValue("imageUrls", toJson(imageUrls))
                            .addValue("price", product.price())
                            .addValue("stockAvailable", product.stockAvailable() == null ? 0 : product.stockAvailable())
                            .addValue("status", ProductStatus.ACTIVE.name())
                            .addValue("createdAt", Timestamp.from(Instant.now())));
        }
    }

    private AdminSellerApplicationResponse mapAdminResponse(ResultSet rs, int rowNum) throws SQLException {
        return new AdminSellerApplicationResponse(
                rs.getObject("id", UUID.class),
                rs.getObject("user_id", UUID.class),
                rs.getString("display_name"),
                rs.getString("email"),
                rs.getString("seller_type"),
                rs.getString("legal_document_type"),
                rs.getString("legal_document_number"),
                rs.getString("document_file_name"),
                rs.getString("document_file_mime_type"),
                rs.getString("company_name"),
                rs.getString("company_description"),
                rs.getString("contact_phone"),
                rs.getString("category"),
                fromJson(rs.getString("products_json")),
                rs.getString("status"),
                rs.getObject("reviewed_by", UUID.class),
                toInstant(rs.getTimestamp("reviewed_at")),
                rs.getString("review_note"),
                rs.getInt("approved_product_count"),
                toInstant(rs.getTimestamp("created_at")));
    }

    private String toJson(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("No se pudieron procesar los productos");
        }
    }

    private List<SellerApplicationRequest.ProductDraftRequest> fromJson(String value) {
        try {
            return mapper.readValue(value, new TypeReference<>() {});
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("No se pudieron procesar los productos");
        }
    }

    private Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    private List<String> productImages(SellerApplicationRequest.ProductDraftRequest product) {
        List<String> images = product.imageUrls() == null ? List.of() : product.imageUrls();
        List<String> cleanImages = images.stream()
                .map(this::clean)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        if (!cleanImages.isEmpty()) {
            return cleanImages;
        }
        String fallback = clean(product.imageUrl());
        return StringUtils.hasText(fallback) ? List.of(fallback) : List.of();
    }

    private String clean(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
