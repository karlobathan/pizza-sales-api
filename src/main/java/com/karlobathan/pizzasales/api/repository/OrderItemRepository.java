package com.karlobathan.pizzasales.api.repository;

import com.karlobathan.pizzasales.api.domain.OrderItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    // native, so replaced (soft-deleted) items count as existing and a re-import doesn't add them back
    @Query(value = "select source_order_details_id from order_item where source_order_details_id in (:sourceOrderDetailsIds)",
            nativeQuery = true
    )
    Set<Long> findSourceOrderDetailsIdIn(Collection<Long> sourceOrderDetailsIds);

    // fetches each item's pizza and its pizza type in the same query, as both are lazy and the API maps them
    @EntityGraph(attributePaths = {"pizza", "pizza.pizzaType"})
    List<OrderItem> findByOrderIdOrderByIdAsc(Long orderId);

    // one statement for all of an order's current items, instead of loading and updating each
    @Modifying
    @Query("update OrderItem oi set oi.deletedAt = :deletedAt where oi.order.id = :orderId and oi.deletedAt is null")
    int softDeleteByOrderId(Long orderId, Instant deletedAt);
}
