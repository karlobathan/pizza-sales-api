package com.karlobathan.pizzasales.api.service;

import com.karlobathan.pizzasales.api.ApiResources;
import com.karlobathan.pizzasales.api.dto.PizzaTypeResponse;
import com.karlobathan.pizzasales.api.exception.ResourceNotFoundException;
import com.karlobathan.pizzasales.api.mapper.PizzaMapper;
import com.karlobathan.pizzasales.api.repository.PizzaTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * {@link PizzaTypeService} backed by the database. Category and ingredients are fetched in the same query
 * as the pizza type, so mapping them needs no further queries.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DefaultPizzaTypeService implements PizzaTypeService {

    private final PizzaTypeRepository pizzaTypeRepository;
    private final PizzaMapper         pizzaMapper;

    @Override
    public List<PizzaTypeResponse> findAll() {
        return pizzaMapper.toPizzaTypeResponses(pizzaTypeRepository.findAllWithDetails());
    }

    @Override
    public PizzaTypeResponse findById(Long id) {
        return pizzaTypeRepository.findWithDetailsById(id)
                .map(pizzaMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(ApiResources.PIZZA_TYPE, id));
    }
}
