package com.umc.pediupatrao.entity;

import java.math.BigDecimal;

/** Um item dentro do pedido. O preço é copiado do produto na hora da criação. */
public class ItemPedido {

    private String produtoId;
    private String nome;
    private BigDecimal precoUnitario;
    private int quantidade;

    public ItemPedido() {
    }

    public ItemPedido(String produtoId, String nome, BigDecimal precoUnitario, int quantidade) {
        this.produtoId = produtoId;
        this.nome = nome;
        this.precoUnitario = precoUnitario;
        this.quantidade = quantidade;
    }

    public BigDecimal getSubtotal() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }

    public String getProdutoId() { return produtoId; }
    public void setProdutoId(String produtoId) { this.produtoId = produtoId; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public BigDecimal getPrecoUnitario() { return precoUnitario; }
    public void setPrecoUnitario(BigDecimal precoUnitario) { this.precoUnitario = precoUnitario; }

    public int getQuantidade() { return quantidade; }
    public void setQuantidade(int quantidade) { this.quantidade = quantidade; }
}
