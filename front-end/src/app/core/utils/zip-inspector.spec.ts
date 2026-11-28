import { describe, it, expect } from 'vitest';
import { listarExecutaveisNoZip } from './zip-inspector';

function criarZipSintetico(nomes: string[]): File {
  const encoder = new TextEncoder();
  const cdChunks: Uint8Array[] = [];
  let cdSize = 0;

  for (const nome of nomes) {
    const nomeBytes = encoder.encode(nome);
    const entry = new Uint8Array(46 + nomeBytes.length);
    const view = new DataView(entry.buffer);
    view.setUint32(0, 0x02014b50, true); // Central File Header
    view.setUint16(28, nomeBytes.length, true);
    entry.set(nomeBytes, 46);
    cdChunks.push(entry);
    cdSize += entry.length;
  }

  const eocd = new Uint8Array(22);
  const eocdView = new DataView(eocd.buffer);
  eocdView.setUint32(0, 0x06054b50, true);
  eocdView.setUint16(8, nomes.length, true);
  eocdView.setUint16(10, nomes.length, true);
  eocdView.setUint32(12, cdSize, true);
  eocdView.setUint32(16, 0, true); // cdOffset = 0

  const blob = new Blob([...cdChunks, eocd] as BlobPart[], { type: 'application/zip' });
  return new File([blob], 'teste.zip', { type: 'application/zip' });
}

describe('zip-inspector', () => {
  it('lista executáveis e ordena auxiliares por último', async () => {
    const file = criarZipSintetico([
      'Jogo/UnityCrashHandler64.exe',
      'Jogo/readme.txt',
      'Jogo/FatecRun.exe',
    ]);

    const res = await listarExecutaveisNoZip(file);
    expect(res).toEqual([
      { caminho: 'Jogo/FatecRun.exe', auxiliar: false },
      { caminho: 'Jogo/UnityCrashHandler64.exe', auxiliar: true },
    ]);
  });

  it('rejeita arquivo sem assinatura ZIP', async () => {
    const file = new File(['nao eh um zip valido de verdade'], 'falso.zip');
    await expect(listarExecutaveisNoZip(file)).rejects.toThrow(/Assinatura ZIP/);
  });
});
