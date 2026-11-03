# Imagem do Garage com a configuração EMBUTIDA (/etc/garage.toml), para a stack de staging não
# depender de bind mount de arquivo do repositório (evita o erro "Is a directory" em gerenciadores
# que não trazem o repo, como o Dockhand).
#
# Executa como usuário NÃO privilegiado (Trivy DS-0002): o binário /garage roda como UID/GID 1000 e
# o diretório de dados é pré-criado com essa posse, para um named volume novo herdar a permissão.
#
# Embutida no build da imagem `campanha-garage` (ver .github/workflows/images.yml).

FROM alpine:3.20 AS prep
RUN mkdir -p /var/lib/garage/meta /var/lib/garage/data \
    && chown -R 1000:1000 /var/lib/garage

FROM dxflrs/garage:v2.5.0
COPY garage.toml /etc/garage.toml
COPY --from=prep --chown=1000:1000 /var/lib/garage /var/lib/garage
USER 1000:1000
