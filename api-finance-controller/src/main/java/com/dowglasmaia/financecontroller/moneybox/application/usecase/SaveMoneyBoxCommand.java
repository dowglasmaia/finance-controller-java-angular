package com.dowglasmaia.financecontroller.moneybox.application.usecase;

import com.dowglasmaia.financecontroller.shared.money.Currency;

public record SaveMoneyBoxCommand(String name, String description, String icon, String color, String goal,
                                  Currency currency) {
}
