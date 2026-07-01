# File, Share, Upload, and Query Support

This file covers the non-core `public` tables used for file metadata, directory/share state, caches, saved queries, and large upload tracking.

The ER diagram shows enforced foreign keys only. Some columns named `asset_guid`, `username`, or `dassco_user_id` are intentionally not drawn where Liquibase does not enforce the relationship.

## Diagram

See [database-diagrams.drawio](database-diagrams.drawio), page `File Share Upload Query`.

## Table Catalog

| Table | Purpose | Columns | Key constraints and notes |
|---|---|---|---|
| `shared_assets` | File/share rows for assets exposed through directories. | `shared_asset_id`, `asset_guid`, `creation_datetime`, `directory_id` | FK to `directories` with delete cascade. `asset_guid` is not FK-enforced. |
| `user_access` | Access tokens/users for a directory/share. | `user_access_id`, `username`, `token`, `creation_datetime`, `directory_id` | FK to `directories` with delete cascade. `username` is not FK-enforced to `dassco_user`. |
| `asset_caches` | Older cache metadata keyed by asset path. | `asset_cache_id`, `asset_path`, `file_size`, `expiration_datetime`, `creation_datetime` | No FKs. Liquibase contains a commented-out drop-table note saying it can be added again when its purpose is known. |
| `file` | File records belonging to an asset. | `file_id`, `asset_guid`, `size_bytes`, `path`, `crc`, `delete_after_sync`, `sync_status`, `mime_type`, `has_thumbnail` | FK to `asset`; FK to `file_sync_status`; index on `path`. `delete_after_sync` defaults false, `sync_status` defaults `NEW_FILE`, and `has_thumbnail` defaults false. |
| `directories` | Share/upload directory allocation state. | `directory_id`, `uri`, `node_host`, `access`, `creation_datetime`, `sync_user`, `sync_workstation`, `sync_pipeline`, `allocated_storage_mb`, `awaiting_erda_sync`, `erda_sync_attempts`, `specify_sync_log_id` | PK `directory_id`; `access` uses PostgreSQL enum type `access_type`. `awaiting_erda_sync` defaults false and `erda_sync_attempts` defaults 0. |
| `file_sync_status` | File sync status lookup. | `file_sync_status` | PK `file_sync_status`; seeded with `NEW_FILE` and `SYNCHRONIZED`. |
| `file_cache` | Cache rows for files. | `file_cache_id`, `file_id`, `expiration_datetime`, `creation_datetime` | FK to `file`; index on `expiration_datetime`. Changelog creates `FILE_CACHE`, but PostgreSQL folds the unquoted name to `file_cache`. |
| `saved_query` | User-saved query JSON. | `username`, `name`, `query` | FK to `dassco_user.username` with cascade on delete; unique `(username, name)`; index on `username`. No primary key. |
| `asset_change` | Directory-scoped asset change log. | `asset_change_id`, `change`, `dassco_user_id`, `directory_id`, `asset_guid`, `timestamp` | FK to `directories` with delete cascade. `timestamp` defaults to `now()`. `dassco_user_id` and `asset_guid` are not FK-enforced. |
| `parked_file` | Files parked before they become attached/processed. | `parked_file_id`, `path`, `size_bytes`, `timestamp` | Unique `path`; index on `timestamp`; `timestamp` defaults to `now()`. |
| `active_large_uploads` | Active large upload/TUS-style tracking. | `upload_id`, `asset_guid`, `directory_id`, `path`, `created_at`, `updated_at` | PK `upload_id`; FK to `directories` with delete cascade; indexes on `directory_id` and `asset_guid`; `created_at` and `updated_at` default to `now()`. `asset_guid` is indexed but not FK-enforced. |
