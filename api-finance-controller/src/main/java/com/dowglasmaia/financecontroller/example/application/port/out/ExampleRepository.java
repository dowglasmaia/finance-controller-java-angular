package com.dowglasmaia.financecontroller.example.application.port.out;

import java.util.Optional;
import com.dowglasmaia.financecontroller.example.domain.model.Example;

public interface ExampleRepository {
    Optional<Example> findById(long id);
}
