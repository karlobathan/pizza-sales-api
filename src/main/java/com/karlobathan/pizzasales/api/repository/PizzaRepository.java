package com.karlobathan.pizzasales.api.repository;

import com.karlobathan.pizzasales.api.domain.Pizza;
import com.karlobathan.pizzasales.api.domain.PizzaSize;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Pizzas are soft-deleted. Methods named {@code ...Active...} or ending in {@code DeletedAtIsNull} skip deleted ones;
 * the others include them, as the import, code checks and existing orders must still see deleted rows.
 */
public interface PizzaRepository extends JpaRepository<Pizza, Long> {

    @Query("select p.code from Pizza p")
    Set<String> findAllCode();

    // fetches the pizza type in the same query, as it is lazy and the API maps it
    @Query("select p from Pizza p join fetch p.pizzaType where p.deletedAt is null order by p.id")
    List<Pizza> findAllActiveWithPizzaType();

    @Query("select p from Pizza p join fetch p.pizzaType where p.id = :id and p.deletedAt is null")
    Optional<Pizza> findActiveWithPizzaTypeById(Long id);

    @Query("select p from Pizza p join fetch p.pizzaType where p.id in :ids and p.deletedAt is null")
    List<Pizza> findActiveWithPizzaTypeByIdIn(Collection<Long> ids);

    // deleted ones included: their codes stay reserved, as the import matches on codes
    boolean existsByCode(String code);

    boolean existsByPizzaTypeIdAndDeletedAtIsNull(Long pizzaTypeId);

    boolean existsByPizzaTypeIdAndSizeAndDeletedAtIsNull(Long pizzaTypeId, PizzaSize size);

    boolean existsByPizzaTypeIdAndSizeAndDeletedAtIsNullAndIdNot(Long pizzaTypeId, PizzaSize size, Long id);
}
