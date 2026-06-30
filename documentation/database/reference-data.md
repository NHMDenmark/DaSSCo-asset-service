# Reference Data

This file lists reference values seeded by the main Liquibase changelogs and known mismatches between database lookup values and Java enums.

## Seeded Values

These values are seeded by default Liquibase changelogs.

| Table/type | Values |
|---|---|
| PostgreSQL enum `access_type` | `READ`, `WRITE`, `ADMIN` |
| `internal_asset_status` | `METADATA_RECEIVED`, `ASSET_RECEIVED`, `COMPLETED`, `ERDA_FAILED`, `ERDA_ERROR`, `SHARE_REOPENED`, `ERDA_SYNCHRONISED`, `SPECIFY_SYNC_SCHEDULED`, `SPECIFY_SYNC_FAILED`, `SPECIFY_SYNCHRONISED` |
| `asset_status` | `WORKING_COPY`, `ARCHIVE`, `BEING_PROCESSED`, `PROCESSING_HALTED`, `ISSUE_WITH_MEDIA`, `ISSUE_WITH_METADATA`, `FOR_DELETION` |
| `workstation_status` | `IN_SERVICE`, `OUT_OF_SERVICE` |
| `preparation_type` | `slide`, `pinning` |
| `subject` | `folder`, `specimen`, `label` |
| `event_type` | `CREATE_ASSET`, `UPDATE_ASSET`, `AUDIT_ASSET`, `DELETE_ASSET`, `CREATE_ASSET_METADATA`, `UPDATE_ASSET_METADATA`, `BULK_UPDATE_ASSET_METADATA`, `AUDIT_ASSET_METADATA`, `DELETE_ASSET_METADATA`, `METADATA_TAKEN`, `ASSET_FINALISED`, `SYNC_STORAGE`, `SYNCHRONISE_SPECIFY` |
| `file_format` | `TIF`, `JPEG`, `RAW`, `RAF`, `CR3`, `DNG`, `TXT` |
| `file_sync_status` | `NEW_FILE`, `SYNCHRONIZED` |

## Not Seeded by the Main Default Changelog

| Table | Notes |
|---|---|
| `role` | Application-maintained role catalog. Services can add roles before assigning restrictions. AGE seed scripts create separate graph `Role` nodes, but those are not relational seed data. |
| `issue_category` | Extendable enum-list table. No default values found in the main changelog. |
| `publisher` | Extendable enum-list table. No default values found in the main changelog after the `3.2.0` publisher model replacement. |
| `funding` | Data table/lookup. No default values found in the main changelog. |

## Java Enum Mismatches

| Area | Mismatch |
|---|---|
| `InternalStatus` vs `internal_asset_status` | Java enum `InternalStatus` does not include every lookup row. `ERDA_ERROR` is retained as a legacy database value. `SHARE_REOPENED` is inserted and selected by `InternalStatusRepository.IN_PROGRESS_SQL`, but is not present in the Java enum at the time this doc was updated. |
| `ERDA_ERROR` | Migration `3.4.0` updates existing assets from `ERDA_ERROR` to `ERDA_FAILED`, but does not remove the lookup row. Current `setAssetStatus` parses the Java enum and rejects `ERDA_ERROR`. Treat `ERDA_ERROR` as legacy-only. |
| `DasscoEvent` vs `event_type` | Java enum `DasscoEvent` includes `ASSET_SYNCED`, but the main Liquibase seed for `event_type` does not insert it. |

## Extendable Lists

The application has list endpoints and/or service paths for maintaining several lookup-backed values. See [Lookup values and constraints](lookup-values-and-constraints.md) for which values are database-enforced and which are application-validated.
