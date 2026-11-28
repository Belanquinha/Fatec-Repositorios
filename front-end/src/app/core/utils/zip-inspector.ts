export interface ExecutavelZipInfo {
  caminho: string;
  auxiliar: boolean;
}

const AUXILIARES_CONHECIDOS = [
  'unitycrashhandler',
  'vc_redist',
  'ueprereqsetup',
  'dxsetup',
  'unins000',
];

/**
 * Lê apenas o End of Central Directory (EOCD) e o Central Directory de um `.zip`
 * usando `File.slice` — sem carregar 500MB em memória RAM no navegador.
 */
export async function listarExecutaveisNoZip(file: File): Promise<ExecutavelZipInfo[]> {
  if (file.size < 22) {
    throw new Error('Arquivo .zip inválido ou vazio.');
  }

  // Lê até os últimos 66KB para encontrar a assinatura EOCD (0x06054b50)
  const tailSize = Math.min(file.size, 65536 + 22);
  const tailBuffer = await file.slice(file.size - tailSize, file.size).arrayBuffer();
  const tailView = new DataView(tailBuffer);

  let eocdOffset = -1;
  for (let i = tailBuffer.byteLength - 22; i >= 0; i--) {
    if (tailView.getUint32(i, true) === 0x06054b50) {
      eocdOffset = i;
      break;
    }
  }

  if (eocdOffset === -1) {
    throw new Error('Assinatura ZIP não encontrada. Envie um arquivo .zip válido.');
  }

  const cdSize = tailView.getUint32(eocdOffset + 12, true);
  const cdOffset = tailView.getUint32(eocdOffset + 16, true);

  if (cdOffset + cdSize > file.size) {
    throw new Error('Diretório central do arquivo .zip está corrompido.');
  }

  const cdBuffer = await file.slice(cdOffset, cdOffset + cdSize).arrayBuffer();
  const cdView = new DataView(cdBuffer);
  const decoder = new TextDecoder('utf-8');
  const executaveis: ExecutavelZipInfo[] = [];

  let pos = 0;
  while (pos + 46 <= cdBuffer.byteLength) {
    if (cdView.getUint32(pos, true) !== 0x02014b50) {
      break;
    }
    const nameLen = cdView.getUint16(pos + 28, true);
    const extraLen = cdView.getUint16(pos + 30, true);
    const commentLen = cdView.getUint16(pos + 32, true);

    const nameBytes = new Uint8Array(cdBuffer, pos + 46, nameLen);
    const caminho = decoder.decode(nameBytes).replace(/\\/g, '/');

    if (!caminho.endsWith('/') && caminho.toLowerCase().endsWith('.exe')) {
      const lower = caminho.toLowerCase();
      const auxiliar = AUXILIARES_CONHECIDOS.some((k) => lower.includes(k));
      executaveis.push({ caminho, auxiliar });
    }

    pos += 46 + nameLen + extraLen + commentLen;
  }

  // Coloca os executáveis principais primeiro e auxiliares por último
  return executaveis.sort((a, b) => Number(a.auxiliar) - Number(b.auxiliar));
}
