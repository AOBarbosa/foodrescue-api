# Architecture

Foundation laid by issue #1. This document describes the conventions every
UC issue (#2–#13) builds on — read it before adding a new entity, service,
repository or endpoint.

> **Phase 2 (in progress):** the code base is being refactored into the
> **PerishRescue Framework**, with FoodRescue as its first instance. The
> module layout below reflects step E1 (modules and packages only, no
> behavior change); the kernel abstractions (hot spots, lifecycle state
> machine) land in the next steps.

## Modules and what lives where

```
framework/            the PerishRescue kernel (no instance-specific code may import from instances/)
  perishrescue-core
  perishrescue-domain
  perishrescue-persistence
  perishrescue-business
  perishrescue-rest
instances/
  foodrescue          instance 1: the runnable Spring Boot application
```

| Module | Package | Contains |
|---|---|---|
| `perishrescue-core` | `br.com.seudominio.perishrescue.core` | Framework-light building blocks with no domain knowledge: `Money`/`Percentage` value objects, `TimeProvider`, `MessageUtils` (i18n), the `core.validation` framework (`Validator<T>`, `AbstractValidator<T>`, `BusinessOperation`, `ValidationError`, `ValidationException`). |
| `perishrescue-domain` | `br.com.seudominio.perishrescue.domain` | JPA entities (`entities`), their builders (`builders`), enums (`enums`), DTOs (`dtos`), entity↔DTO mappers (`mappers`), JPA converters for `core` value objects (`converter`), the domain exception hierarchy (`exception`). |
| `perishrescue-persistence` | `br.com.seudominio.perishrescue.persistence` | `GenericRepository<T>` and one concrete Spring Data repository per aggregate (`repositories`), Liquibase changelogs. |
| `perishrescue-business` | `br.com.seudominio.perishrescue.business` | `GenericService<E,D>` and one concrete service per aggregate (`services`), business validators (`validation`), cross-cutting error codes (`helpers.MessageCode`), shared beans like `PasswordEncoder` (`config`). |
| `perishrescue-rest` | `br.com.seudominio.perishrescue.rest` | Controllers, the `ApiResponse`/`ApiError`/`ApiSubError` response envelope (`dtos`), JWT security (`security`), the global exception handler (`exception`), Swagger config (`config`), and `@EnablePerishRescue` (`boot`). A plain library jar: it is not runnable on its own. |
| `foodrescue` | `br.com.seudominio.foodrescue` | `FoodRescueApplication` (`@SpringBootApplication` + `@EnablePerishRescue`), the `FoodItem` resource type (`domain`, `dtos`, `mappers`, `rest`), its Liquibase changelog, `application.properties`, the executable jar. |

Dependency direction: `foodrescue → rest → business → persistence → domain → core`.
A module never depends on one above it in this list.

### How an instance plugs the framework in

The instance's main class carries `@EnablePerishRescue`, which imports
`PerishRescueConfiguration`. That class component-scans
`br.com.seudominio.perishrescue`, registers the same package as an
auto-configuration package (so Spring Boot finds the framework's JPA entities
and repositories **in addition to** the instance's own package) and enables
scheduling. Don't add `@EntityScan`/`@EnableJpaRepositories` to an instance:
they replace the scanned packages instead of adding to them.
`FoodRescueApplicationTest` boots the whole context against H2 and fails if
this wiring breaks.

### Universal actors (frozen spot)

`Offerer` (the establishment: CNPJ, address, free-text `category`, daily
`openingTime`/`closingTime`) and `Beneficiary` (who reserves and rescues:
CPF in `document`, unique and check-digit validated) are kernel entities used
as-is by every instance. JWT roles are `OFFERER` and `BENEFICIARY`; routes are
`/offerers` and `/beneficiaries`.

### Hot spot 1: the perishable resource

`PerishableResource` is an **abstract** entity (`@Inheritance(JOINED)`, table
`resources`) with what every perishable item shares. An instance adds one
concrete subclass per kind of item, with:

1. **Entity**: `extends PerishableResource`, own table, own attributes, and
   `criticalDeadline(LocalDateTime now)` (the end of the item's useful life).
   A static `builder()` returning `new ResourceBuilder<>(MyItem::new)` gives
   it the common fluent builder.
2. **DTOs**: a registration request and a response.
3. **Mapper**: a `@Component` implementing `ResourceTypeMapper<R, Q, D>`.
4. **Controller**: a `@RestController` with its own `@RequestMapping` that
   `extends AbstractResourceController<R, Q, D>` and only calls `super(...)`.
   Register/list/find/inventory, authorization and validation are inherited.
5. **Changelog**: the subclass table (PK = FK to `resources(id)`) and its
   `_aud` table (no `revtype`; FK `(rev, id)` → `resources_aud`).

`FoodItem` / `FoodItemController` in the FoodRescue instance are the
reference implementation. The kernel's own tests use
`domain.testsupport.TestResource`, published by `perishrescue-domain` as a
test-jar, so the framework is tested without any instance.

### Liquibase

The kernel ships `db/changelog/perishrescue-master.xml`. An instance points
`spring.liquibase.change-log` at its own master, which includes the kernel
master first and then its own changesets (FoodRescue:
`db/changelog/foodrescue-master.xml`).

## Entities

Every entity extends `domain.entities.AbstractEntity`, which provides:

- `Long id` — **not** declared in `AbstractEntity` itself (each concrete
  entity declares its own `@Id` field with its own `@SequenceGenerator`,
  since the sequence name differs per entity), but `getId()`/`setId()` are
  abstract on `AbstractEntity` so generic code (`GenericRepository`,
  `GenericService`) can work with any entity type.
- `creationDate` / `modificationDate` — set automatically via `@PrePersist`/
  `@PreUpdate`. Don't add your own `createdAt`-style field; use these.
- `active` (`Boolean`, defaults `true`) — the soft-delete flag `GenericRepository`
  filters on. `deleteById`/`delete` flip it to `false` instead of issuing a
  real `DELETE`.
- `equals`/`hashCode` based on `getId()` — don't override per entity.

Each entity is `@Audited` (Hibernate Envers): every insert/update/delete is
recorded in a `<table>_aud` row linked to `revinfo`. If you add a new
`@Audited` entity, you must also add its `_aud` table and sequence to a
Liquibase changeset — Envers does **not** create it automatically under
`ddl-auto=validate` (see `framework/perishrescue-persistence/src/main/resources/db/changelog/changes/0010-*`
for the pattern, and the commit history for how the exact column set was
derived from a real Hibernate schema dump rather than guessed).

Entities are plain JavaBeans (getters + setters) — they do **not** validate
their own invariants. That's a deliberate split from earlier drafts of this
project: structural/format checks belong to a `BusinessValidator` (see
below), not to the entity or its builder.

## Builders

Each entity has a companion builder in `domain.builders` (e.g.
`ConsumerBuilder` for `Consumer`), a plain fluent assembler with **no
validation** — it only calls setters. `Entity.builder()` is a static factory
delegating to it. When a field needs a sensible default (e.g. `Product`'s
`currentPrice` defaulting to `originalPrice`), that default lives in the
builder's `build()` method as a plain conditional, not as thrown validation.

## DTOs and mappers

- DTOs live in `domain.dtos`, one per aggregate (e.g. `ConsumerDTO`), plus
  standalone request/response shapes when an operation doesn't map 1:1 to
  an entity (e.g. `LoginRequest`). Bean-validation annotations
  (`@NotBlank`, `@Email`, …) go directly on the DTO's record components —
  that's what `@Valid @RequestBody` in a controller validates.
- A DTO never carries a secret back out. `ConsumerDTO.password()` carries
  the *raw* password on the way in (register); `ConsumerMapper.toDto()`
  always sets it to `null` on the way out. Follow this pattern for any
  future secret-bearing field.
- Mappers live in `domain.mappers`, implement `DTOMapper<E, D>`
  (`toEntity`/`toDto`), and are hand-written `@Component`s — no MapStruct.

## Repositories

`persistence.repositories.GenericRepository<T extends AbstractEntity>`
extends Spring Data's `JpaRepository<T, Long>` and overrides the read/delete
methods to respect the `active` soft-delete flag (`findAll`, `findById`,
`deleteById`, `delete`, `deleteAll`), plus adds `findByIdAndActive`,
`findAllActiveAndInactive`, `reactive`, `countActive`.

A concrete repository (e.g. `ConsumerRepository`) is a one-liner:

```java
@Repository
public interface ConsumerRepository extends GenericRepository<Consumer> {
    Optional<Consumer> findByEmail(String email); // add only what you actually use
}
```

Don't pre-declare finder methods nobody calls yet — add them when the
service that needs them is written.

## Services

`business.services.GenericService<E, D>` wraps a `GenericRepository<E>` +
`DTOMapper<E, D>` + a bean `Validator` + `MessageUtils`, giving
`save`/`update`/`saveAll`/`delete`/`deleteById`/`findById`/
`findByIdAndActive`/`findAll` (with and without paging)/`reactive` for free.

A concrete service (e.g. `ConsumerService`) extends it, injects the
concrete repository/mapper/business validator it needs, and **overrides**
`save`/`update`/etc. whenever the operation needs more than "map DTO →
validate → persist" — see `ConsumerService.save()` for the pattern (hash
the password, then delegate to the validator, then persist) and
`ConsumerService.login()` for a bespoke method that isn't part of the
generic CRUD surface at all.

## Business validators

`business.validation.BusinessValidator<T extends AbstractEntity>` extends
`core.validation.Validator<T>`, adding `validateOperation(T entity, BusinessOperation operation)`.
A concrete validator (e.g. `ConsumerBusinessValidator`) extends
`core.validation.AbstractValidator<T>`:

- `doValidate(entity)` runs the bean validator over the entity and turns
  every `ConstraintViolation` into an `addError(...)` call, then runs
  whatever cross-entity checks need a repository (e.g. email uniqueness).
- `validate(entity)` (inherited) and `validateOperation(entity, operation)`
  both clear accumulated errors, call `doValidate`, and throw a
  `core.validation.exception.ValidationException` carrying every error at
  once if any were recorded.
- Uniqueness checks that exclude the entity's own row use
  `Objects.equals(existing.getId(), entity.getId())` — **never**
  `existing.getId().equals(...)` directly, which throws `NullPointerException`
  for an entity that hasn't been persisted yet (`id == null`).

## REST layer

- **Response envelope**: every endpoint returns
  `ApiResponse<T>(T data, String message, boolean success, MessageCode code)`.
  On failure, `data` is an `ApiError` (`status`, `message`, `messageCode`,
  `subErrors`, `timestamp`).
- **Errors**: `rest.exception.RestExceptionHandler` (`@RestControllerAdvice`)
  maps every exception type in play to this shape — the domain hierarchy
  (`EntityNotFoundException`→404, `DuplicateEntityException`→409,
  `BusinessRuleViolationException`→422), `ValidationException`→422 (with
  one `ApiSubError` per failed rule), bean-validation failures (`@Valid` →
  `MethodArgumentNotValidException`→400, thrown directly →
  `ConstraintViolationException`→400), `ObjectOptimisticLockingFailureException`→409,
  and the generic 401/403/500 fallbacks. Add a handler here for any new
  exception type a use case introduces — never `try/catch` in a controller.
- **Security**: stateless JWT only (`rest.security`). `JwtAuthenticationFilter`
  populates the `SecurityContext` from a valid `Authorization: Bearer <token>`;
  `SecurityConfig` `permitAll()`s registration/login/Swagger routes and
  requires authentication everywhere else; per-route role checks
  (`hasRole("ESTABLISHMENT")`/`hasRole("CONSUMER")`) belong on each
  controller method via `@PreAuthorize` (`@EnableMethodSecurity` is on),
  not centralized in `SecurityConfig`. `RestAuthenticationEntryPoint`/
  `RestAccessDeniedHandler` write the 401/403 JSON directly — they run at
  the filter level, before Spring MVC, so they build their **own**
  `ObjectMapper` (with `JavaTimeModule` registered and
  `WRITE_DATES_AS_TIMESTAMPS` disabled to match the rest of the API) rather
  than injecting one — no `ObjectMapper` bean is available for injection in
  this Spring Boot version.
- **Documentation**: every controller/DTO carries `@Tag`/`@Operation`/
  bean-validation annotations; Swagger UI authenticates via the `bearerAuth`
  JWT scheme configured in `rest.config.OpenApiConfig`.
- **Locale**: `FoodRescueApplication.main()` (in the instance) forces `Locale.setDefault(Locale.US)`
  so bean-validation messages stay in English regardless of the host
  machine's locale — don't remove it.

## How to add a new use case

1. **Entity** (if new): add it under `domain.entities` extending
   `AbstractEntity`, with its own `@Id`/`@SequenceGenerator`, `@Audited`.
   Add its builder under `domain.builders`.
2. **Migration**: add a Liquibase changeset for the table + sequence, and
   another for its `_aud` table (see §Entities above).
3. **Repository**: add a `GenericRepository<Entity>` subinterface under
   `persistence.repositories`, with only the finder methods you need now.
4. **DTO + mapper**: add the DTO under `domain.dtos` (bean-validation
   annotations on its fields) and the mapper under `domain.mappers`.
5. **Business validator** (if the entity has real invariants beyond bean
   validation, e.g. uniqueness): add it under `business.validation.validators`.
6. **Service**: add it under `business.services`, extending `GenericService`,
   overriding whatever operations need custom logic.
7. **Controller**: add it under `rest.controller`, with Swagger annotations
   and `@PreAuthorize` where a role check applies.
8. **Exceptions**: if a new failure mode doesn't map to an existing
   exception, add the exception (reuse the `domain.exception` hierarchy
   where it fits) and a handler in `RestExceptionHandler`.
9. **Tests**: unit-test the business validator and service (mock the
   repository), and the controller's new exception mappings if you added any.
