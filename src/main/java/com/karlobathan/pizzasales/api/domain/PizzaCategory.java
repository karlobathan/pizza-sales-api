package com.karlobathan.pizzasales.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "pizza_category")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class PizzaCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pizza_category_seq")
    @SequenceGenerator(name = "pizza_category_seq", sequenceName = "pizza_category_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

}
