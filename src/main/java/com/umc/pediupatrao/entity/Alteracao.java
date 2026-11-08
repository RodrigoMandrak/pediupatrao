package com.umc.pediupatrao.entity;

/** Um campo que mudou: nome, valor antes e valor depois. */
public class Alteracao {

    private final String campo;
    private final String valorAnterior;
    private final String valorNovo;

    public Alteracao(String campo, String valorAnterior, String valorNovo) {
        this.campo = campo;
        this.valorAnterior = valorAnterior;
        this.valorNovo = valorNovo;
    }

    public String getCampo() { return campo; }
    public String getValorAnterior() { return valorAnterior; }
    public String getValorNovo() { return valorNovo; }
}
