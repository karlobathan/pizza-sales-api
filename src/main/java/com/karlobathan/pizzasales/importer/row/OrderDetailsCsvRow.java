package com.karlobathan.pizzasales.importer.row;

import com.opencsv.bean.CsvBindByName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class OrderDetailsCsvRow {

    @CsvBindByName(column = "order_details_id", required = true)
    private Long orderDetailsId;

    @CsvBindByName(column = "order_id", required = true)
    private Long orderId;

    @CsvBindByName(column = "pizza_id", required = true)
    private String pizzaId;

    @CsvBindByName(column = "quantity", required = true)
    private Integer quantity;
}
