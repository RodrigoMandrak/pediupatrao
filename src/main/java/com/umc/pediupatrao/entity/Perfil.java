package com.umc.pediupatrao.entity;

/**
 * Perfis de acesso do sistema.
 * O texto salvo no banco (campo "role" do usuário) é exatamente o nome do enum.
 */
public enum Perfil {
    ADMIN,
    GERENTE,
    ATENDENTE;

    // Converte o texto do banco para o enum. Usuários antigos (role "USER" ou vazio) viram ATENDENTE,
    // que é o perfil com menos poder.
    public static Perfil deTexto(String texto) {
        if (texto == null) {
            return ATENDENTE;
        }
        try {
            return Perfil.valueOf(texto.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ATENDENTE;
        }
    }

    // Usado para validar o que chega do formulário/API (aqui não aceitamos valor inválido)
    public static boolean valido(String texto) {
        if (texto == null) {
            return false;
        }
        for (Perfil p : values()) {
            if (p.name().equals(texto.trim().toUpperCase())) {
                return true;
            }
        }
        return false;
    }
}
