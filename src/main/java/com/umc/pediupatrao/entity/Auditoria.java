package com.umc.pediupatrao.entity;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Registro de auditoria. A classe só tem getters (não tem setters) de propósito:
 * depois de criado, o registro não é alterado.
 */
@Document(collection = "auditoria")
public class Auditoria {

    @Id
    private String id; // preenchido pelo MongoDB ao salvar (sem setter público)
    private final String usuario;
    private final String perfil;
    private final LocalDateTime dataHora;
    private final String operacao;
    private final String entidade;      // "PEDIDO" ou "CLIENTE"
    private final String entidadeId;
    private final List<Alteracao> alteracoes;
    private final String justificativa;

    public Auditoria(String id, String usuario, String perfil, LocalDateTime dataHora, String operacao,
            String entidade, String entidadeId, List<Alteracao> alteracoes, String justificativa) {
        this.id = id;
        this.usuario = usuario;
        this.perfil = perfil;
        this.dataHora = dataHora;
        this.operacao = operacao;
        this.entidade = entidade;
        this.entidadeId = entidadeId;
        this.alteracoes = alteracoes;
        this.justificativa = justificativa;
    }

    public String getId() { return id; }
    public String getUsuario() { return usuario; }
    public String getPerfil() { return perfil; }
    public LocalDateTime getDataHora() { return dataHora; }
    public String getOperacao() { return operacao; }
    public String getEntidade() { return entidade; }
    public String getEntidadeId() { return entidadeId; }
    public List<Alteracao> getAlteracoes() { return alteracoes; }
    public String getJustificativa() { return justificativa; }
}
