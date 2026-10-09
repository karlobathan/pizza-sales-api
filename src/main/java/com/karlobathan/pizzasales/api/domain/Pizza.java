package com.karlobathan.pizzasales.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "pizza")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Pizza {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pizza_seq")
    @SequenceGenerator(name = "pizza_seq", sequenceName = "pizza_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code; // natural/source id, kept for idempotent re-import (pizza_id in CSV)

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pizza_type_id", nullable = false)
    private PizzaType pizzaType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PizzaSize size;

    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal price;
}
