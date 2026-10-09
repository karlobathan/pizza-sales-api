package com.karlobathan.pizzasales.api.repository;

import com.karlobathan.pizzasales.api.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

    // native, so deleted orders count as existing and a re-import doesn't recreate them
    @Query(value = "select source_order_id from orders where source_order_id in (:sourceOrderIds)", nativeQuery = true)
    Set<Long> findSourceOrderIdIn(Collection<Long> sourceOrderIds);

    // native, so order items imported for a deleted order attach to it (and stay hidden with it) instead of failing
    @Query(value = "select * from orders where source_order_id in (:sourceOrderIds)", nativeQuery = true)
    List<Order> findBySourceOrderIdIn(Collection<Long> sourceOrderIds);
}
