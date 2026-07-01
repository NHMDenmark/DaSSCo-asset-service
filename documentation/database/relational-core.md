# Relational Core

This file covers the core application-owned relational model in the `public` schema: assets, specimens, events, users, roles, access restrictions, groups, and core lookup tables.

The ER diagram shows enforced foreign keys only. Isolated lookup tables can still be application-level enum lists; see [Lookup values and constraints](lookup-values-and-constraints.md).

## Diagram

See [database-diagrams.drawio](database-diagrams.drawio), page `Relational Core`.

The diagram shows enforced foreign keys as solid lines and application-level validation or soft references as dashed lines. `FILE_FORMAT` is intentionally shown as an application-level enum-list table for `asset.file_formats`; `asset.file_formats` is a `text[]` and is not FK-enforced.

## Table Catalog

| Table | Purpose | Columns | Key constraints and notes |
|---|---|---|---|
| `internal_asset_status` | Lookup table for upload/sync lifecycle state. | `internal_status` | PK `internal_status`. Seeded with current and legacy statuses. See [Reference data](reference-data.md). |
| `institution` | Owning institution. | `institution_name` | PK `institution_name`. |
| `collection` | Collection within an institution. | `collection_id`, `collection_name`, `institution_name` | PK `collection_id`; FK to `institution`. No uniqueness constraint on name plus institution. |
| `role` | Application-maintained role catalog used by role restriction tables. | `role` | PK `role`. Not seeded by the main default changelog; services create roles when needed. |
| `collection_role_restriction` | Roles allowed/restricted for a collection. | `collection_role_restriction_id`, `role`, `collection_id` | FKs to `role` and `collection`. No composite unique constraint prevents duplicate restrictions. |
| `institution_role_restriction` | Roles allowed/restricted for an institution. | `institution_role_restriction_id`, `role`, `institution_name` | FKs to `role` and `institution`. No composite unique constraint prevents duplicate restrictions. |
| `asset_status` | Extendable enum-list table for public/workflow asset status. | `asset_status` | PK `asset_status`; FK target for `asset.status`. Values can be added through service paths. |
| `pipeline` | Digitisation pipeline within an institution. | `pipeline_id`, `pipeline_name`, `institution_name` | PK `pipeline_id`; FK to `institution`. No uniqueness constraint on name plus institution. |
| `workstation_status` | Workstation status lookup. | `workstation_status` | PK `workstation_status`. |
| `workstation` | Physical/logical workstation within an institution. | `workstation_id`, `workstation_name`, `workstation_status`, `institution_name` | FKs to `workstation_status` and `institution`. No uniqueness constraint on name plus institution. |
| `specimen` | Specimen metadata. | `specimen_id`, `specimen_pid`, `collection_id`, `barcode`, `preparation_types` | FK to `collection`; unique `(collection_id, barcode)`. `preparation_types` is an application-validated `text[]`, not FK-enforced. |
| `preparation_type` | Extendable enum-list table for preparation types. | `preparation_type` | PK `preparation_type`. FK target for `asset_specimen.preparation_type`; application-validates `specimen.preparation_types`. |
| `funding` | Funding entity lookup/data. | `funding_id`, `funding` | PK `funding_id`. |
| `subject` | Extendable enum-list table for asset subjects. | `subject` | PK `subject`; FK target for `asset.subject`. Asset update code can add missing subjects. |
| `event_type` | Event type lookup. | `event` | PK `event`. Does not seed every value in the Java `DasscoEvent` enum. |
| `dassco_user` | Local user mirror keyed to external identity-provider users. | `dassco_user_id`, `keycloak_id`, `username` | PK `dassco_user_id`; unique nullable `keycloak_id`; unique not-null `username`. PostgreSQL allows multiple `NULL` values in a unique nullable column. |
| `issue_category` | Extendable enum-list table for issue categories. | `issue_category` | PK `issue_category`; FK target for `issue.category`. No default seed values found in the main changelog. |
| `file_format` | Extendable enum-list table for asset file formats. | `file_format` | PK `file_format`. `asset.file_formats` is a `text[]` and is application-validated rather than FK-enforced. |
| `legality` | Copyright/license/credit metadata. | `legality_id`, `copyright`, `license`, `credit` | PK `legality_id`; FK target for `asset.legality_id`. |
| `asset` | Central asset metadata record. | `asset_guid`, `asset_pid`, `asset_locked`, `subject`, `collection_id`, `digitiser_id`, `file_formats`, `payload_type`, `status`, `tags`, `workstation_id`, `internal_status`, `make_public`, `metadata_source`, `push_to_specify`, `metadata_version`, `camera_setting_control`, `date_asset_taken`, `date_asset_finalised`, `date_metadata_ingested`, `error_message`, `error_timestamp`, `mos_id`, `legality_id`, `specify_attachment_remarks`, `specify_attachment_title` | PK `asset_guid`; FKs to `subject`, `collection`, `dassco_user`, `asset_status`, `workstation`, `internal_asset_status`, and `legality`. `file_formats` is application-validated against `file_format`. `payload_type` has no current lookup table or FK. |
| `parent_child` | Asset-to-asset parent/child relation. | `parent_child_id`, `parent_guid`, `child_guid` | Both GUID columns FK to `asset.asset_guid`. No unique constraint prevents duplicate edges. |
| `issue` | Issues attached to assets. | `issue_id`, `asset_guid`, `category`, `name`, `timestamp`, `status`, `description`, `notes`, `solved` | FKs to `asset` and `issue_category`. |
| `event` | Audit/history events for assets. | `event_id`, `asset_guid`, `dassco_user_id`, `event`, `timestamp`, `pipeline_id`, `change_list`, `bulk_update_uuid` | FKs to `asset`, `dassco_user`, `event_type`, and `pipeline`; `timestamp` defaults to `now()`; index on `bulk_update_uuid`. |
| `asset_funding` | Join table between assets and funding rows. | `asset_funding_id`, `funding_id`, `asset_guid` | FKs to `funding` and `asset`. No composite unique constraint prevents duplicate asset/funding pairs. |
| `digitiser_list` | Users assigned/listed for an asset. | `digitiser_list_id`, `dassco_user_id`, `asset_guid` | FKs to `dassco_user` and `asset`. No composite unique constraint prevents duplicate user/asset pairs. |
| `asset_group` | Named user-created group of assets. | `asset_group_id`, `group_name`, `creator_user_id` | FK to `dassco_user`; unique `group_name`. Service code lowercases group names before create/read paths. |
| `asset_group_access` | Users with access to an asset group. | `asset_group_access_id`, `asset_group_id`, `dassco_user_id` | FKs to `asset_group` and `dassco_user`, both cascade on delete after `3.8.0`. No composite unique constraint prevents duplicate access rows. |
| `asset_group_asset` | Assets contained in an asset group. | `asset_group_asset_id`, `asset_group_id`, `asset_guid` | FKs to `asset_group` and `asset`, both cascade on delete after `3.8.0`. No composite unique constraint prevents duplicate membership rows. |
| `asset_specimen` | Join between assets and specimens. | `asset_specimen_id`, `asset_guid`, `specimen_id`, `preparation_type`, `specify_collection_object_attachment_id`, `asset_detached` | FKs to `asset`, `specimen`, and `preparation_type`. `preparation_type` is nullable. `asset_detached` defaults false. |
| `publisher` | Extendable enum-list table for external publishers. | `publisher` | PK `publisher`. Recreated in `3.2.0` after an older `publisher` and `publication_link` model were dropped. |
| `asset_publisher` | Asset publication metadata by publisher. | `asset_publisher_id`, `description`, `publisher`, `asset_guid` | FKs to `publisher` and `asset`. |
| `specimen_role_restriction` | Roles allowed/restricted for a specimen. | `specimen_role_restriction_id`, `role`, `specimen_id` | FKs to `role` and `specimen`. No composite unique constraint prevents duplicate restrictions. |
| `asset_role_restriction` | Roles allowed/restricted for an asset. | `asset_role_restriction_id`, `role`, `asset_guid` | FKs to `role` and `asset`. No composite unique constraint prevents duplicate restrictions. |
