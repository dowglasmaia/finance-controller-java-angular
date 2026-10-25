package com.dowglasmaia.financecontroller.example.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import com.dowglasmaia.financecontroller.example.application.port.out.ExampleRepository;
import com.dowglasmaia.financecontroller.example.application.usecase.GetExampleUseCase;
import com.dowglasmaia.financecontroller.example.domain.model.Example;

class GetExampleUseCaseTest {
    @Test
    void returnsExampleThroughPort() {
        ExampleRepository repository = Mockito.mock(ExampleRepository.class);
        when(repository.findById(1)).thenReturn(Optional.of(new Example(1, "Example", "ACTIVE")));
        var result = new GetExampleUseCase(repository).execute(1);
        assertTrue(result.isPresent());
        assertEquals("Example", result.orElseThrow().name());
    }
}
