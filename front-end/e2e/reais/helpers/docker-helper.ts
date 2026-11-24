import { exec } from 'child_process';
import { promisify } from 'util';

const execAsync = promisify(exec);

/**
 * Consulta os logs recentes do container Docker do back-end para encontrar o código
 * de verificação de 6 dígitos impresso pelo ConsoleDevAuthEmailSender.
 *
 * @param emailDestino E-mail do usuário cujo código deve ser recuperado
 * @param timeoutMs Tempo máximo de espera pela impressão do código (padrão: 15s)
 * @param finalidade 'CADASTRO' ou 'RECUPERACAO_SENHA'
 * @returns O código de 6 dígitos numéricos
 */
export async function obterCodigoVerificacaoDocker(
  emailDestino: string,
  timeoutMs = 15000,
  finalidade: 'CADASTRO' | 'RECUPERACAO_SENHA' = 'CADASTRO',
): Promise<string> {
  const inicio = Date.now();
  const emailLimpo = emailDestino.trim().toLowerCase();
  const emailRegex = emailLimpo.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');

  // Formato gerado pelo ConsoleDevAuthEmailSender.java:
  // [MODO DEV — RESEND_API_KEY vazia] Código (CADASTRO) para email: 123456 (válido por 15 min)
  const padrao = new RegExp(`Código \\(${finalidade}\\) para ${emailRegex}: (\\d{6})`, 'i');

  while (Date.now() - inicio < timeoutMs) {
    const logs = await capturarLogsDoBackend();
    const linhas = logs.split('\n').reverse();

    for (const linha of linhas) {
      const match = linha.match(padrao);
      if (match && match[1]) {
        return match[1];
      }
    }

    // Aguarda 500ms antes de consultar novamente os logs
    await new Promise((resolve) => setTimeout(resolve, 500));
  }

  throw new Error(
    `[Docker Helper] Código de ${finalidade} para "${emailDestino}" não foi encontrado nos logs do container fatec-backend em ${timeoutMs}ms. Verifique se o container fatec-backend está em execução.`,
  );
}

/**
 * Tenta capturar os logs do container do back-end através do Docker CLI.
 *
 * A janela de leitura (`TAIL_LOGS`) precisa ser grande: com a suíte inteira
 * rodando em paralelo o back-end despeja muitas linhas por segundo e, com uma
 * janela curta, a linha do código sai do `--tail` antes da próxima consulta —
 * o teste então conclui que o código "não foi encontrado", mesmo ele tendo
 * sido impresso.
 */
const TAIL_LOGS = 3000;

async function capturarLogsDoBackend(): Promise<string> {
  // Lista de nomes mais prováveis para o container do Spring Boot
  const nomesContainers = [
    'fatec-backend',
    'fatecrepository-backend-1',
    'fatec-repositorios-backend-1',
    'fatec_copia_teste-backend-1',
  ];

  for (const nome of nomesContainers) {
    try {
      const { stdout } = await execAsync(`docker logs --tail ${TAIL_LOGS} ${nome}`);
      if (stdout) return stdout;
    } catch {
      // Container não encontrado por este nome específico, tenta o próximo
    }
  }

  // Fallback inteligente: busca o ID do container ativo que contém 'backend' no nome
  try {
    const { stdout: containerId } = await execAsync('docker ps -q -f "name=backend"');
    const primeiroId = containerId.trim().split('\n')[0];
    if (primeiroId) {
      const { stdout } = await execAsync(`docker logs --tail ${TAIL_LOGS} ${primeiroId}`);
      return stdout;
    }
  } catch {
    // Docker CLI indisponível ou permissão insuficiente
  }

  return '';
}
