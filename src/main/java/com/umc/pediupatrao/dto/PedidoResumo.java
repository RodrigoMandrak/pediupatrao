package com.umc.pediupatrao.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Linha da lista de pedidos recentes do dashboard. */
public record PedidoResumo(String id, String cliente, String status, BigDecimal valorTotal, LocalDateTime dataEntrada) {
}
