package com.arquixpress.marketplace.catalog.application;

import com.arquixpress.marketplace.catalog.ProductRepository;
import com.arquixpress.marketplace.catalog.ProductStatus;
import com.arquixpress.marketplace.catalog.ProductSummary;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
public class CatalogService {
    private final ProductRepository products;
    private final NamedParameterJdbcTemplate catalogReadReplica;
    private final boolean readReplicaEnabled;

    public CatalogService(ProductRepository products,
                          ObjectProvider<NamedParameterJdbcTemplate> catalogReadReplica,
                          @Value("${app.read-replica.enabled:false}") boolean readReplicaEnabled) {
        this.products = products;
        this.catalogReadReplica = catalogReadReplica.getIfAvailable();
        this.readReplicaEnabled = readReplicaEnabled;
    }

    @Cacheable(cacheNames = "product-search", key = "{#query, #category, #page, #size}")
    public Page<ProductSummary> search(String query, String category, int page, int size) {
        String normalizedQuery = query == null || query.isBlank() ? null : query.trim();
        String normalizedCategory = category == null || category.isBlank() ? null : category.trim().toLowerCase();
        PageRequest pageable = PageRequest.of(page, Math.min(size, 100));
        if (readReplicaEnabled && catalogReadReplica != null) {
            try {
                return searchReplica(normalizedQuery, normalizedCategory, pageable);
            } catch (DataAccessException ex) {
                return searchPrimary(normalizedQuery, normalizedCategory, pageable);
            }
        }
        return searchPrimary(normalizedQuery, normalizedCategory, pageable);
    }

    public ProductSummary detail(UUID id) {
        if (readReplicaEnabled && catalogReadReplica != null) {
            try {
                return detailReplica(id);
            } catch (DataAccessException ex) {
                return detailPrimary(id);
            }
        }
        return detailPrimary(id);
    }

    private Page<ProductSummary> searchPrimary(String normalizedQuery, String normalizedCategory, Pageable pageable) {
        if (normalizedQuery != null && normalizedCategory != null) {
            return products.searchByQueryAndCategory(pattern(normalizedQuery), normalizedCategory, pageable).map(ProductSummary::from);
        }
        if (normalizedQuery != null) {
            return products.searchByQuery(pattern(normalizedQuery), pageable).map(ProductSummary::from);
        }
        if (normalizedCategory != null) {
            return products.searchByCategory(normalizedCategory, pageable).map(ProductSummary::from);
        }
        return products.searchAll(pageable).map(ProductSummary::from);
    }

    private ProductSummary detailPrimary(UUID id) {
        return products.findByIdAndStatus(id, ProductStatus.ACTIVE)
                .map(ProductSummary::from)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));
    }

    private Page<ProductSummary> searchReplica(String normalizedQuery, String normalizedCategory, Pageable pageable) {
        StringBuilder sql = new StringBuilder("""
                select id, seller_id, title, description, category, image_url, image_urls, price, stock_available
                  from product
                 where status = 'ACTIVE'
                """);
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("limit", pageable.getPageSize())
                .addValue("offset", pageable.getOffset());
        if (normalizedQuery != null) {
            sql.append(" and (lower(title) like :pattern or lower(description) like :pattern)");
            params.addValue("pattern", pattern(normalizedQuery));
        }
        if (normalizedCategory != null) {
            sql.append(" and lower(category) = :category");
            params.addValue("category", normalizedCategory);
        }
        sql.append(" order by created_at desc limit :limit offset :offset");
        var rows = catalogReadReplica.query(sql.toString(), params, (rs, rowNum) -> new ProductSummary(
                UUID.fromString(rs.getString("id")),
                UUID.fromString(rs.getString("seller_id")),
                rs.getString("title"),
                rs.getString("description"),
                rs.getString("category"),
                rs.getString("image_url"),
                ProductSummary.parseImages(rs.getString("image_urls"), rs.getString("image_url")),
                rs.getBigDecimal("price"),
                rs.getInt("stock_available")));
        return new PageImpl<>(rows, pageable, rows.size());
    }

    private ProductSummary detailReplica(UUID id) {
        var rows = catalogReadReplica.query("""
                select id, seller_id, title, description, category, image_url, image_urls, price, stock_available
                  from product
                 where id = :id
                   and status = 'ACTIVE'
                """, new MapSqlParameterSource("id", id), (rs, rowNum) -> new ProductSummary(
                UUID.fromString(rs.getString("id")),
                UUID.fromString(rs.getString("seller_id")),
                rs.getString("title"),
                rs.getString("description"),
                rs.getString("category"),
                rs.getString("image_url"),
                ProductSummary.parseImages(rs.getString("image_urls"), rs.getString("image_url")),
                rs.getBigDecimal("price"),
                rs.getInt("stock_available")));
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("Producto no encontrado");
        }
        return rows.get(0);
    }

    private String pattern(String value) {
        return "%" + value.toLowerCase() + "%";
    }
}
