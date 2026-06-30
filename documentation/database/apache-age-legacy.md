# Apache AGE Legacy Graph

The Apache AGE graph is named `dassco`. It lives inside PostgreSQL through the AGE extension; it is not a separate database server.

The relational PostgreSQL schema is the current source of truth. AGE remains a legacy dependency because some code paths still read or update the graph, but the main Liquibase changelog does not create or maintain the graph.

## Setup and Ownership

| Area | Current behavior |
|---|---|
| Graph creation | `src/main/resources/sql/test-data.sql` runs `SELECT create_graph('dassco')`. The main Liquibase changelog does not. |
| Other AGE seed scripts | Files such as `test-data-with-user.sql`, `test-data-roles.sql`, and `development-test-data-2.sql` assume graph `dassco` already exists. |
| AGE runtime bootstrap | `DBConstants.AGE_BOILERPLATE` creates/loads AGE and sets `search_path = ag_catalog, "$user", public`. `AssetSyncRepository` registers `agtype` and sets only `search_path TO ag_catalog`. |
| Readonly grants | Grants for `ag_catalog` and graph schema `dassco` are commented out in Liquibase. |

## Current Code Usage

| Code area | AGE status |
|---|---|
| `AssetRepository.readAsset(...)` | Reads the asset row relationally, then calls `readEvents_internal(...)`, which reads events from AGE. |
| `QueriesService.getAssetsFromQuery(...)` | Builds relational SQL and reads events from the relational `event` table for query results. |
| `QueriesService.getAssetCountFromQuery(...)` | Still builds graph SQL through `unwrapQuery(...)`, but the current public count endpoint calls `getAssetsFromQuery(...).size()` instead. Treat this count method as legacy/unwired unless direct internal callers are added. |
| `QueriesService.assetSql` and `assetCountSql` | Legacy AGE SQL templates remain in the service. |
| `AssetSyncRepository` | Reads completed assets from AGE and marks graph assets as `synced = true`. |
| `BulkUpdateService` | Current bulk update flow writes relational `asset`, `event`, `asset_funding`, `digitiser_list`, `issue`, and role tables. |
| `BulkUpdateRepository` | Still used by `AssetGroupService` for relational `readMultipleAssets(...)`. Its AGE mutation helpers remain in the file but appear unused by the current service flow. |
| `StatisticsDataRepository.getGraphData(...)` | Despite the method name, current implementation uses relational joins. `testNewSql(...)` still runs an AGE query but appears to be a test/helper path. |
| `InternalStatusRepository` | Contains legacy AGE SQL strings, while active public methods use relational SQL. |

## Diagram

See [database-diagrams.drawio](database-diagrams.drawio), page `AGE Legacy Graph`.

The diagram is a legacy inventory reconstructed from AGE seed SQL and remaining AGE query/update code. It is not a guaranteed current production graph shape.

## Labels and Edges Found

| Type | Names |
|---|---|
| Labels from explicit graph setup | `Institute`, `Event`, `User`, `Pipeline`, `Workstation`, `Collection`, `Asset`, `Specimen` |
| Labels from seed/current AGE query/update code | `Institution`, `Status`, `File_format`, `Internal_status`, `Subject`, `Payload_type`, `Funding_entity`, `Digitiser`, `Role`, `Asset_Group` |
| Edges from explicit graph setup | `INITIATED_BY`, `STATIONED_AT`, `USED_BY`, `CREATED_BY`, `CHANGED_BY`, `CHILD_OF`, `BELONGS_TO`, `IS_PART_OF`, `USED` |
| Edges from seed/update code | `RESTRICTED_TO`, `CONTAINS`, `HAS_ACCESS`, `MADE_BY`, `HAS`, `FUNDS`, `DIGITISED` |

## Property Caveats

AGE asset properties are inconsistent across seed files and runtime code. Remaining AGE SQL references both canonical-looking and legacy names.

| Graph node | Properties found |
|---|---|
| `Asset` | `name`, `asset_guid`, `guid`, `asset_pid`, `pid`, `status`, `internal_status`, `synced`, `asset_locked`, `tags`, `funding`, `subject`, `payload_type`, `file_formats`, `asset_taken_date`, `date_asset_taken`, `date_asset_finalised`, `date_metadata_taken`, `multi_specimen`, `restricted_access`, `error_message`, `error_timestamp` |
| `Specimen` | `name`, `barcode`, `specimen_pid`, `specimen_barcode`, `preparation_type` |
| Lookup-like nodes | Usually only `name`. |

## Known AGE Quirks

| Quirk | Details |
|---|---|
| `Institute` vs `Institution` | `test-data.sql` creates vlabel `Institute`, but seed data and most queries use `Institution`. |
| `name` vs `asset_guid` | Some AGE code matches assets by `name`, while other AGE code returns or filters by `asset_guid`. Seed data contains both `guid` and `name` in places. |
| Scalar properties vs lookup nodes | Seed/read queries mostly use scalar asset properties such as `status`, `funding`, `subject`, `payload_type`, and `file_formats`. Legacy mutation helpers also contain node/edge models such as `Status`, `Subject`, `Payload_type`, `Funding_entity`, `Digitiser`, `HAS`, `FUNDS`, and `DIGITISED`. |
| Relational/graph synchronization | Current create/update/delete flows primarily maintain relational tables. If a graph exists, it may be stale unless legacy sync paths are explicitly run. |
| Lookup duplication | Lookup concepts are duplicated between relational tables and graph nodes, for example `asset_status` vs `Status`, `file_format` vs `File_format`, and `subject` vs `Subject`. |
