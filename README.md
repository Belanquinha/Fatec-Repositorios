# Comandos do docker

## Antes de Usar Baixe e Inicie o Docker Desktop

docker compose up -d --build     # 1º build baixa dependências (precisa internet) <br>
docker compose down              # para o container



## erros meus a mecher
upload-projeto.ts:374 — quando o upload de imagem falha, o editor recebe um data: URL Base64 e reporta sucesso. A imagem "sobe", fica na tela, e o projeto é submetido com uma string de megabytes no banco no lugar de uma URL. Ninguém nunca soube que o upload falhou.
upload-projeto.ts:619 — mesmo padrão na capa: se o upload falha, manda o preview Base64 como imagemCapaUrl.
O conserto é mostrar o erro ao usuário em vez de fingir que deu certo. 

