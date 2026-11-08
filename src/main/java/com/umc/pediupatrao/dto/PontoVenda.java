package com.umc.pediupatrao.dto;

import java.math.BigDecimal;

/** Um ponto do gráfico de vendas: período ("2026-11-07" ou "2026-11"), nº de pedidos e faturamento. */
public record PontoVenda(String periodo, long pedidos, BigDecimal faturamento) {
}
