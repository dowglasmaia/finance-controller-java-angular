package com.dowglasmaia.financecontroller.category.application.usecase;

import com.dowglasmaia.financecontroller.category.CategoryType;

/** Dados editáveis de uma categoria (criação e atualização). */
public record CategoryCommand(String name, CategoryType type, String icon, String color) {
}
