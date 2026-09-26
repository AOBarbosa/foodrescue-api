#!/usr/bin/env bash

# Popula o banco de desenvolvimento com um cenario de demonstracao completo e
# executa um smoke test ponta a ponta da API (UC01, UC02, UC03 e UC05).
#
# Pre-requisitos: banco do Docker Compose e API em execucao, curl, jq e docker.
# Uso padrao: ./seed-and-smoke-test.sh
# Outra URL:  BASE_URL=http://localhost:9090 ./seed-and-smoke-test.sh
#
# Variaveis opcionais:
#   SEED_FORECAST_ALL=false   gera previsao (UC05) so para o produto do smoke test
#   SEED_FORECAST_DELAY=15    segundos entre previsoes, para respeitar a cota por minuto do Gemini
#   SEED_API_WAIT_SECONDS=60  tempo maximo aguardando a API responder
#
# ATENCAO: antes de criar os dados, o script remove todos os registros das
# tabelas da aplicacao no banco de desenvolvimento configurado no Docker Compose.
#
# Enquanto o UC04 (registrar venda) nao tiver API, o historico de vendas e
# inserido direto no banco. Ele e deterministico: rodar o script duas vezes no
# mesmo dia gera os mesmos numeros.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="${SEED_COMPOSE_FILE:-${SCRIPT_DIR}/docker-compose.yml}"
BASE_URL="${BASE_URL:-http://localhost:8080}"
DB_SERVICE="${SEED_DB_SERVICE:-db}"
DB_USER="${SEED_DB_USER:-postgres}"
DB_NAME="${SEED_DB_NAME:-foodrescue}"
ESTABLISHMENT_EMAIL="${SEED_ESTABLISHMENT_EMAIL:-estabelecimento.teste@foodrescue.local}"
CONSUMER_EMAIL="${SEED_CONSUMER_EMAIL:-consumidor.teste@foodrescue.local}"
TEST_PASSWORD="${SEED_PASSWORD:-Teste@123}"
MAIN_CNPJ="${SEED_ESTABLISHMENT_CNPJ:-11222333000181}"
FORECAST_ALL="${SEED_FORECAST_ALL:-true}"
FORECAST_DELAY="${SEED_FORECAST_DELAY:-15}"
API_WAIT_SECONDS="${SEED_API_WAIT_SECONDS:-60}"
RUN_ID="$(date +%s)-$$"
TODAY="$(date '+%F')"
RESPONSE_FILE="$(mktemp)"
HTTP_STATUS=""
RESPONSE_BODY=""
STEP_NUMBER=0

cleanup() {
    rm -f "${RESPONSE_FILE}"
}
trap cleanup EXIT

require_command() {
    if ! command -v "$1" >/dev/null 2>&1; then
        printf 'Erro: o comando "%s" e obrigatorio.\n' "$1" >&2
        exit 1
    fi
}

request() {
    local expected_statuses="$1"
    local method="$2"
    local path="$3"
    local body="${4:-}"
    local token="${5:-}"
    local -a curl_arguments

    curl_arguments=(
        --silent
        --show-error
        --output "${RESPONSE_FILE}"
        --write-out '%{http_code}'
        --request "${method}"
        --header 'Content-Type: application/json'
    )

    if [[ -n "${token}" ]]; then
        curl_arguments+=(--header "Authorization: Bearer ${token}")
    fi
    if [[ -n "${body}" ]]; then
        curl_arguments+=(--data "${body}")
    fi

    HTTP_STATUS="$(curl "${curl_arguments[@]}" "${BASE_URL}${path}")"
    RESPONSE_BODY="$(<"${RESPONSE_FILE}")"

    if [[ ",${expected_statuses}," != *",${HTTP_STATUS},"* ]]; then
        printf '\nFalha em %s %s\n' "${method}" "${path}" >&2
        printf 'Status esperado: %s | recebido: %s\n' "${expected_statuses}" "${HTTP_STATUS}" >&2
        printf 'Resposta: %s\n' "${RESPONSE_BODY}" >&2
        exit 1
    fi
}

json_value() {
    local expression="$1"
    printf '%s' "${RESPONSE_BODY}" | jq --exit-status --raw-output "${expression}"
}

assert_json() {
    local expression="$1"
    local description="$2"

    if ! printf '%s' "${RESPONSE_BODY}" | jq --exit-status "${expression}" >/dev/null; then
        printf '\nFalha na verificacao: %s\n' "${description}" >&2
        printf 'Resposta: %s\n' "${RESPONSE_BODY}" >&2
        exit 1
    fi
}

psql_exec() {
    docker compose --file "${COMPOSE_FILE}" exec -T "${DB_SERVICE}" \
        psql --username "${DB_USER}" --dbname "${DB_NAME}" --set ON_ERROR_STOP=1 "$@"
}

psql_value() {
    psql_exec --tuples-only --no-align --command "$1"
}

calculate_cnpj_digit() {
    local digits="$1"
    shift
    local -a weights=("$@")
    local sum=0
    local index digit remainder

    for ((index = 0; index < ${#digits}; index++)); do
        digit="${digits:index:1}"
        sum=$((sum + 10#${digit} * weights[index]))
    done

    remainder=$((sum % 11))
    if ((remainder < 2)); then
        printf '0'
    else
        printf '%d' "$((11 - remainder))"
    fi
}

generate_valid_cnpj() {
    local seed root first_digit second_digit
    seed=$((( $(date +%s) + $$ ) % 100000000))
    printf -v root '%08d0001' "${seed}"
    first_digit="$(calculate_cnpj_digit "${root}" 5 4 3 2 9 8 7 6 5 4 3 2)"
    second_digit="$(calculate_cnpj_digit "${root}${first_digit}" 6 5 4 3 2 9 8 7 6 5 4 3 2)"
    printf '%s%s%s' "${root}" "${first_digit}" "${second_digit}"
}

date_offset() {
    local days="$1"

    if date -v+1d '+%F' >/dev/null 2>&1; then
        if ((days >= 0)); then
            date -v+"${days}"d '+%F'
        else
            date -v"${days}"d '+%F'
        fi
    else
        date --date="${days} days" '+%F'
    fi
}

step() {
    STEP_NUMBER=$((STEP_NUMBER + 1))
    printf '\n[%02d] %s\n' "${STEP_NUMBER}" "$1"
}

info() {
    printf '     %s\n' "$1"
}

check_database() {
    if ! psql_value 'SELECT 1' >/dev/null 2>&1; then
        printf 'Erro: nao foi possivel acessar o banco pelo servico "%s" do Docker Compose.\n' "${DB_SERVICE}" >&2
        printf 'Suba o banco com: docker compose up -d db\n' >&2
        exit 1
    fi
}

wait_for_api() {
    local waited=0

    until [[ "$(curl --silent --output /dev/null --write-out '%{http_code}' "${BASE_URL}/establishments" || true)" == '200' ]]; do
        if ((waited >= API_WAIT_SECONDS)); then
            printf 'Erro: a API nao respondeu em %s apos %ss.\n' "${BASE_URL}" "${API_WAIT_SECONDS}" >&2
            exit 1
        fi
        sleep 2
        waited=$((waited + 2))
    done
}

# Limpa apenas as tabelas/sequences que existem, para funcionar tanto em
# branches com o UC05 (demand_forecasts) quanto sem ele.
reset_database() {
    psql_exec --quiet >/dev/null <<'SQL'
SET client_min_messages TO warning;
DO $$
DECLARE
    item text;
BEGIN
    FOREACH item IN ARRAY ARRAY[
        'demand_forecasts_aud', 'ai_recommendations_aud', 'offer_orders_aud', 'offers_aud',
        'sales_aud', 'products_aud', 'consumers_aud', 'establishments_aud',
        'demand_forecasts', 'ai_recommendations', 'offer_orders', 'offers',
        'sales', 'products', 'consumers', 'establishments', 'revinfo'
    ] LOOP
        IF to_regclass(item) IS NOT NULL THEN
            EXECUTE format('TRUNCATE TABLE %I CASCADE', item);
        END IF;
    END LOOP;

    FOREACH item IN ARRAY ARRAY[
        'seq_demand_forecast', 'seq_ai_recommendation', 'seq_offer_order', 'seq_offer',
        'seq_sale', 'seq_product', 'seq_consumer', 'seq_establishment', 'seq_revinfo'
    ] LOOP
        IF to_regclass(item) IS NOT NULL THEN
            EXECUTE format('ALTER SEQUENCE %I RESTART WITH 1', item);
        END IF;
    END LOOP;
END
$$;
SQL
}

# ---------------------------------------------------------------------------
# Catalogo de demonstracao. Cada produto representa um cenario:
#   nome | categoria | preco | estoque | validade (dias a partir de hoje) |
#   vendas base manha/tarde/noite por horario | dias de historico | cenario
# ---------------------------------------------------------------------------
CATALOG=(
    'Pao frances|PADARIA|0.90|120|1|12|6|8|28|Giro alto e estoque alto: previsao HIGH, possivel sobra'
    'Pao de queijo|SALGADO|4.50|40|2|3|2|3|28|Vendas estaveis: previsao HIGH'
    'Coxinha|SALGADO|7.00|6|1|2|3|4|28|Estoque baixo: previsao limitada ao estoque'
    'Torta de frango|SALGADO|9.50|12|3|1|2|2|14|Duas semanas de historico: confianca MEDIUM'
    'Bolo de cenoura|DOCE|35.00|20|0|0|1|1|28|Vence hoje, estoque bem acima da demanda: risco de desperdicio'
    'Sanduiche natural|LANCHE|18.90|4|-1|1|2|1|21|Validade vencida: alerta no UC03'
    'Sonho|DOCE|6.00|15|2|0|0|0|0|Produto novo com 3 vendas: previsao recusada (422)'
    'Brigadeiro|DOCE|3.00|0|10|2|2|3|28|Estoque zerado: previsao 0'
)

PRODUCT_IDS=()
PRODUCT_NAMES=()
PRODUCT_SCENARIOS=()
PRODUCT_HISTORY_ROWS=()

product_id_by_name() {
    local name="$1"
    local index

    for index in "${!PRODUCT_NAMES[@]}"; do
        if [[ "${PRODUCT_NAMES[index]}" == "${name}" ]]; then
            printf '%s' "${PRODUCT_IDS[index]}"
            return
        fi
    done
    printf 'Erro: produto "%s" nao encontrado no catalogo.\n' "${name}" >&2
    exit 1
}

create_product() {
    local name="$1"
    local category="$2"
    local price="$3"
    local product_payload

    product_payload="$(jq --null-input \
        --arg name "${name}" \
        --arg category "${category}" \
        --argjson originalPrice "${price}" \
        '{name: $name, category: $category, originalPrice: $originalPrice, photoUrl: null}')"
    request '200' POST '/products' "${product_payload}" "${establishment_token}"
    assert_json ".data.name == \"${name}\" and .data.currentPrice == ${price}" 'o produto deve ser criado com preco atual igual ao original'
    json_value '.data.id'
}

update_inventory() {
    local product_id="$1"
    local stock="$2"
    local expiration_offset="$3"
    local expiration_date inventory_payload

    expiration_date="$(date_offset "${expiration_offset}")"
    inventory_payload="$(jq --null-input \
        --argjson stockQuantity "${stock}" \
        --arg expirationDate "${expiration_date}" \
        '{stockQuantity: $stockQuantity, expirationDate: $expirationDate}')"
    request '200' PATCH "/products/${product_id}/inventory" "${inventory_payload}" "${establishment_token}"
    assert_json ".data.product.stockQuantity == ${stock}" 'o estoque atualizado deve ser devolvido imediatamente'
    assert_json ".data.product.expirationDate == \"${expiration_date}\"" 'a validade deve ser persistida'
    assert_json '.data.product.modificationDate != null' 'a data da ultima modificacao deve ser informada'
    if ((expiration_offset < 0)); then
        assert_json '.data.expirationDateInPast == true' 'validade passada deve alertar sem impedir o salvamento'
    else
        assert_json '.data.expirationDateInPast == false' 'validade de hoje em diante nao deve gerar alerta'
    fi
}

# Insere o historico de vendas das ultimas semanas (UC04 ainda sem API).
# Horarios de 07h a 21h; sexta vende 20% a mais e fim de semana 50% a mais;
# uma variacao deterministica de -1 a +2 unidades muda de semana para semana,
# evitando numeros identicos.
seed_sales_history() {
    local values
    values="$(IFS=,; printf '%s' "${PRODUCT_HISTORY_ROWS[*]}")"

    psql_exec --quiet >/dev/null <<SQL
INSERT INTO sales (id, product_id, quantity, unit_price, sold_at, active, creation_date)
SELECT nextval('seq_sale'), product_id, quantity, price, sold_at, true, now()
FROM (
    SELECT c.product_id,
           c.price,
           (DATE '${TODAY}' - d) + make_time(s.hour, (d * 7 + s.hour * 11) % 60, 0) AS sold_at,
           GREATEST(0, ROUND(
               CASE s.period WHEN 'M' THEN c.morning WHEN 'A' THEN c.afternoon ELSE c.evening END
               * CASE EXTRACT(ISODOW FROM DATE '${TODAY}' - d)
                     WHEN 6 THEN 1.5 WHEN 7 THEN 1.5 WHEN 5 THEN 1.2 ELSE 1.0 END
           ) + ((d * 5 + s.hour) % 4) - 1)::int AS quantity
    FROM (VALUES ${values}) AS c(product_id, price, morning, afternoon, evening, days_back)
    CROSS JOIN LATERAL generate_series(1, c.days_back) AS d
    CROSS JOIN (VALUES (7, 'M'), (9, 'M'), (11, 'M'), (13, 'A'), (15, 'A'), (17, 'E'), (19, 'E'), (21, 'E'))
        AS s(hour, period)
) generated
WHERE quantity > 0
ORDER BY sold_at;

INSERT INTO sales (id, product_id, quantity, unit_price, sold_at, active, creation_date)
VALUES
    (nextval('seq_sale'), $(product_id_by_name 'Sonho'), 2, 6.00, (DATE '${TODAY}' - 3) + TIME '16:10', true, now()),
    (nextval('seq_sale'), $(product_id_by_name 'Sonho'), 1, 6.00, (DATE '${TODAY}' - 2) + TIME '10:25', true, now()),
    (nextval('seq_sale'), $(product_id_by_name 'Sonho'), 3, 6.00, (DATE '${TODAY}' - 1) + TIME '17:40', true, now());
SQL
}

print_forecast() {
    local label="$1"
    printf '%s' "${RESPONSE_BODY}" | jq --raw-output --arg label "${label}" \
        '"     \($label): \(.data.predictedQuantity) un. ate \(.data.forecastUntil[11:16]) | estoque \(.data.stockQuantity) | \(.data.confidence) | \(.data.source)"'
    printf '%s' "${RESPONSE_BODY}" | jq --raw-output \
        'if .data.rationale then "       \"\(.data.rationale)\"" else empty end'
}

require_command curl
require_command jq
require_command docker

step 'Verificando banco e API'
check_database
wait_for_api
info "API acessivel em ${BASE_URL}"

step 'Reiniciando os dados do banco de desenvolvimento'
reset_database
info 'Tabelas limpas e sequences reiniciadas'

step 'UC01 - Criando o estabelecimento principal e testando login'
establishment_payload="$(jq --null-input \
    --arg name 'Padaria FoodRescue Teste' \
    --arg cnpj "${MAIN_CNPJ}" \
    --arg address 'Rua dos Testes, 100' \
    --arg category 'BAKERY' \
    --arg email "${ESTABLISHMENT_EMAIL}" \
    --arg password "${TEST_PASSWORD}" \
    '{id: null, name: $name, cnpj: $cnpj, address: $address, category: $category, email: $email, password: $password}')"
request '201' POST '/establishments' "${establishment_payload}"
request '422' POST '/establishments' "${establishment_payload}"
info 'CNPJ/e-mail duplicado rejeitado (422)'

establishment_login="$(jq --null-input \
    --arg email "${ESTABLISHMENT_EMAIL}" \
    --arg password "${TEST_PASSWORD}" \
    '{email: $email, password: $password}')"
request '200' POST '/establishments/login' "${establishment_login}"
establishment_id="$(json_value '.data.establishment.id')"
establishment_token="$(json_value '.data.token')"

wrong_login="$(jq --null-input --arg email "${ESTABLISHMENT_EMAIL}" '{email: $email, password: "senha-errada"}')"
request '422' POST '/establishments/login' "${wrong_login}"
info 'Login com senha errada rejeitado (422)'

step 'UC01 - Consulta publica e atualizacao do proprio estabelecimento'
request '200' GET "/establishments/${establishment_id}"
assert_json ".data.id == ${establishment_id}" 'o estabelecimento consultado deve ser o principal'

establishment_update="$(jq --null-input \
    --arg name 'Padaria FoodRescue Teste' \
    --arg cnpj "${MAIN_CNPJ}" \
    --arg address 'Rua dos Testes, 100 - Centro' \
    --arg category 'BAKERY' \
    --arg email "${ESTABLISHMENT_EMAIL}" \
    '{name: $name, cnpj: $cnpj, address: $address, category: $category, email: $email, password: null}')"
request '200' PUT "/establishments/${establishment_id}" "${establishment_update}" "${establishment_token}"
assert_json '.data.address == "Rua dos Testes, 100 - Centro"' 'o endereco deve ser atualizado'

step 'Criando o consumidor de teste e testando login'
consumer_payload="$(jq --null-input \
    --arg name 'Consumidor FoodRescue Teste' \
    --arg email "${CONSUMER_EMAIL}" \
    --arg password "${TEST_PASSWORD}" \
    '{id: null, name: $name, email: $email, password: $password}')"
request '201' POST '/consumers' "${consumer_payload}"

consumer_login="$(jq --null-input \
    --arg email "${CONSUMER_EMAIL}" \
    --arg password "${TEST_PASSWORD}" \
    '{email: $email, password: $password}')"
request '200' POST '/consumers/login' "${consumer_login}"
consumer_id="$(json_value '.data.consumer.id')"
consumer_token="$(json_value '.data.token')"

step "UC02 - Cadastrando o catalogo de demonstracao (${#CATALOG[@]} produtos)"
for entry in "${CATALOG[@]}"; do
    IFS='|' read -r name category price stock expiration morning afternoon evening days_back scenario <<<"${entry}"
    product_id="$(create_product "${name}" "${category}" "${price}")"
    PRODUCT_IDS+=("${product_id}")
    PRODUCT_NAMES+=("${name}")
    PRODUCT_SCENARIOS+=("${scenario}")
    if ((days_back > 0)); then
        PRODUCT_HISTORY_ROWS+=("(${product_id}, ${price}, ${morning}, ${afternoon}, ${evening}, ${days_back})")
    fi
    info "#${product_id} ${name}"
done

invalid_product='{"name":"","category":"DOCE","originalPrice":-1,"photoUrl":null}'
request '400,422' POST '/products' "${invalid_product}" "${establishment_token}"
info 'Produto com nome vazio e preco negativo rejeitado'

step 'UC03 - Definindo estoque e validade de cada produto'
for entry in "${CATALOG[@]}"; do
    IFS='|' read -r name category price stock expiration _ <<<"${entry}"
    update_inventory "$(product_id_by_name "${name}")" "${stock}" "${expiration}"
done
info 'Estoques e validades aplicados (inclui validade vencida e estoque zero)'

pao_de_queijo_id="$(product_id_by_name 'Pao de queijo')"
request '422' PATCH "/products/${pao_de_queijo_id}/inventory" '{"stockQuantity":-1}' "${establishment_token}"
request '422' PATCH "/products/${pao_de_queijo_id}/inventory" '{}' "${establishment_token}"
info 'Estoque negativo e atualizacao vazia rejeitados (422)'

request '200' GET '/products' '' "${establishment_token}"
assert_json ".data | length == ${#CATALOG[@]}" 'a listagem deve conter todo o catalogo'
request '200' GET "/products/${pao_de_queijo_id}" '' "${establishment_token}"
assert_json '.data.stockQuantity == 40' 'a consulta deve refletir o estoque atualizado'

step 'UC04 (via banco) - Gerando historico de vendas das ultimas semanas'
seed_sales_history
sales_summary="$(psql_value "SELECT count(*) || ' vendas, ' || sum(quantity) || ' unidades, de ' || min(sold_at)::date || ' a ' || max(sold_at)::date FROM sales")"
info "${sales_summary}"

forecast_available="$(psql_value "SELECT to_regclass('demand_forecasts') IS NOT NULL")"
if [[ "${forecast_available}" == 't' ]]; then
    step 'UC05 - Previsao de demanda'
    sonho_id="$(product_id_by_name 'Sonho')"

    request '404' GET "/products/${pao_de_queijo_id}/demand-forecast/latest" '' "${establishment_token}"
    info 'Produto ainda sem previsao: 404'

    request '422' POST "/products/${sonho_id}/demand-forecast" '' "${establishment_token}"
    assert_json '.code == "BUSINESS_RULE_VIOLATION"' 'historico insuficiente deve ser recusado'
    info 'Sonho (3 vendas): previsao recusada por historico insuficiente (422)'

    request '403' POST "/products/${pao_de_queijo_id}/demand-forecast" '' "${consumer_token}"
    info 'Consumidor nao pode gerar previsao (403)'

    request '200' POST "/products/${pao_de_queijo_id}/demand-forecast" '' "${establishment_token}"
    assert_json '.data.predictedQuantity >= 0 and .data.predictedQuantity <= .data.stockQuantity' 'a previsao deve ficar entre 0 e o estoque'
    assert_json '.data.confidence | IN("LOW", "MEDIUM", "HIGH")' 'a confianca deve ser valida'
    assert_json '.data.source != null' 'a origem da previsao deve ser informada'
    forecast_id="$(json_value '.data.id')"
    print_forecast 'Pao de queijo'
    if [[ "$(json_value '.data.source')" != gemini:* ]]; then
        info 'Obs.: previsao feita pelo algoritmo local (Gemini sem chave, fora do ar ou sem cota; veja o log da API).'
    fi

    request '200' GET "/products/${pao_de_queijo_id}/demand-forecast/latest" '' "${establishment_token}"
    assert_json ".data.id == ${forecast_id}" 'a ultima previsao deve ser a recem-gerada'

    if [[ "${FORECAST_ALL}" == 'true' ]]; then
        info "Gerando previsoes para os demais produtos com historico (intervalo de ${FORECAST_DELAY}s):"
        for index in "${!PRODUCT_IDS[@]}"; do
            name="${PRODUCT_NAMES[index]}"
            if [[ "${name}" == 'Pao de queijo' || "${name}" == 'Sonho' ]]; then
                continue
            fi
            sleep "${FORECAST_DELAY}"
            request '200' POST "/products/${PRODUCT_IDS[index]}/demand-forecast" '' "${establishment_token}"
            print_forecast "${name}"
        done
    fi
else
    step 'UC05 - Previsao de demanda (pulado)'
    info 'A tabela demand_forecasts nao existe: a API em execucao nao tem o UC05.'
fi

step 'Isolamento entre estabelecimentos e DELETE de estabelecimento descartavel'
temporary_cnpj="$(generate_valid_cnpj)"
temporary_email="estabelecimento.descartavel.${RUN_ID}@foodrescue.local"
temporary_payload="$(jq --null-input \
    --arg name 'Estabelecimento Descartavel' \
    --arg cnpj "${temporary_cnpj}" \
    --arg address 'Rua Temporaria, 1' \
    --arg category 'MARKET' \
    --arg email "${temporary_email}" \
    --arg password "${TEST_PASSWORD}" \
    '{id: null, name: $name, cnpj: $cnpj, address: $address, category: $category, email: $email, password: $password}')"
request '201' POST '/establishments' "${temporary_payload}"
temporary_id="$(json_value '.data.establishment.id')"
temporary_token="$(json_value '.data.token')"

request '404' GET "/products/${pao_de_queijo_id}" '' "${temporary_token}"
request '404' PATCH "/products/${pao_de_queijo_id}/inventory" '{"stockQuantity":99}' "${temporary_token}"
if [[ "${forecast_available}" == 't' ]]; then
    request '404' POST "/products/${pao_de_queijo_id}/demand-forecast" '' "${temporary_token}"
fi
info 'Outro estabelecimento nao ve nem altera produtos alheios (404)'

unauthorized_product_payload='{"name":"Produto indevido","category":"TESTE","originalPrice":1.00,"photoUrl":null}'
request '403' POST '/products' "${unauthorized_product_payload}" "${consumer_token}"
request '401' GET '/products'
info 'Consumidor nao cadastra produto (403) e rota protegida exige token (401)'

request '204' DELETE "/establishments/${temporary_id}" '' "${temporary_token}"
request '404' GET "/establishments/${temporary_id}"
info 'Estabelecimento descartavel desativado'

printf '\nSeed e smoke test concluidos com sucesso.\n'
printf '\nCredenciais:\n'
printf '  Estabelecimento: %s (id %s)\n' "${ESTABLISHMENT_EMAIL}" "${establishment_id}"
printf '  Consumidor:      %s (id %s)\n' "${CONSUMER_EMAIL}" "${consumer_id}"
printf '  Senha de ambos:  %s\n' "${TEST_PASSWORD}"
printf '\nProdutos e cenarios de demonstracao:\n'
for index in "${!PRODUCT_IDS[@]}"; do
    printf '  #%-3s %-18s %s\n' "${PRODUCT_IDS[index]}" "${PRODUCT_NAMES[index]}" "${PRODUCT_SCENARIOS[index]}"
done
