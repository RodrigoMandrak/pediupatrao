package com.umc.pediupatrao.entity;

/**
 * Fluxo do pedido:
 * RECEBIDO -> EM_PREPARACAO -> PRONTO -> (SAIU_PARA_ENTREGA ou RETIRADO) -> FINALIZADO
 * CANCELADO é um caso especial, tratado em um método próprio do serviço (só o gerente cancela).
 */
public enum StatusPedido {
    RECEBIDO,
    EM_PREPARACAO,
    PRONTO,
    SAIU_PARA_ENTREGA,
    RETIRADO,
    FINALIZADO,
    CANCELADO;

    // Diz se é permitido ir deste status para o próximo (sem contar cancelamento)
    public boolean podeIrPara(StatusPedido novo) {
        switch (this) {
            case RECEBIDO:
                return novo == EM_PREPARACAO;
            case EM_PREPARACAO:
                return novo == PRONTO;
            case PRONTO:
                return novo == SAIU_PARA_ENTREGA || novo == RETIRADO;
            case SAIU_PARA_ENTREGA:
            case RETIRADO:
                return novo == FINALIZADO;
            default:
                return false; // FINALIZADO e CANCELADO não mudam mais
        }
    }

    // Cancelamento e desconto só valem enquanto o pedido ainda não saiu
    public boolean antesDaSaida() {
        return this == RECEBIDO || this == EM_PREPARACAO || this == PRONTO;
    }

    // Busca o enum a partir de um texto, com mensagem de erro amigável
    public static StatusPedido deTexto(String texto) {
        if (texto == null) {
            throw new IllegalArgumentException("Informe o status.");
        }
        try {
            return StatusPedido.valueOf(texto.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Status inválido: " + texto);
        }
    }
}
