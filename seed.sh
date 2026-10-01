#!/usr/bin/env bash

# Popula o banco de desenvolvimento com dados de demonstracao, para testar a
# aplicacao manualmente pelo frontend. Nao chama a API nem o Gemini: apenas
# grava os dados direto no banco, em uma unica transacao.
#
# Pre-requisitos: banco do Docker Compose em execucao (docker compose up -d db)
# e o schema ja criado pelo Liquibase (suba a API ao menos uma vez).
#
# Uso: ./seed.sh
#
# Variaveis opcionais: SEED_DB_SERVICE, SEED_DB_USER, SEED_DB_NAME,
# SEED_ESTABLISHMENT_EMAIL, SEED_CONSUMER_EMAIL, SEED_PASSWORD,
# SEED_ESTABLISHMENT_CNPJ, SEED_OTHER_ESTABLISHMENT_EMAIL,
# SEED_OTHER_ESTABLISHMENT_CNPJ.
#
# ATENCAO: antes de gravar, remove todos os registros das tabelas da aplicacao.
#
# O que e criado:
# - 2 estabelecimentos e 1 consumidor, todos com a mesma senha. O segundo
#   estabelecimento serve para conferir que um nao enxerga os dados do outro;
# - 13 produtos (11 do estabelecimento principal), cada um montando um cenario
#   de demonstracao, incluindo um produto desativado;
# - ~2.100 vendas das ultimas 4 semanas (UC04);
# - previsoes de demanda do dia (UC05) para parte dos produtos, calculadas as
#   06:00 com a mesma media por dia da semana da API, o que alimenta o painel
#   de risco de desperdicio (UC06). Os demais produtos ficam sem previsao para
#   serem previstos pelo frontend;
# - recomendacoes de desconto (UC07): uma pendente e um historico com os
#   status EXPIRED, REFUSED e ADJUSTED.
#
# Tudo e deterministico: rodar duas vezes no mesmo dia gera os mesmos numeros.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="${SEED_COMPOSE_FILE:-${SCRIPT_DIR}/docker-compose.yml}"
DB_SERVICE="${SEED_DB_SERVICE:-db}"
DB_USER="${SEED_DB_USER:-postgres}"
DB_NAME="${SEED_DB_NAME:-foodrescue}"
ESTABLISHMENT_EMAIL="${SEED_ESTABLISHMENT_EMAIL:-estabelecimento.teste@foodrescue.local}"
OTHER_ESTABLISHMENT_EMAIL="${SEED_OTHER_ESTABLISHMENT_EMAIL:-restaurante.teste@foodrescue.local}"
CONSUMER_EMAIL="${SEED_CONSUMER_EMAIL:-consumidor.teste@foodrescue.local}"
TEST_PASSWORD="${SEED_PASSWORD:-Teste@123}"
MAIN_CNPJ="${SEED_ESTABLISHMENT_CNPJ:-11222333000181}"
OTHER_CNPJ="${SEED_OTHER_ESTABLISHMENT_CNPJ:-11444777000161}"
TODAY="$(date '+%F')"

psql_exec() {
    docker compose --file "${COMPOSE_FILE}" exec -T "${DB_SERVICE}" \
        psql --username "${DB_USER}" --dbname "${DB_NAME}" --set ON_ERROR_STOP=1 "$@"
}

if ! command -v docker >/dev/null 2>&1; then
    printf 'Erro: o comando "docker" e obrigatorio.\n' >&2
    exit 1
fi

if ! psql_exec --tuples-only --no-align --command 'SELECT 1' >/dev/null 2>&1; then
    printf 'Erro: nao foi possivel acessar o banco pelo servico "%s" do Docker Compose.\n' "${DB_SERVICE}" >&2
    printf 'Suba o banco com: docker compose up -d db\n' >&2
    exit 1
fi

if [[ "$(psql_exec --tuples-only --no-align --command "SELECT to_regclass('demand_forecasts') IS NOT NULL")" != 't' ]]; then
    printf 'Erro: o schema ainda nao existe ou esta desatualizado. Suba a API uma vez para o Liquibase atualiza-lo.\n' >&2
    exit 1
fi

printf 'Populando o banco "%s" (data de referencia: %s)...\n\n' "${DB_NAME}" "${TODAY}"

psql_exec --quiet \
    --set today="${TODAY}" \
    --set establishment_email="${ESTABLISHMENT_EMAIL}" \
    --set other_establishment_email="${OTHER_ESTABLISHMENT_EMAIL}" \
    --set consumer_email="${CONSUMER_EMAIL}" \
    --set password="${TEST_PASSWORD}" \
    --set cnpj="${MAIN_CNPJ}" \
    --set other_cnpj="${OTHER_CNPJ}" <<'SQL'
SET client_min_messages TO warning;
BEGIN;

TRUNCATE TABLE
    demand_forecasts_aud, ai_recommendations_aud, offer_orders_aud, offers_aud,
    sales_aud, products_aud, consumers_aud, establishments_aud,
    demand_forecasts, ai_recommendations, offer_orders, offers,
    sales, products, consumers, establishments, revinfo
    CASCADE;

ALTER SEQUENCE seq_demand_forecast RESTART WITH 1;
ALTER SEQUENCE seq_ai_recommendation RESTART WITH 1;
ALTER SEQUENCE seq_offer_order RESTART WITH 1;
ALTER SEQUENCE seq_offer RESTART WITH 1;
ALTER SEQUENCE seq_sale RESTART WITH 1;
ALTER SEQUENCE seq_product RESTART WITH 1;
ALTER SEQUENCE seq_consumer RESTART WITH 1;
ALTER SEQUENCE seq_establishment RESTART WITH 1;
ALTER SEQUENCE seq_revinfo RESTART WITH 1;

-- Hash BCrypt ($2a$) compativel com o BCryptPasswordEncoder da API.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

INSERT INTO establishments (id, name, cnpj, address, category, email, password_hash, active, creation_date)
VALUES (nextval('seq_establishment'), 'Padaria FoodRescue Teste', :'cnpj', 'Rua dos Testes, 100 - Centro',
        'BAKERY', :'establishment_email', crypt(:'password', gen_salt('bf', 10)), true, now()),
       (nextval('seq_establishment'), 'Restaurante Vizinho Teste', :'other_cnpj', 'Rua dos Testes, 200 - Centro',
        'RESTAURANT', :'other_establishment_email', crypt(:'password', gen_salt('bf', 10)), true, now());

INSERT INTO consumers (id, name, email, password_hash, active, creation_date)
VALUES (nextval('seq_consumer'), 'Consumidor FoodRescue Teste', :'consumer_email',
        crypt(:'password', gen_salt('bf', 10)), true, now());

-- Catalogo de demonstracao: estabelecimento dono, vendas base por horario
-- (manha/tarde/noite), dias de historico, previsao ja gravada (NULL = nenhuma,
-- 'media' = media por dia da semana, 'gemini' = previsao com justificativa da
-- IA) e o cenario que cada produto ilustra.
CREATE TEMP TABLE seed_catalog (
    position   int,
    store      text,
    name       text,
    category   text,
    price      numeric(38, 2),
    stock      int,
    expires_in int,
    morning    int,
    afternoon  int,
    evening    int,
    days_back  int,
    forecast   text,
    active     boolean,
    scenario   text
) ON COMMIT DROP;

INSERT INTO seed_catalog VALUES
    ( 1, 'main',  'Pão francês',       'PADARIA',   0.90, 120,  1, 12, 6, 8, 28, NULL,     true,  'Giro alto e estoque alto: previsão HIGH, possível sobra'),
    ( 2, 'main',  'Pão de queijo',     'SALGADO',   4.50,  40,  2,  3, 2, 3, 28, 'gemini', true,  'Previsão do Gemini já gravada, com justificativa'),
    ( 3, 'main',  'Coxinha',           'SALGADO',   7.00,   6,  1,  2, 3, 4, 28, NULL,     true,  'Estoque baixo: previsão limitada ao estoque'),
    ( 4, 'main',  'Torta de frango',   'SALGADO',   9.50,  12,  3,  1, 2, 2, 14, NULL,     true,  'Duas semanas de histórico: confiança MEDIUM'),
    ( 5, 'main',  'Bolo de cenoura',   'DOCE',     35.00,  45,  0,  0, 0, 1, 28, 'media',  true,  'Vence hoje e em risco: gere a recomendação de desconto'),
    ( 6, 'main',  'Sanduíche natural', 'LANCHE',   18.90,   4, -1,  1, 2, 1, 21, NULL,     true,  'Validade vencida: alerta no estoque'),
    ( 7, 'main',  'Sonho',             'DOCE',      6.00,  15,  2,  0, 0, 0,  0, NULL,     true,  'Produto novo com 3 vendas: previsão recusada (422)'),
    ( 8, 'main',  'Brigadeiro',        'DOCE',      3.00,   0, 10,  2, 2, 3, 28, 'media',  true,  'Estoque zerado: previsão 0 e risco 0%'),
    ( 9, 'main',  'Croissant',         'PADARIA',   8.00,  60,  1,  1, 1, 0, 28, 'media',  true,  'Recomendação de desconto pendente'),
    (10, 'main',  'Baguete',           'PADARIA',  12.00,  45,  1,  1, 0, 0, 28, 'media',  true,  'Histórico de recomendações e desconto ajustado de 25%'),
    (11, 'main',  'Empada',            'SALGADO',   5.50,  10,  2,  1, 1, 1, 28, NULL,     false, 'Produto desativado: não aparece nas listagens'),
    (12, 'other', 'Prato feito',       'REFEICAO', 22.00,  30,  0,  0, 4, 2, 28, 'media',  true,  'Outro estabelecimento: invisível para a padaria'),
    (13, 'other', 'Suco natural',      'BEBIDA',    8.00,  25,  2,  1, 2, 1, 28, NULL,     true,  'Outro estabelecimento: invisível para a padaria');

INSERT INTO products (id, establishment_id, name, category, original_price, current_price, photo_url,
                      stock_quantity, expiration_date, active, creation_date)
SELECT nextval('seq_product'),
       (SELECT id FROM establishments
        WHERE email = CASE store WHEN 'main' THEN :'establishment_email' ELSE :'other_establishment_email' END),
       name, category, price, price, NULL, stock, DATE :'today' + expires_in, active, now()
FROM seed_catalog
ORDER BY position;

-- Historico de vendas: horarios de 07h a 21h; sexta vende 20% a mais e fim de
-- semana 50% a mais; uma variacao deterministica de -1 a +2 unidades muda de
-- semana para semana.
INSERT INTO sales (id, product_id, quantity, unit_price, sold_at, active, creation_date)
SELECT nextval('seq_sale'), product_id, quantity, price, sold_at, true, now()
FROM (
    SELECT p.id AS product_id,
           c.price,
           (DATE :'today' - d) + make_time(s.hour, (d * 7 + s.hour * 11) % 60, 0) AS sold_at,
           GREATEST(0, ROUND(
               CASE s.period WHEN 'M' THEN c.morning WHEN 'A' THEN c.afternoon ELSE c.evening END
               * CASE EXTRACT(ISODOW FROM DATE :'today' - d)
                     WHEN 6 THEN 1.5 WHEN 7 THEN 1.5 WHEN 5 THEN 1.2 ELSE 1.0 END
           ) + ((d * 5 + s.hour) % 4) - 1)::int AS quantity
    FROM seed_catalog c
    JOIN products p ON p.name = c.name
    CROSS JOIN LATERAL generate_series(1, c.days_back) AS d
    CROSS JOIN (VALUES (7, 'M'), (9, 'M'), (11, 'M'), (13, 'A'), (15, 'A'), (17, 'E'), (19, 'E'), (21, 'E'))
        AS s(hour, period)
) generated
WHERE quantity > 0
ORDER BY sold_at;

-- Produto novo: poucas vendas, abaixo do minimo para prever demanda.
INSERT INTO sales (id, product_id, quantity, unit_price, sold_at, active, creation_date)
SELECT nextval('seq_sale'), p.id, v.quantity, p.original_price, (DATE :'today' - v.days_ago) + v.at, true, now()
FROM products p
CROSS JOIN (VALUES (3, 2, TIME '16:10'), (2, 1, TIME '10:25'), (1, 3, TIME '17:40')) AS v(days_ago, quantity, at)
WHERE p.name = 'Sonho';

-- Previsoes do dia (UC05), como se calculadas as 06:00 ate o fechamento (22:00):
-- media das vendas nos mesmos dias da semana de hoje, limitada ao estoque. Com
-- historico completo, cada dia da semana aparece days_back / 7 vezes.
INSERT INTO demand_forecasts (id, product_id, predicted_quantity, stock_quantity, confidence, sample_size,
                              source, rationale, calculated_at, forecast_until, active, creation_date)
SELECT nextval('seq_demand_forecast'), p.id,
       LEAST(ROUND(h.units::numeric / (c.days_back / 7))::int, p.stock_quantity),
       p.stock_quantity,
       CASE WHEN c.days_back / 7 >= 4 THEN 'HIGH' WHEN c.days_back / 7 >= 2 THEN 'MEDIUM' ELSE 'LOW' END,
       h.sample_size,
       CASE c.forecast WHEN 'gemini' THEN 'gemini:gemini-3.8-flash' ELSE 'weekday-hourly-average' END,
       CASE c.forecast WHEN 'gemini'
           THEN 'Vendas estáveis nas últimas 4 semanas neste dia da semana, com picos pela manhã e no fim da tarde.'
       END,
       DATE :'today' + TIME '06:00', DATE :'today' + TIME '22:00', true, DATE :'today' + TIME '06:00'
FROM seed_catalog c
JOIN products p ON p.name = c.name
CROSS JOIN LATERAL (
    SELECT COALESCE(sum(s.quantity), 0) AS units, count(*) AS sample_size
    FROM sales s
    WHERE s.product_id = p.id
      AND EXTRACT(ISODOW FROM s.sold_at) = EXTRACT(ISODOW FROM DATE :'today')
) h
WHERE c.forecast IS NOT NULL
  AND c.days_back >= 7
ORDER BY c.position;

-- Recomendacoes de desconto (UC07). A pendente usa a mesma formula da API
-- (60% do risco + 15 pontos de urgencia, limitado a 60%); as respondidas
-- guardam o percentual aplicado, como a API faz.
INSERT INTO ai_recommendations (id, product_id, type, suggested_percentage, status, previous_recommendation_id,
                                responded_at, active, creation_date)
SELECT nextval('seq_ai_recommendation'), p.id, 'DISCOUNT',
       COALESCE(r.percentage, LEAST(60, ROUND(
           (p.stock_quantity - f.predicted_quantity) * 100.0 / p.stock_quantity * 0.6 + 15, 2))),
       r.status, NULL,
       (DATE :'today' - r.days_ago) + TIME '06:05' + r.answered_after,
       true, (DATE :'today' - r.days_ago) + TIME '06:05'
FROM (VALUES ('Baguete',   'EXPIRED',  40.00, 6, NULL),
             ('Baguete',   'REFUSED',  35.00, 4, INTERVAL '2 hours'),
             ('Baguete',   'ADJUSTED', 25.00, 1, INTERVAL '1 hour'),
             ('Croissant', 'PENDING',  NULL,  0, NULL))
    AS r(product, status, percentage, days_ago, answered_after)
JOIN products p ON p.name = r.product
LEFT JOIN demand_forecasts f ON f.product_id = p.id
ORDER BY r.days_ago DESC;

-- O desconto ajustado de ontem ja foi aplicado ao preco atual.
UPDATE products SET current_price = ROUND(original_price * 0.75, 2) WHERE name = 'Baguete';

\echo 'Produtos criados:'
SELECT p.id AS "ID",
       e.name AS "Estabelecimento",
       p.name AS "Produto",
       p.stock_quantity AS "Estoque",
       to_char(p.expiration_date, 'DD/MM') AS "Validade",
       p.current_price AS "Preço",
       (SELECT count(*) FROM sales s WHERE s.product_id = p.id) AS "Vendas",
       COALESCE(f.predicted_quantity::text || ' (' || f.confidence || ')', '-') AS "Previsão",
       CASE WHEN f.id IS NULL THEN '-'
            WHEN p.stock_quantity = 0 THEN '0.00%'
            ELSE ROUND(GREATEST(p.stock_quantity - f.predicted_quantity, 0) * 100.0 / p.stock_quantity, 2) || '%'
       END AS "Risco",
       c.scenario AS "Cenário"
FROM products p
JOIN establishments e ON e.id = p.establishment_id
JOIN seed_catalog c ON c.name = p.name
LEFT JOIN demand_forecasts f ON f.product_id = p.id
ORDER BY p.id;

\echo 'Recomendações de desconto:'
SELECT r.id AS "ID",
       p.name AS "Produto",
       r.status AS "Status",
       r.suggested_percentage || '%' AS "Desconto",
       to_char(r.creation_date, 'DD/MM HH24:MI') AS "Criada em",
       COALESCE(to_char(r.responded_at, 'DD/MM HH24:MI'), '-') AS "Respondida em"
FROM ai_recommendations r
JOIN products p ON p.id = r.product_id
ORDER BY r.id;

\echo 'Histórico de vendas:'
SELECT count(*) AS "Vendas",
       sum(quantity) AS "Unidades",
       to_char(min(sold_at), 'DD/MM/YYYY') AS "De",
       to_char(max(sold_at), 'DD/MM/YYYY') AS "Até"
FROM sales;

COMMIT;
SQL

printf 'Credenciais:\n'
printf '  Estabelecimento:       %s\n' "${ESTABLISHMENT_EMAIL}"
printf '  Outro estabelecimento: %s\n' "${OTHER_ESTABLISHMENT_EMAIL}"
printf '  Consumidor:            %s\n' "${CONSUMER_EMAIL}"
printf '  Senha de todos:        %s\n' "${TEST_PASSWORD}"
printf '\nProdutos sem previsao podem ser previstos pelo frontend; o risco (limite de 70%%) usa a previsao mais recente.\n'
