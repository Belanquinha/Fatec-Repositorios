package com.fatecrepository.dto.response;

/**
 * Atalho de login de desenvolvimento, oferecido ao front-end para não precisar digitar e-mail.
 *
 * @param email  e-mail institucional asumido
 * @param rotulo texto do botão
 */
public record DevAuthAccountResponse(String email, String rotulo) {
}