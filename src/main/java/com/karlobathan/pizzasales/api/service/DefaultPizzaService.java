package com.karlobathan.pizzasales.api.service;

import com.karlobathan.pizzasales.api.ApiResources;
import com.karlobathan.pizzasales.api.dto.PizzaResponse;
import com.karlobathan.pizzasales.api.exception.ResourceNotFoundException;
import com.karlobathan.pizzasales.api.mapper.PizzaMapper;
import com.karlobathan.pizzasales.api.repository.PizzaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * {@link PizzaService} backed by the database. The pizza type is fetched in the same query as the pizza,
 * so mapping it needs no further queries.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DefaultPizzaService implements PizzaService {

    private final PizzaRepository pizzaRepository;
    private final PizzaMapper     pizzaMapper;

    @Override
    public List<PizzaResponse> findAll() {
        return pizzaMapper.toPizzaResponses(pizzaRepository.findAllByOrderByIdAsc());
    }

    @Override
    public PizzaResponse findById(Long id) {
        return pizzaRepository.findWithPizzaTypeById(id)
                .map(pizzaMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(ApiResources.PIZZA, id));
    }
}
