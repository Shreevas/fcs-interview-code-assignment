# Implementation Notes

This document explains **what was implemented, why**, and **how to build, run and test** the
project. See [CODE_ASSIGNMENT.md](CODE_ASSIGNMENT.md) for the original task list and
[QUESTIONS.md](QUESTIONS.md) for the written design-reasoning answers.

## Contents

- [1. Location](#1-location)
- [2. Store — legacy system sync fix](#2-store--legacy-system-sync-fix)
- [3. Warehouse](#3-warehouse)
- [4. Bonus — Fulfillment (Warehouse ↔ Product ↔ Store)](#4-bonus--fulfillment-warehouse--product--store)
- [5. Cross-cutting: exception handling, logging, error responses](#5-cross-cutting-exception-handling-logging-error-responses)
- [6. Testing strategy](#6-testing-strategy)
- [7. Code coverage (JaCoCo)](#7-code-coverage-jacoco)
- [8. CI/CD and health checks](#8-cicd-and-health-checks)
- [9. How to run](#9-how-to-run)
- [10. How to run the tests / check coverage](#10-how-to-run-the-tests--check-coverage)
- [11. Known local-environment caveats](#11-known-local-environment-caveats)

---

## 1. Location

**File:** `src/main/java/com/fulfilment/application/monolith/location/LocationGateway.java`

`resolveByIdentifier` was a stub. It now does a linear search of the static in-memory `Location`
list and returns the match, or `null` if the identifier isn't known. Callers (the Warehouse use
cases) treat `null` as "invalid location". The class is now `@ApplicationScoped` so it can be
injected as a `LocationResolver` — it wasn't a CDI bean before, so nothing could actually use it.

## 2. Store — legacy system sync fix

**Files:** `stores/StoreResource.java`, new package `stores/events/`.

**Problem:** `StoreResource`'s create/update/patch methods called `LegacyStoreManagerGateway`
directly, inline, inside the `@Transactional` method body. A JTA transaction only actually commits
when the `@Transactional`-intercepted method *returns*, so the legacy system could receive a
"Store created" notification for a Store whose insert hadn't committed yet (or that could still
fail to commit).

**Fix:** the legacy call was replaced with a CDI event fired *after* the entity is
persisted/updated, consumed by a listener bound to `@Observes(during = TransactionPhase.AFTER_SUCCESS)`:

```
StoreResource.create()/update()/patch()
  → store.persist() / entity mutation
  → storeChangedEvent.fire(new StoreChangedEvent(...))   // queued, not executed yet
  → method returns → transaction commits
  → StoreLegacySyncListener.onStoreChanged(...) runs     // only now, and only if commit succeeded
  → calls LegacyStoreManagerGateway
```

This guarantees the legacy system only ever sees data that is durably committed. If the legacy call
itself fails, the listener logs and swallows the error rather than propagating it — the DB commit
already happened by that point and can't be undone, so surfacing a 500 to the original caller would
be misleading (the Store *was* created successfully; only the downstream sync failed).

Two latent bugs in `patch()` were fixed along the way:
- the partial-update conditions were inverted (`if (entity.name != null)` instead of
  `if (updatedStore.name != null)` — i.e. it checked the *existing* value instead of the
  *incoming* one, which meant omitting `quantityProductsInStock` from a PATCH body could silently
  reset it to `0`).
- the legacy sync payload used the raw, unpersisted request body (`updatedStore`) instead of the
  merged, persisted entity.

## 3. Warehouse

**Files:** `warehouses/domain/**`, `warehouses/adapters/**`.

### Validation rules implemented

| Rule | Where |
|---|---|
| Business unit code must not already be active | `CreateWarehouseUseCase` |
| Location must exist | `WarehouseFeasibilityValidator.requireExistingLocation` |
| Max warehouses per location not exceeded | `WarehouseFeasibilityValidator.requireWarehouseCountFeasible` |
| Total active capacity at location within `Location.maxCapacity` | `WarehouseFeasibilityValidator.requireCapacityFeasible` |
| Stock must not exceed the warehouse's own capacity | `WarehouseFeasibilityValidator.requireStockWithinCapacity` |
| **Replace only:** new capacity ≥ old warehouse's stock | `ReplaceWarehouseUseCase` |
| **Replace only:** new stock == old warehouse's stock | `ReplaceWarehouseUseCase` |
| **Replace only:** location cannot change | `ReplaceWarehouseUseCase` |

`WarehouseFeasibilityValidator` is a new shared class so Create and Replace don't duplicate the
location/capacity/count checks.

### Replace = archive + create, not update

Per `BRIEFING.md`, replacing a Warehouse archives the current one and creates a new one **that
reuses the same Business Unit Code**, to preserve history. The original `ReplaceWarehouseUseCase`
stub called `warehouseStore.update(...)`, which would have overwritten the existing row in place —
that loses history and contradicts the brief. It now calls `warehouseStore.remove(old)` (soft-archive
the existing row) followed by `warehouseStore.create(newWarehouse)` (a brand-new row, new database
id, same business unit code). Same reasoning applied to `ArchiveWarehouseUseCase`, whose stub also
incorrectly called `update(...)` instead of `remove(...)`.

Because a Business Unit Code is *reused* across an archived + active row over time, it is **not**
a unique key in the database — `WarehouseRepository` always filters `archivedAt IS NULL` when
looking up "the" warehouse for a given code.

### Exceptions

New package `warehouses/domain/exceptions`: `WarehouseNotFoundException` (→ HTTP 404) and
`WarehouseValidationException` (→ HTTP 400), both deliberately free of any JAX-RS import so the
domain/use-case layer stays framework-agnostic. `WarehouseExceptionMapper` (in the `adapters.restapi`
package) does the translation to an HTTP response.

### REST layer

`WarehouseResourceImpl` now injects the three use cases and calls them from
`createANewWarehouseUnit` / `archiveAWarehouseUnitByID` / `replaceTheCurrentActiveWarehouse`
(previously all `throw new UnsupportedOperationException`), each wrapped in `@Transactional`. A
`Long id` field was added to the domain `Warehouse` model (it didn't have one) so the REST response
can include the database-generated id, matching the OpenAPI schema.

> **Note:** the OpenAPI-generated `WarehouseResource` interface declares `createANewWarehouseUnit`
> to return a plain `Warehouse`, not a `jakarta.ws.rs.core.Response`. JAX-RS defaults a plain
> return value to HTTP `200`, so `POST /warehouse` returns `200` rather than the `201` documented
> in `warehouse-openapi.yaml`. Fixing that would require adding a response-wrapper vendor
> extension to the spec — noted as a known, low-priority gap rather than worked around.

## 4. Bonus — Fulfillment (Warehouse ↔ Product ↔ Store)

**New package:** `com.fulfilment.application.monolith.fulfillment`.

A new `Fulfillment` entity associates a Product, a Store and a Warehouse. `FulfillmentService`
enforces the three constraints from the assignment before persisting:

1. A Product can be fulfilled by at most **2** distinct Warehouses per Store.
2. A Store can be fulfilled by at most **3** distinct Warehouses (across all products).
3. A Warehouse can hold at most **5** distinct Product types.

Exposed as hand-written JAX-RS (`FulfillmentResource`, `POST/GET/DELETE /fulfillment`) rather than
OpenAPI-generated, since there's no pre-existing external contract to honor for this feature (see
[QUESTIONS.md](QUESTIONS.md) Q2 for the reasoning on when to pick one approach over the other).

**Design choice worth flagging:** associations are keyed on the Warehouse's **database id**, not
its Business Unit Code. Since a "replace" creates a brand-new row under the same code, keying on
the code would silently reattach a Store's fulfillment history to whatever warehouse currently
holds that code. Keying on the id means a new association must be created explicitly against the
new warehouse after a replace — this is an assumption, not something the assignment specifies.

**Known limitation:** the three constraint checks are "count, then insert" inside one transaction.
A database unique constraint on `(productId, storeId, warehouseId)` prevents exact duplicate races,
but two concurrent requests could theoretically both pass a count check before either commits (e.g.
both see "1 of 2 warehouses used" and both insert a 2nd). Accepted as an explicit, documented
limitation for this assignment's scope; a production system would need pessimistic locking or a
serializable transaction for full correctness under concurrency.

## 5. Cross-cutting: exception handling, logging, error responses

- `StoreResource` and `ProductResource` each had a byte-for-byte identical nested `ErrorMapper`
  class. Extracted into `common/web/ErrorResponseFactory` (builds the JSON error body) and
  `common/web/GenericExceptionMapper` (the catch-all `ExceptionMapper<Exception>`), so all four
  resources (Store, Product, Warehouse, Fulfillment) now produce an identical error JSON shape.
  Warehouse/Fulfillment additionally register their own more-specific mappers
  (`WarehouseExceptionMapper`, `FulfillmentExceptionMapper`) for their domain exceptions — JAX-RS
  resolves the most-specific mapper per exception type automatically, so both can coexist safely.
- Logging uses `org.jboss.logging.Logger` throughout (matching the existing house style). Use
  cases log `INFO` on a successful mutation and `WARN` when rejecting a request for an expected
  business-rule reason; `ERROR` is reserved for genuinely unexpected failures (the generic
  catch-all mapper, and the swallowed legacy-sync failure in `StoreLegacySyncListener`).

## 6. Testing strategy

98 tests total, split by what they need:

- **Plain JUnit5 + Mockito** (no container, milliseconds each) for anything with real business
  logic and no direct DB dependency: `LocationGatewayTest`, `CreateWarehouseUseCaseTest`,
  `ReplaceWarehouseUseCaseTest`, `ArchiveWarehouseUseCaseTest`, `WarehouseFeasibilityValidatorTest`,
  `FulfillmentServiceTest`, `StoreLegacySyncListenerTest`, and the exception-mapper tests. Each
  covers one positive path plus one test per validation rule (duplicate code, invalid location,
  max count reached, capacity exceeded, stock mismatch, etc.) and boundary cases (Nth allowed,
  N+1th rejected).
- **`@QuarkusTest` + rest-assured** for things that need the real container/DB: full REST-surface
  tests for Store, Warehouse and Fulfillment (`StoreResourceTest`, `WarehouseResourceTest`,
  `FulfillmentResourceTest`), and `WarehouseRepositoryTest` for the Panache query logic
  (`@TestTransaction` auto-rolls back each test so the seed data stays stable).
- **One deliberately targeted regression test** for the trickiest fix in this assignment:
  `StoreEventTransactionalityTest` proves the legacy gateway is **never** called when the owning
  transaction rolls back, using a small `TransactionalEventFirer` test helper that fires the event
  then throws.

The original `WarehouseEndpointIT` (`@QuarkusIntegrationTest`, `*IT.java` naming) was rewritten as
`WarehouseResourceTest` (`@QuarkusTest`). `*IT.java` files are not picked up by the default
Surefire configuration in this project (Failsafe, which would run them, is only wired up under the
`native` Maven profile) — so that test never actually executed under `./mvnw test` before.
`@QuarkusTest` runs in-process and is also required for JaCoCo to see the code it exercises (see
next section).

## 7. Code coverage (JaCoCo)

`pom.xml` wires `jacoco-maven-plugin` into the build:
- `prepare-agent` — attaches the coverage agent to the test JVM.
- `report` (bound to the `test` phase) — produces the HTML report at `target/site/jacoco/index.html`.
- `check` (bound to the `verify` phase) — **fails the build** if instruction or line coverage
  drops below **80%**. Run `./mvnw verify` (not just `test`) to enforce this.

The generated OpenAPI package (`com/warehouse/api/**`) is excluded from the coverage count, since
it's generated code, not hand-written.

`io.quarkus:quarkus-jacoco` is also required (not just the Maven plugin). Without it, any class
only exercised through a `@QuarkusTest` (i.e. through Quarkus's own test classloader) showed up as
0% covered even though it was clearly being exercised — the plain JaCoCo Java agent can't
instrument classes loaded that way. Adding the extension raised measured coverage from ~54% to the
actual ~96%.

**Current achieved coverage:** ~96% instructions / ~95% lines (well above the 80% gate).

## 8. CI/CD and health checks

- `.github/workflows/ci.yml` — runs `./mvnw -B verify` on every push/PR to `main` (and
  `CODE/**` branches), then uploads the JaCoCo report and Surefire reports as build artifacts.
  GitHub's `ubuntu-latest` runners have Docker preinstalled, so Quarkus Dev Services (Testcontainers
  Postgres) works there without extra configuration.
- `io.quarkus:quarkus-smallrye-health` was added, exposing `/q/health`, `/q/health/live` and
  `/q/health/ready`. Since a JDBC datasource is already configured, Quarkus auto-registers a
  database readiness check with no extra code.

## 9. How to run

**Prerequisites:** JDK 17+, and either a local PostgreSQL instance or Docker (used automatically by
Quarkus Dev Services in dev/test mode).

```sh
cd java-assignment

# Build
./mvnw package

# Run in dev mode (live reload, auto-starts a Postgres container via Dev Services)
./mvnw quarkus:dev

# Or run the packaged jar against a manually-started Postgres:
docker run -it --rm --name quarkus_test \
  -e POSTGRES_USER=quarkus_test -e POSTGRES_PASSWORD=quarkus_test -e POSTGRES_DB=quarkus_test \
  -p 15432:5432 postgres:13.3
java -jar target/quarkus-app/quarkus-run.jar
```

Once running, some quick manual checks:

```sh
curl http://localhost:8080/q/health
curl http://localhost:8080/warehouse
curl -X POST -H "Content-Type: application/json" \
  -d '{"businessUnitCode":"MWH.100","location":"HELMOND-001","capacity":30,"stock":10}' \
  http://localhost:8080/warehouse
curl -X POST -H "Content-Type: application/json" \
  -d '{"productId":1,"storeId":1,"warehouseId":2}' \
  http://localhost:8080/fulfillment
```

## 10. How to run the tests / check coverage

```sh
cd java-assignment

./mvnw test      # runs all 98 tests
./mvnw verify    # runs all tests AND enforces the 80% JaCoCo coverage gate
```

After `verify`, open `target/site/jacoco/index.html` in a browser for the full per-class coverage
breakdown.

## 11. Known local-environment caveats

These are environment quirks discovered while implementing this on a specific local machine — they
should **not** affect a normal CI runner (like the GitHub Actions workflow above) or most other
setups, but are worth knowing if `./mvnw test`/`verify` misbehaves locally:

- **Wrong JDK picked up by the Maven wrapper.** If `JAVA_HOME` isn't set, some systems resolve a
  different (often newer/wrong) JDK than the one on `PATH`. If you see errors like
  `Java 26 (70) is not supported by the current version of Byte Buddy` or similar bytecode-version
  errors during test augmentation, explicitly export `JAVA_HOME` to a JDK 17 install before running
  `./mvnw`.
- **Docker Desktop / Testcontainers incompatibility.** If Dev Services fails to start Postgres with
  an error like `Previous attempts to find a Docker environment failed`, even though `docker info`
  works fine from the shell, it's usually a version mismatch between the bundled Testcontainers
  client and a very new Docker Desktop release. Workaround: start Postgres manually and point tests
  at it directly, bypassing Dev Services:

  ```sh
  docker run -d --name pg_test -e POSTGRES_USER=quarkus_test -e POSTGRES_PASSWORD=quarkus_test \
    -e POSTGRES_DB=quarkus_test -p 15432:5432 postgres:13.3

  ./mvnw verify \
    -Dquarkus.datasource.db-kind=postgresql \
    -Dquarkus.datasource.username=quarkus_test \
    -Dquarkus.datasource.password=quarkus_test \
    -Dquarkus.datasource.jdbc.url=jdbc:postgresql://localhost:15432/quarkus_test
  ```

  This was used to verify the full test suite (98/98 passing, coverage gate passing) during
  development; it does not require any change to the committed `application.properties`.