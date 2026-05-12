package com.arquixpress.marketplace.catalog;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    @Query("""
            select p from Product p
            where p.status = com.arquixpress.marketplace.catalog.ProductStatus.ACTIVE
            order by p.createdAt desc
            """)
    Page<Product> searchAll(Pageable pageable);

    @Query("""
            select p from Product p
            where p.status = com.arquixpress.marketplace.catalog.ProductStatus.ACTIVE
              and (lower(p.title) like :pattern or lower(p.description) like :pattern)
            order by p.createdAt desc
            """)
    Page<Product> searchByQuery(@Param("pattern") String pattern, Pageable pageable);

    @Query("""
            select p from Product p
            where p.status = com.arquixpress.marketplace.catalog.ProductStatus.ACTIVE
              and lower(p.category) = :category
            order by p.createdAt desc
            """)
    Page<Product> searchByCategory(@Param("category") String category, Pageable pageable);

    @Query("""
            select p from Product p
            where p.status = com.arquixpress.marketplace.catalog.ProductStatus.ACTIVE
              and (lower(p.title) like :pattern or lower(p.description) like :pattern)
              and lower(p.category) = :category
            order by p.createdAt desc
            """)
    Page<Product> searchByQueryAndCategory(
            @Param("pattern") String pattern,
            @Param("category") String category,
            Pageable pageable
    );

    Optional<Product> findByIdAndStatus(UUID id, ProductStatus status);

    List<Product> findBySellerIdOrderByCreatedAtDesc(UUID sellerId);

    Optional<Product> findByIdAndSellerId(UUID id, UUID sellerId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Product p
            set p.stockAvailable = p.stockAvailable - :quantity,
                p.version = p.version + 1
            where p.id = :productId
              and p.status = com.arquixpress.marketplace.catalog.ProductStatus.ACTIVE
              and p.stockAvailable >= :quantity
            """)
    int reserveStock(@Param("productId") UUID productId, @Param("quantity") int quantity);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Product p
            set p.stockAvailable = p.stockAvailable + :quantity,
                p.version = p.version + 1
            where p.id = :productId
            """)
    int releaseStock(@Param("productId") UUID productId, @Param("quantity") int quantity);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Product p
            set p.stockAvailable = :stock,
                p.version = p.version + 1
            where p.id = :productId
            """)
    int setStock(@Param("productId") UUID productId, @Param("stock") int stock);
}