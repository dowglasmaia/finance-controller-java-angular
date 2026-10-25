package com.dowglasmaia.financecontroller.example.api;

import com.dowglasmaia.financecontroller.example.application.usecase.GetExampleUseCase;
import com.dowglasmaia.financecontroller.generated.api.ExamplesApi;
import com.dowglasmaia.financecontroller.generated.model.ExampleDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ExampleController implements ExamplesApi {
    private final GetExampleUseCase getExample;

    public ExampleController(GetExampleUseCase getExample) {
        this.getExample = getExample;
    }

    @Override
    public ResponseEntity<ExampleDto> getExampleById(Long id) {
        return getExample.execute(id)
            .map(example -> ResponseEntity.ok(ContractExampleMapper.toDto(example)))
            .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
