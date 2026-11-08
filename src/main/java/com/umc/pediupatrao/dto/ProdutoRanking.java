package com.umc.pediupatrao.dto;

import java.math.BigDecimal;

/** Linha do ranking de produtos mais vendidos (RF04). */
public record ProdutoRanking(String produtoId, String nome, long quantidadeVendida, BigDecimal faturamento) {
}
