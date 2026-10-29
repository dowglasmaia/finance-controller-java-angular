-- Utilizador da aplicação sem DDL (INFRA-SPEC-02, F12). Executar UMA vez, como dono do banco (o utilizador de migração
-- que o Flyway usa), antes da primeira migração. Substitua __APP_PASSWORD__ por um segredo vindo do cofre/segredos.
--   psql -U <dono> -d <banco> -v ON_ERROR_STOP=1 -f prod-roles.sql
-- A aplicação passa a conectar-se como fc_app (SPRING_DATASOURCE_*) e o Flyway como o dono (FLYWAY_USER/FLYWAY_PASSWORD).
CREATE ROLE fc_app LOGIN PASSWORD '__APP_PASSWORD__' NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION;

DO $$ BEGIN EXECUTE format('GRANT CONNECT ON DATABASE %I TO fc_app', current_database()); END $$;
GRANT USAGE ON SCHEMA public TO fc_app;

-- só DML, sobre as tabelas de hoje e sobre as que as próximas migrações (do dono) criarem
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO fc_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO fc_app;

-- ninguém além do dono cria objetos no esquema
REVOKE CREATE ON SCHEMA public FROM PUBLIC;
