package com.dowglasmaia.financecontroller.example.api;

import com.dowglasmaia.financecontroller.example.domain.model.Example;
import com.dowglasmaia.financecontroller.generated.model.ExampleDto;

final class ContractExampleMapper {
    private ContractExampleMapper() {}

    static ExampleDto toDto(Example example) {
        return new ExampleDto().id(example.id()).name(example.name()).status(example.status());
    }
}
