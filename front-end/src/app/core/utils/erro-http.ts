/**
 * Normaliza erro HTTP para mensagem exibível.
 *
 * O nginx do dev pode devolver HTML (ex.: 413 `client_max_body_size`) e o
 * `HttpClient` entrega esse corpo cru em `err.error` (string começando com
 * `<`) ou dentro de `err.error.mensagem/message`. Corpo/trecho cru começando
 * com `<` nunca vai para a tela — cai no `padrao` amigável.
 */
export function mensagemErroAmigavel(err: unknown, padrao: string): string {
  const corpo = (err as { error?: unknown })?.error;
  const bruta =
    typeof corpo === 'string'
      ? corpo
      : (
        (corpo as { mensagem?: unknown; message?: unknown } | null | undefined)?.mensagem ??
        (corpo as { message?: unknown } | null | undefined)?.message
      );
  if (typeof bruta === 'string') {
    if (bruta.trim().startsWith('<')) {
      return padrao;
    }
    if (bruta.trim()) {
      return bruta;
    }
  }
  return padrao;
}
