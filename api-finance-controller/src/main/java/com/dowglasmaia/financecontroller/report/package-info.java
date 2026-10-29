/**
 * Consultas de leitura agregadas: a visão geral mensal do dashboard (e, na fatia dos relatórios, os relatórios por
 * período). Consome só APIs públicas de {@code transaction}, {@code category} e {@code budget}; não lê tabelas alheias.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Report")
package com.dowglasmaia.financecontroller.report;
