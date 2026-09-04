import { Component } from '@angular/core';
import { Button } from '../../../shared/directives/button';

@Component({
  selector: 'app-cadastro-instituicao',
  imports: [],
  templateUrl: './cadastro-instituicao.html',
  styleUrl: './cadastro-instituicao.css',
})
export class CadastroInstituicao {
  async onSubmit(event: Event): Promise<void> {
    event.preventDefault();
    const form = event.target as HTMLFormElement;
    const fd = new FormData(form);

    const body = {
      nome: String(fd.get('nome_instituicao') ?? '').trim(),
      cnpj: String(fd.get('cnpj') ?? '').replace(/\D/g, ''),
      endereco: String(fd.get('endereco') ?? '').trim(),
      cidade: String(fd.get('cidade') ?? '').trim(),
      estado: String(fd.get('estado') ?? '').trim(),
      gestor: {
        nome: String(fd.get('nome_gestor') ?? '').trim(),
        email: String(fd.get('email_gestor') ?? '').trim(),
        telefone: String(fd.get('telefone_gestor') ?? '').replace(/\D/g, ''),
        senha: String(fd.get('senha_gestor') ?? '')
      }
    };

    try {
      const headers: Record<string, string> = { 'Content-Type': 'application/json' };
      const accessToken = typeof window !== 'undefined' ? window.localStorage.getItem('accessToken') : null;
      const tokenType = typeof window !== 'undefined' ? window.localStorage.getItem('tokenType') ?? 'Bearer' : 'Bearer';
      if (accessToken) {
        headers['Authorization'] = `${tokenType} ${accessToken}`;
      }

      const res = await fetch('http://localhost:4040/instituicoes', {
        method: 'POST',
        headers,
        body: JSON.stringify(body),
      });

      if (res.status === 201 || res.ok) {
        alert('Instituição cadastrada com sucesso.');
        form.reset();
      } else if (res.status === 400) {
        const json = await res.json().catch(() => null);
        console.error('Bad request:', json ?? await res.text());
        alert('Dados inválidos. Verifique os campos.');
      } else if (res.status === 401) {
        alert('Não autorizado. Faça login antes de cadastrar.');
      } else {
        const txt = await res.text().catch(() => '');
        console.error('Erro ao cadastrar instituição:', res.status, txt);
        alert('Erro ao cadastrar instituição. Veja console para mais detalhes.');
      }
    } catch (err) {
      console.error('Erro de rede:', err);
      alert('Erro de rede ao contatar o servidor.');
    }
  }
}
