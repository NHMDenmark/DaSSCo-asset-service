# Lookup Values and Constraints

This file explains the difference between intentional lookup-backed values and true soft references.

The asset service uses several single-column tables as extendable enum lists. Some are FK targets. Others back array columns and are validated in application code because PostgreSQL cannot directly FK each element of a `text[]` column to a lookup table.

## Lookup-Backed Values

| List table | Used for | Database-enforced usages | Application-enforced or non-FK usages | Notes |
|---|---|---|---|---|
| `file_format` | Valid asset file formats. | None. | `asset.file_formats` is `text[]`; `AssetService` validates every value against `file_format`. `SpecifyArsSyncService` can add new formats before creating assets from Specify messages. | Managed by `ExtendableEnumService` and `/v1/lists/fileformats`. |
| `preparation_type` | Valid specimen/asset-specimen preparation types. | `asset_specimen.preparation_type`. | `specimen.preparation_types` is `text[]`; `SpecimenService` validates every value against `preparation_type`. | Managed by `ExtendableEnumService` and `/v1/lists/preparationtypes`. |
| `asset_status` | Valid public/workflow asset statuses. | `asset.status`. | `AssetService` validates asset status against `asset_status`; `SpecifyArsSyncService` can add missing statuses before creating assets from Specify messages. | Managed by `ExtendableEnumService` and `/v1/lists/statuses` for listing. |
| `subject` | Valid asset subjects. | `asset.subject`. | Asset update code can add missing subjects. | Managed by `ExtendableEnumService`; no public list mutation endpoint was found for subjects. |
| `issue_category` | Valid issue categories. | `issue.category`. | `AssetService.validateIssue` checks that the category exists before create/update flows. | Managed by `ExtendableEnumService` and `/v1/lists/issuecategories`. |
| `publisher` | Valid external publishers. | `asset_publisher.publisher`. | `PublicationService` and asset validation check publishers before publication rows are written. | Managed by `ExtendableEnumService` and `/v1/lists/externalpublishers`. |
| `role` | Valid access-control role names. | Institution, collection, specimen, and asset role restriction tables. | Services create roles before assigning restrictions when necessary. | Application-maintained catalog, not part of `ExtendableEnumService`. |
| `internal_asset_status` | Internal upload/sync lifecycle states. | `asset.internal_status`. | Java enum and database values are not perfectly aligned. | Seeded by Liquibase; not an extendable list endpoint. |
| `event_type` | Valid event names. | `event.event`. | Java enum includes `ASSET_SYNCED`, which is not seeded in `event_type`. | Seeded by Liquibase; not an extendable list endpoint. |
| `file_sync_status` | Valid file sync states. | `file.sync_status`. | None found. | Seeded by Liquibase. |
| `workstation_status` | Valid workstation states. | `workstation.workstation_status`. | None found. | Seeded by Liquibase. |

## Plain Text Without Current Lookup Table

| Column | Current behavior |
|---|---|
| `asset.payload_type` | Plain `text`. The old `payload_type` lookup table is commented out in Liquibase. `PayloadTypeCache` caches values in memory, but no current table or FK enforces payload type values. |

## True Soft References

These columns look like relational references but are not FK-enforced by Liquibase.

| Column | Notes |
|---|---|
| `shared_assets.asset_guid` | Records the shared asset identifier for a directory/share. Not FK-enforced to `asset`. |
| `asset_change.asset_guid` | Records the asset identifier for a directory-scoped change. Not FK-enforced to `asset`. |
| `active_large_uploads.asset_guid` | Indexed for lookup, but not FK-enforced to `asset`, allowing upload state to exist independently of asset-row lifecycle. |
| `user_access.username` | Records share access username. Not FK-enforced to `dassco_user.username`. |
| `asset_change.dassco_user_id` | Records the acting user ID. Not FK-enforced to `dassco_user`. |

## Duplicate Rules

Most join and restriction tables use surrogate primary keys plus FKs only. Unless listed otherwise, duplicate logical relationships are not prevented by database constraints.

| Table | Duplicate note |
|---|---|
| `parent_child` | No unique constraint prevents duplicate parent/child edges. |
| `asset_funding` | No unique constraint prevents duplicate asset/funding pairs. |
| `digitiser_list` | No unique constraint prevents duplicate user/asset rows. |
| `collection_role_restriction` | No unique constraint prevents duplicate collection/role restrictions. |
| `institution_role_restriction` | No unique constraint prevents duplicate institution/role restrictions. |
| `specimen_role_restriction` | No unique constraint prevents duplicate specimen/role restrictions. |
| `asset_role_restriction` | No unique constraint prevents duplicate asset/role restrictions. |
| `asset_group_access` | No unique constraint prevents duplicate group/user access rows. |
| `asset_group_asset` | No unique constraint prevents duplicate group/asset membership rows. |

Service code often avoids duplicates for specific paths with `WHERE NOT EXISTS` or by clearing and replacing restrictions, but those rules are not global database constraints.

## Constraints and Indexes Worth Noting

| Constraint/index | Notes |
|---|---|
| `specimen(collection_id, barcode)` | Unique. |
| `saved_query(username, name)` | Unique. `saved_query.username` cascades on user delete. `saved_query` has no primary key. |
| `parked_file.path` | Unique. |
| `file.path` | Indexed as `path_idx`. |
| `file_cache.expiration_datetime` | Indexed as `file_cache_expiration_datetime_idx`. |
| `event.bulk_update_uuid` | Indexed as `idx_event_bulk_update_uuid`. |
| `active_large_uploads.directory_id` | Indexed as `idx_active_large_uploads_directory`. |
| `active_large_uploads.asset_guid` | Indexed as `idx_active_large_uploads_asset_guid`; no FK to `asset`. |
| Asset group FKs | `asset_group_access` and `asset_group_asset` cascade on delete after migration `3.8.0`. |

## Replaced Structures

| Structure | Current state |
|---|---|
| Original `publisher` plus `publication_link` model | Dropped in `3.2.0` and replaced by `publisher(publisher)` plus `asset_publisher`. |
| `specimen.preparation_type` | Migrated to `specimen.preparation_types` and `asset_specimen.preparation_type` in `3.3.0`. |
| `asset.specify_attachment_id` | Added and then dropped in `3.5.0`. The lasting Specify attachment ID is `asset_specimen.specify_collection_object_attachment_id`. |
| `asset.initial_metadata_recorded_by` | Dropped in `3.1.0`. |

## Naming Remnants

Several defaults and docs still carry file-proxy naming:

| Source | Remnant |
|---|---|
| `application.properties` | Defaults to `dassco_file_proxy`. |
| `docker-compose-app.yaml` | Points `POSTGRES_URL` to `dassco_file_proxy`. |
| File/share/upload tables | Tables such as `directories`, `shared_assets`, `user_access`, `file`, `file_cache`, `asset_caches`, `parked_file`, and `active_large_uploads` are file/share/upload concerns that now live in the asset service database. |
