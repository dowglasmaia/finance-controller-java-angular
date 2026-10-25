package com.dowglasmaia.financecontroller.example.application.usecase;

import java.util.Optional;
import org.springframework.stereotype.Service;
import com.dowglasmaia.financecontroller.example.application.port.out.ExampleRepository;
import com.dowglasmaia.financecontroller.example.domain.model.Example;

/** One class per use case. Not final: Spring proxies it when it gains @Transactional. */
@Service
public class GetExampleUseCase {
    private final ExampleRepository repository;

    public GetExampleUseCase(ExampleRepository repository) {
        this.repository = repository;
    }

    public Optional<Example> execute(long id) {
        return repository.findById(id);
    }
}
