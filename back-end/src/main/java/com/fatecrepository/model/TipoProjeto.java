package com.fatecrepository.model;

/**
 * Modelo de projeto suportado na plataforma:
 * <ul>
 *   <li>{@code GITHUB}: projeto padrão com link externo de repositório.</li>
 *   <li>{@code JOGO}: projeto de jogo executável empacotado em {@code .zip}.</li>
 * </ul>
 */
public enum TipoProjeto {
    GITHUB,
    JOGO
}
