package com.karlobathan.pizzasales.api.repository;

import com.karlobathan.pizzasales.api.domain.PizzaType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Pizza types are soft-deleted. Methods named {@code ...Active...} skip deleted ones; the others include them, as the
 * import and code checks must still see deleted rows.
 */
public interface PizzaTypeRepository extends JpaRepository<PizzaType, Long> {

    @Query("select pt.code from PizzaType pt")
    Set<String> findAllCode();

    // fetches category and ingredients in the same query, as both are lazy and the API maps them
    @Query("""
            select distinct pt from PizzaType pt
            join fetch pt.pizzaCategory
            left join fetch pt.pizzaIngredients
            where pt.deletedAt is null
            order by pt.id
            """)
    List<PizzaType> findAllActiveWithDetails();

    @Query("""
            select pt from PizzaType pt
            join fetch pt.pizzaCategory
            left join fetch pt.pizzaIngredients
            where pt.id = :id and pt.deletedAt is null
            """)
    Optional<PizzaType> findActiveWithDetailsById(Long id);

    // deleted ones included: their codes stay reserved, as the import matches on codes
    boolean existsByCode(String code);
}
