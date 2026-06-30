# Database Instances and Schemas

The asset service uses PostgreSQL. The local/deployment database image is expected to include Apache AGE, but AGE is a legacy dependency rather than the current source-of-truth model.

## Database Instances

| Database area | What it is | Current source notes |
|---|---|---|
| Asset service PostgreSQL database | Main application database for ARS assets, specimens, events, file metadata, access, mappings, saved queries, and upload state. | `application.properties` defaults to `jdbc:postgresql://localhost:5433/dassco_file_proxy`. `docker-compose-postgres.yaml` creates `dassco_asset_service` on host port `5434`. README deployment examples mostly use `dassco_asset_service`. |
| Apache AGE graph inside PostgreSQL | Legacy graph model named `dassco`, queried through `ag_catalog.cypher(...)`. | The graph is not created by the main Liquibase changelog. `src/main/resources/sql/test-data.sql` creates it; other AGE seed scripts assume it already exists. |

## Schemas

| Schema | Owner | Purpose |
|---|---|---|
| `public` | Asset service Liquibase | Main relational model: assets, specimens, events, statuses, users, access restrictions, asset groups, file/share/cache/upload tables, saved queries, and Liquibase bookkeeping. |
| `mappings` | Asset service Liquibase | Specify-to-ARS name mapping tables for institutions and collections. |
| `ag_catalog` | Apache AGE extension | AGE functions, types, and metadata, including `cypher`, `create_graph`, and `agtype`. Some graph methods set `search_path = ag_catalog, "$user", public`; `AssetSyncRepository` sets only `ag_catalog`. |
| `dassco` | Apache AGE graph schema | Physical graph label/edge tables created by AGE when `create_graph('dassco')` is run. Application AGE code queries this graph by name. |
| `information_schema`, `pg_catalog`, `pg_toast` | PostgreSQL | System schemas. Liquibase grants the readonly user access to some of these. |

## Liquibase Scope

The main changelog is `src/main/resources/liquibase/changelog-master.xml`. It includes `changelog-0.0.0.xml`, `changelog-1.0.0.xml`, `changelog-1.0.0-test.xml`, `changelog-2.0.0.xml`, and `changelog-3.0.0.xml` through `changelog-3.13.0.xml`.

The `changelog-1.0.0-test.xml` file only loads development test data when the `development` Liquibase context is enabled.

## Readonly User Grants

`changelog-1.0.0.xml` creates and grants the readonly user access to `public`, `information_schema`, `pg_catalog`, and `pg_toast`.

Important caveats:

| Area | Caveat |
|---|---|
| AGE schemas | Grants for `ag_catalog` and the graph schema `dassco` are present but commented out. Readonly AGE paths are therefore fragile unless deployment grants are added outside the changelog. |
| `mappings` schema | `mappings` is created later in `changelog-2.0.0.xml`; no readonly grants for this schema are added by the current changelog. |

## Naming Caveats

The repository still has file-proxy naming remnants:

| Source | Current value |
|---|---|
| `application.properties` | Defaults to database/user/password `dassco_file_proxy` on `localhost:5433`. |
| `docker-compose-app.yaml` | Points `POSTGRES_URL` to `jdbc:postgresql://database:5432/dassco_file_proxy`. |
| `docker-compose-postgres.yaml` | Creates database/user/password `dassco_asset_service` and exposes it on host port `5434`. |
| README deployment examples | Mostly use `dassco_asset_service`. |

Treat `dassco_asset_service` as the current service database name in deployment examples. Treat `dassco_file_proxy` as a legacy/local default unless a specific environment still uses it.
