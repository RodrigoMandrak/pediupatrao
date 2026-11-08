package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Alteracao;
import com.umc.pediupatrao.entity.Auditoria;
import com.umc.pediupatrao.entity.TipoOperacao;
import com.umc.pediupatrao.repository.AuditoriaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class AuditoriaService {

    private final AuditoriaRepository auditoriaRepository;

    public AuditoriaService(AuditoriaRepository auditoriaRepository) {
        this.auditoriaRepository = auditoriaRepository;
    }

    // Grava um evento de auditoria com o usuário que está logado agora
    public void registrar(TipoOperacao operacao, String entidade, String entidadeId,
            List<Alteracao> alteracoes, String justificativa) {
        Auditoria registro = new Auditoria(
                null,
                UsuarioLogado.nome(),
                UsuarioLogado.perfil(),
                LocalDateTime.now(),
                operacao.name(),
                entidade,
                entidadeId,
                alteracoes,
                justificativa);
        auditoriaRepository.save(registro);
    }

    // Só adiciona na lista se o valor realmente mudou (null e vazio contam como igual)
    public void compararCampo(List<Alteracao> lista, String campo, Object antes, Object depois) {
        String a = textoOuNull(antes);
        String d = textoOuNull(depois);
        if (!Objects.equals(a, d)) {
            lista.add(new Alteracao(campo, a, d));
        }
    }

    public List<Auditoria> consultar(String entidade, String entidadeId) {
        if (entidade != null && !entidade.isBlank() && entidadeId != null && !entidadeId.isBlank()) {
            return auditoriaRepository.findByEntidadeAndEntidadeIdOrderByDataHoraDesc(entidade, entidadeId);
        }
        if (entidade != null && !entidade.isBlank()) {
            return auditoriaRepository.findByEntidadeOrderByDataHoraDesc(entidade);
        }
        return auditoriaRepository.findAllByOrderByDataHoraDesc();
    }

    private String textoOuNull(Object valor) {
        if (valor == null) {
            return null;
        }
        String texto = valor.toString().trim();
        return texto.isEmpty() ? null : texto;
    }
}
