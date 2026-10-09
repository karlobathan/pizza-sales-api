package com.karlobathan.pizzasales.api.service;

import com.karlobathan.pizzasales.api.dto.OrderRequest;
import com.karlobathan.pizzasales.api.dto.OrderResponse;
import com.karlobathan.pizzasales.api.dto.OrderSummaryResponse;
import com.karlobathan.pizzasales.api.dto.PageResponse;
import com.karlobathan.pizzasales.api.exception.InvalidRequestException;
import com.karlobathan.pizzasales.api.exception.ResourceNotFoundException;

import java.time.LocalDate;

/**
 * Access to orders: when each was placed, and the pizzas on it.
 */
public interface OrderService {

    /**
     * Returns one page of orders, optionally limited to a date range.
     *
     * @param from first order date to include, or {@code null} for no lower bound
     * @param to   last order date to include, or {@code null} for no upper bound
     * @param page zero-based page number; a page past the end is empty
     * @param size number of orders per page
     * @return the page of orders, sorted by order date, then time, then id
     * @throws InvalidRequestException if both dates are given and {@code from} is after {@code to}
     */
    PageResponse<OrderSummaryResponse> findAll(LocalDate from, LocalDate to, int page, int size);

    /**
     * Returns one order with its items and totals.
     *
     * @param id the order's database id
     * @return the order with its items (ordered by id), total quantity and total price
     * @throws ResourceNotFoundException if no order has this id
     */
    OrderResponse findById(Long id);

    /**
     * Creates an order with its items. Nothing is saved if any item is invalid.
     *
     * @param request the order's date, time and items
     * @return the created order with its new id, items (in request order) and totals
     * @throws InvalidRequestException if an item references a pizza that doesn't exist
     */
    OrderResponse create(OrderRequest request);
}
