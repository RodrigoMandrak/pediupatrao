package com.umc.pediupatrao.repository;

import com.umc.pediupatrao.entity.Auditoria;
import java.util.List;
import org.springframework.data.repository.Repository;

/**
 * Repositório "só de inclusão e consulta": de propósito NÃO estende MongoRepository,
 * então não existe método para alterar ou apagar registros de auditoria.
 */
public interface AuditoriaRepository extends Repository<Auditoria, String> {

    <S extends Auditoria> S save(S auditoria);

    List<Auditoria> findAllByOrderByDataHoraDesc();

    List<Auditoria> findByEntidadeOrderByDataHoraDesc(String entidade);

    List<Auditoria> findByEntidadeAndEntidadeIdOrderByDataHoraDesc(String entidade, String entidadeId);
}
