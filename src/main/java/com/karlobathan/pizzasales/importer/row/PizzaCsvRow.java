package com.karlobathan.pizzasales.importer.row;

import com.karlobathan.pizzasales.api.domain.PizzaSize;
import com.opencsv.bean.CsvBindByName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

@Getter
@Setter
@ToString
public class PizzaCsvRow {

    @CsvBindByName(column = "pizza_id", required = true)
    private String pizzaId;

    @CsvBindByName(column = "pizza_type_id", required = true)
    private String pizzaTypeId;

    @CsvBindByName(column = "size", required = true)
    private PizzaSize size;

    @CsvBindByName(column = "price", required = true)
    private BigDecimal price;
}
