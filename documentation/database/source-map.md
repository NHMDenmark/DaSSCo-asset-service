# Source Map

This documentation was reviewed against repository sources and a local metadata-only database check using the default datasource from `application.properties`.

## Source Files

| Concern | Source files |
|---|---|
| Relational schema | `src/main/resources/liquibase/changelog-master.xml`, `changelog-1.0.0.xml`, `changelog-2.0.0.xml`, `changelog-3.0.0.xml` through `changelog-3.13.0.xml` |
| Database diagrams | `documentation/database/database-diagrams.drawio` |
| Development seed data | `src/main/resources/liquibase/changelog-1.0.0-test.xml`, `src/main/resources/sql/1.0.0/*.sql` |
| AGE graph setup/seeds | `src/main/resources/sql/test-data.sql`, `src/main/resources/sql/development-test-data.sql`, `src/main/resources/sql/development-test-data-2.sql`, `src/main/resources/sql/test-data-with-user.sql`, `src/main/resources/sql/test-data-roles.sql`, `src/main/resources/sql/6.0.0/enums.sql` |
| AGE runtime boilerplate | `src/main/java/dk/northtech/dasscoassetservice/repositories/helpers/DBConstants.java` |
| AGE runtime queries | `AssetSyncRepository`, `BulkUpdateRepository`, `AssetRepository`, `QueriesService`, `StatisticsDataRepository`, `InternalStatusRepository` |
| Relational query builder | `QueriesService`, `QueriesRepository`, `QueryItemField` |
| Extendable enum lists | `ExtendableEnumService`, `EnumRepository`, `Lists`, `AssetService`, `SpecimenService`, `PublicationService`, `SpecifyArsSyncService` |
| Data source config | `DataSources.java`, `application.properties` |
| Local/deployment database config | `docker-compose.yaml`, `docker-compose-postgres.yaml`, `docker-compose-app.yaml`, `docker-compose-age-viewer.yaml`, `README.md` |

## Live Metadata Check

The local database reachable through the default application properties was checked for foreign key references to lookup tables and selected column types. This confirmed that:

| Finding | Confirmed behavior |
|---|---|
| `file_format` | No FK references; used as the application-level value list for `asset.file_formats`. |
| `preparation_type` | FK target for `asset_specimen.preparation_type`; `specimen.preparation_types` is not FK-enforced. |
| `asset.payload_type` | Plain `text`; no current lookup table or FK. |
| `saved_query` | Has unique `(username, name)` and no primary key. |
| Share/upload references | `shared_assets.asset_guid`, `asset_change.asset_guid`, `active_large_uploads.asset_guid`, `user_access.username`, and `asset_change.dassco_user_id` are not FK-enforced. |

## Pre-Migration Seed Data Note

`src/main/resources/sql/1.0.0/development-test-assets.sql` is pre-migration seed SQL, not current-schema standalone SQL. It inserts columns later dropped or migrated by subsequent changelogs, such as `asset.initial_metadata_recorded_by` and old specimen preparation fields.
