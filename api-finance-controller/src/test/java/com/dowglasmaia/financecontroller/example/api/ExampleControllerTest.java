package com.dowglasmaia.financecontroller.example.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import com.dowglasmaia.financecontroller.example.application.usecase.GetExampleUseCase;
import com.dowglasmaia.financecontroller.example.domain.model.Example;

class ExampleControllerTest {
    @Test
    void mapsSuccessAndMissingResource() {
        var controller = new ExampleController(new GetExampleUseCase(id -> id == 1
            ? Optional.of(new Example(1, "Example", "ACTIVE")) : Optional.empty()));
        var found = controller.getExampleById(1L);
        assertEquals(HttpStatus.OK, found.getStatusCode());
        assertEquals("ACTIVE", found.getBody().getStatus());
        var missing = controller.getExampleById(2L);
        assertEquals(HttpStatus.NOT_FOUND, missing.getStatusCode());
        assertNull(missing.getBody());
    }
}
