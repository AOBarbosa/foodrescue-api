# Fluxo de branches e contribuição

## Branches

- `main` — branch estável/de release. Só recebe atualizações via merge de `develop`. Nunca recebe push direto nem PR de outra branch.
- `develop` — branch de integração. É o alvo (`base`) dos Pull Requests de todas as issues (UC01–UC12 e correlatas).
- Branches de feature/issue — criadas a partir de `develop` (ex.: `feature/uc04-registrar-venda`), com PR de volta para `develop`.

Promoção de `develop` para `main` é feita por PR (`develop` → `main`); nenhuma outra branch pode ser a origem de um PR para `main` (verificado automaticamente pelo workflow `Guard main branch`).

## Pull Requests

- Nenhum push direto é aceito em `main` ou `develop` — toda mudança entra via PR.
- O PR só pode ser mergeado se o pipeline de CI (`Build and test`) estiver verde.
- PRs para `main` só podem ter `develop` como branch de origem (checado pelo workflow `Guard main branch`).

## Testes

- Toda issue de caso de uso (UC01–UC12) exige testes unitários cobrindo o fluxo principal e os fluxos alternativos/erros descritos na issue, incluindo as exceções de domínio definidas na issue #1 (`BusinessRuleViolationException`, `EntityNotFoundException`, `DuplicateEntityException`).
- O pipeline de CI roda `mvn verify` em todo push/PR para `main` e `develop`.
