package com.umc.pediupatrao.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Indicadores gerais do dashboard (RF01). "Vendas" = pedidos não cancelados. */
public record ResumoVendas(
        LocalDate inicio,
        LocalDate fim,
        BigDecimal faturamentoTotal,
        long totalPedidos,
        long pedidosValidos,
        long pedidosCancelados,
        BigDecimal ticketMedio,
        BigDecimal descontosConcedidos,
        long itensVendidos) {
}
