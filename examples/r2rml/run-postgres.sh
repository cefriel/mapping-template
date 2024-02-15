docker run -d \
  --name postgres-container \
  -p 5432:5432 \
  -e POSTGRES_USER=r2rml \
  -e POSTGRES_PASSWORD=r2rml \
  -e POSTGRES_DB=r2rml \
  -v ./d016-postgresql.sql:/docker-entrypoint-initdb.d/d016-postgresql.sql \
  postgres:13