package com.fatecrepository.service;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Dados mínimos do projeto que a notificação ao professor precisa. É um recorte
 * de propósito: a notificação roda fora da transação (pós-commit, assíncrono),
 * então carrega valores já resolvidos — nunca a entidade JPA, cujas associações
 * preguiçosas (`instituicao`, `autor`) não estariam mais numa sessão aberta.
 */
public record ProjetoSubmetidoNotificacao(
    UUID projetoId,
    String titulo,
    String nomeInstituicao,
    String emailProfessorResponsavel,
    LocalDate dataSubmissao
) {
}
