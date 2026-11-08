package com.umc.pediupatrao.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/** Pequena classe de apoio para saber quem está logado (nome e perfil). */
public final class UsuarioLogado {

    private UsuarioLogado() {
    }

    public static String nome() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return "sistema";
        }
        return auth.getName();
    }

    public static String perfil() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            for (GrantedAuthority autoridade : auth.getAuthorities()) {
                if (autoridade.getAuthority().startsWith("ROLE_")) {
                    return autoridade.getAuthority().substring(5);
                }
            }
        }
        return "NENHUM";
    }
}
