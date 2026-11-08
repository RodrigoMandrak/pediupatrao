package com.umc.pediupatrao.entity;

/** Tipos de evento que vão para a auditoria. */
public enum TipoOperacao {
    CRIACAO_PEDIDO,
    MUDANCA_STATUS,
    DESCONTO_PEDIDO,
    CANCELAMENTO_PEDIDO,
    CRIACAO_CLIENTE,
    ALTERACAO_CLIENTE,
    EXCLUSAO_CLIENTE
}
