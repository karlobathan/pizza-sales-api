package com.karlobathan.pizzasales.importer.row;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvDate;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@ToString
public class OrderCsvRow {

    @CsvBindByName(column = "order_id", required = true)
    private Long orderId;

    @CsvBindByName(column = "date", required = true)
    @CsvDate("yyyy-MM-dd")
    private LocalDate date;

    @CsvBindByName(column = "time", required = true)
    @CsvDate("HH:mm:ss")
    private LocalTime time;
}
