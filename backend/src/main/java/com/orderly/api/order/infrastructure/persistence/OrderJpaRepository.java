package com.orderly.api.order.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderJpaRepository extends JpaRepository<OrderJpaEntity, UUID> {

    List<OrderJpaEntity> findAllByBusinessIdOrderByCreatedAtDesc(UUID businessId);

    Optional<OrderJpaEntity> findByIdAndBusinessId(UUID id, UUID businessId);

    /**
     * Count active orders (pending confirmation or in-flight).
     * Used to enforce plan limits.
     */
    @Query("SELECT COUNT(o) FROM OrderJpaEntity o WHERE o.businessId = :bid " +
            "AND o.status IN ('PENDING','CONFIRMED','IN_PROGRESS','READY','OUT_FOR_DELIVERY')")
    long countActiveOrders(@Param("bid") UUID businessId);

    @Query("SELECT COUNT(o) FROM OrderJpaEntity o WHERE o.businessId = :bid " +
            "AND o.createdAt >= :since")
    long countOrdersSince(@Param("bid") UUID businessId, @Param("since") java.time.OffsetDateTime since);

    @Query("SELECT DISTINCT o.businessId FROM OrderJpaEntity o WHERE o.createdAt < :since")
    List<UUID> findBusinessesWithNoOrdersSince(@Param("since") java.time.Instant since);

    List<OrderJpaEntity> findAllByBusinessIdAndCustomerWhatsappAndStatus(UUID businessId, String customerWhatsapp, String status);

    @Query("SELECT COUNT(o) FROM OrderJpaEntity o WHERE o.createdAt >= :since")
    long countOrdersSinceGlobal(@Param("since") java.time.OffsetDateTime since);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM OrderJpaEntity o WHERE o.createdAt >= :since AND o.status != 'CANCELLED'")
    java.math.BigDecimal sumRevenueGlobalSince(@Param("since") java.time.OffsetDateTime since);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM OrderJpaEntity o WHERE o.status != 'CANCELLED'")
    java.math.BigDecimal sumRevenueGlobal();

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM OrderJpaEntity o WHERE o.businessId = :bid AND o.status != 'CANCELLED'")
    java.math.BigDecimal sumRevenueByBusiness(@Param("bid") UUID businessId);

    long countByBusinessId(UUID businessId);
}
