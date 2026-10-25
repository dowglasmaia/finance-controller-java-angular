package com.dowglasmaia.financecontroller.example.infrastructure.persistence;

import java.util.Optional;
import org.springframework.stereotype.Repository;
import com.dowglasmaia.financecontroller.example.application.port.out.ExampleRepository;
import com.dowglasmaia.financecontroller.example.domain.model.Example;

@Repository
public final class InMemoryExampleRepository implements ExampleRepository {
    @Override
    public Optional<Example> findById(long id) {
        return id == 1 ? Optional.of(new Example(1, "Example", "ACTIVE")) : Optional.empty();
    }
}
