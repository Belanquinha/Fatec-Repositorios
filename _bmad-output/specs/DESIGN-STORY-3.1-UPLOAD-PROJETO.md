# Especificação de UX/UI — Tela de Postagem de Projeto (Story 3.1)
**Plataforma**: Fatec-Repositorios  
**Autor**: BMAD Design System (Sally 🎨 UX / Winston 🏗️ Architecture)  
**Data**: 2026-09-21 (Atualizado)

---

## 1. Visão Geral & Filosofia Visual

A tela de **Postagem de Projeto** (`upload-projeto`) é o formulário em que alunos e grupos acadêmicos submetem seus Trabalhos de Conclusão de Curso (TCC) e Projetos Integradores para o repositório da FATEC.

A interface adota o formato de **Painel por Seções Ativas** com geometria técnica de **cantos 100% retos (`border-radius: 0`)**, alinhada ao *Design Suíço / Flat Technical Style*.

---

## 2. Guia Semântico de Cores (Paleta Institucional)

A paleta de cores do arquivo [`paleta.css`](file:///c:/Users/Gabriel/Desktop/Fatec-Repositorios/front-end/src/styles/paleta.css) é aplicada conforme as regras semânticas:

| Cor | Variável CSS | Hexadecimal | Significado Semântico | Uso na Interface |
| :--- | :--- | :--- | :--- | :--- |
| 🟡 **Amarelo Conquista** | `--amarelo-500` | `#DCA703` | **Conquista / Excelência Acadêmica** | Botão Principal de Submissão ("Enviar para Avaliação"), Badges de Destaque. |
| 🔴 **Vermelho CPS** | `--vermelho-500` | `#B20000` | **Centro Paula Souza (Institucional)** | Elementos institucionais do CPS. **NÃO USAR EM MENSAGENS DE ERRO/ALERTA!** |
| 🔵 **Azul Mercado** | `--azul-700` | `#303190` | **Empresas & Conexão Corporativa** | Badges corporativas e links de repositório. |
| 🟢 **Verde Estudante** | `--verde-500` | `#00B32A` | **Comunidade Acadêmica / Estudantes** | Avatares de alunos e confirmação de envio para o professor. |
| ⚪ **Branco Neutro** | `--branco-500` | `#F4F6F9` | **Superfície & Fundo da Aplicação** | Fundo da página e áreas limpas de conteúdo. |
| 🔘 **Cinza Suave** | `--cinza-500` | `#202124` | **Tipografia & Bordas Estruturais** | Textos principais, rótulos e bordas. |

---

## 3. Especificação das Seções

### Seção 1: Dados Gerais & Imagem de Capa
- **Título do Projeto**: Input de texto em destaque (`text-xl font-bold`).
- **Descrição Curta**: Textarea limitada a 144 caracteres com contador dinâmico (`0 / 144`).
- **Capa do Projeto**: Dropzone responsivo com preview em tempo real e opção de substituição.

---

### Seção 2: Instituição & Validação Acadêmica
- **Instituição (Fatec)**: Select populado dinamicamente via API (*ex: Fatec Ipiranga, Fatec Pindamonhangaba*).
- **E-mail do Professor Responsável (Autocomplete Flexível)**:
  - Permite a digitação manual de e-mails não cadastrados com o badge informativo:  
    `[ ✉️ Professor Convidado — registro informativo, sem envio de notificações ]`
- **Link do Repositório**: Input para URL do GitHub, GitLab ou Bitbucket.
- **Palavras-chave**: Tags editáveis para categorização.

---

### Seção 3: Conteúdo & Mídias (Editor.js)
- **Editor Rich Content (Editor.js)**:
  - Blocos suportados: Cabeçalhos, Parágrafos, Listas, Código e Citações.
  - **Mídias Adicionais**: As imagens complementares do projeto são inseridas como blocos de imagem nativos diretamente dentro do Editor.js.

---

### Seção 4: Integrantes do Projeto
- **Lista Dinâmica de Participantes**:
  - Botão `+ Adicionar Integrante` adiciona novos cards sem limite estático.
  - Cada card possui os campos:
    1. **Nome Completo**
    2. **Link do LinkedIn**
    3. **Botão Remover** (Lixeira).

---

## 4. Rodapé & Ação Final de Submissão

```html
<!-- Rodapé de Ação Final -->
<div class="bg-white border-t border-gray-200 p-4 sticky bottom-0 flex flex-col sm:flex-row items-center justify-between gap-4 shadow-lg">
  <div class="flex items-center gap-2 text-xs text-gray-500">
    <span class="material-symbols-outlined text-amber-500 text-base">info</span>
    <span>O projeto será submetido à revisão do Professor Responsável antes da publicação.</span>
  </div>

  <button type="submit" 
          class="w-full sm:w-auto bg-amarelo-500 hover:bg-amarelo-600 text-gray-900 font-bold px-6 py-3 shadow transition-all flex items-center justify-center gap-2">
    <span>Enviar para Avaliação Acadêmica</span>
    <span class="material-symbols-outlined">send</span>
  </button>
</div>
```
