# Comandos do docker

## Antes de Usar Baixe e Inicie o Docker Desktop

docker compose up -d --build     # 1º build baixa dependências (precisa internet) <br>
docker compose ps                # status dos 3 containers (pra ver se banco / front / back buildaram) <br>
docker compose logs -f backend   # ver logs do back-end <br>
docker compose down              # derruba tudo (banco continua existindo)
