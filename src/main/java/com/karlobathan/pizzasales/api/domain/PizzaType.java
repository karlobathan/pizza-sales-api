package com.karlobathan.pizzasales.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "pizza_type")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class PizzaType {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pizza_type_seq")
    @SequenceGenerator(name = "pizza_type_seq", sequenceName = "pizza_type_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code; // natural/source id, kept for idempotent re-import (pizza_type_id in CSV)

    @Column(nullable = false, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pizza_category_id", nullable = false)
    private PizzaCategory pizzaCategory;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "pizza_type_ingredient", joinColumns = @JoinColumn(name = "pizza_type_id"), inverseJoinColumns = @JoinColumn(name = "pizza_ingredient_id"))
    private Set<PizzaIngredient> pizzaIngredients = new HashSet<>();

    // soft delete: menu queries skip deleted rows explicitly, but orders still load the pizza types they were placed with
    private Instant deletedAt;
}
