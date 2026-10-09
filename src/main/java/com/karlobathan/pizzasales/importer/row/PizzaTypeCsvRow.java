package com.karlobathan.pizzasales.importer.row;

import com.karlobathan.pizzasales.importer.loader.BinderConfig;
import com.opencsv.bean.CsvBindAndSplitByName;
import com.opencsv.bean.CsvBindByName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
public class PizzaTypeCsvRow {

    @CsvBindByName(column = "pizza_type_id", required = true)
    private String pizzaTypeId;

    @CsvBindByName(column = "name", required = true)
    private String name;

    @CsvBindByName(column = "category", required = true)
    private String category;

    @CsvBindAndSplitByName(column = "ingredients", required = true, elementType = String.class, splitOn = BinderConfig.SPLIT_ON_COMMA_DELIMITER)
    private List<String> ingredients;
}
