# Imagem do Garage com a configuração EMBUTIDA (/etc/garage.toml), para a stack de staging não
# depender de bind mount de arquivo do repositório (evita o erro "Is a directory" em gerenciadores
# que não trazem o repo, como o Dockhand).
#
# Embutida no build da imagem `campanha-garage` (ver .github/workflows/images.yml).
FROM dxflrs/garage:v2.5.0
COPY garage.toml /etc/garage.toml
