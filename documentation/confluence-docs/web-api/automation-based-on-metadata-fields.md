# Generated metadatafields and automation Based On Metadata Fields

This document lists fields that are either:

- generated or derived automatically by ARS, or
- used to start automatic processing in ARS.

## Generated or Derived Fields

| Field                    | Type of automation           | How it is populated                                                                     | Notes                                                                                           |
|--------------------------|------------------------------|-----------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------|
| `multi_specimen`         | Derived                      | Calculated from the number of linked specimens when an asset is read.                   | `true` when more than one specimen is linked.                                                   |
| `created_date`           | Generated                    | Set automatically when an asset is created.                                             | Metadata creation timestamp in ARS.                                                             |
| `date_metadata_updated`  | Generated / derived          | Set on asset creation and later updated from metadata update events.                    | Tracks latest metadata update in ARS.                                                           |
| `internal_status`        | Generated                    | Managed by backend workflows.                                                           | Examples: `METADATA_RECEIVED`, `ASSET_RECEIVED`, `ERDA_SYNCHRONISED`, `SPECIFY_SYNC_SCHEDULED`. |
| `audited`                | Derived                      | Derived from asset events.                                                              | Becomes `true` when an `AUDIT_ASSET` event exists; reset by later update events.                |
| `audited_by`             | Derived                      | Derived from latest `AUDIT_ASSET` event.                                                | Taken from `event.user`.                                                                        |
| `date_audited`           | Derived                      | Derived from latest `AUDIT_ASSET` event.                                                | Taken from `event.timestamp`.                                                                   |
| `date_pushed_to_specify` | Derived                      | Derived from the first `SYNCHRONISE_SPECIFY` event.                                     | Represents first sync to Specify.                                                               |
| `metadata_created_by`    | Derived                      | Derived from `CREATE_ASSET_METADATA` event.                                             | Taken from `event.user`.                                                                        |
| `metadata_updated_by`    | Derived                      | Derived from latest `UPDATE_ASSET_METADATA` or `BULK_UPDATE_ASSET_METADATA` event.      | Taken from `event.user`.                                                                        |
| `asset_created_by`       | Derived                      | Derived from first `CREATE_ASSET` event.                                                | Taken from `event.user`.                                                                        |
| `date_asset_created_ars` | Derived                      | Derived from first `CREATE_ASSET` event.                                                | Taken from `event.timestamp`.                                                                   |
| `asset_updated_by`       | Derived                      | Derived from latest `UPDATE_ASSET` event.                                               | Taken from `event.user`.                                                                        |
| `date_asset_updated_ars` | Derived                      | Derived from latest `UPDATE_ASSET` event.                                               | Taken from `event.timestamp`.                                                                   |
| `asset_deleted_by`       | Derived                      | Derived from `DELETE_ASSET` event.                                                      | Taken from `event.user`.                                                                        |
| `date_asset_deleted_ars` | Derived                      | Derived from `DELETE_ASSET` event.                                                      | Taken from `event.timestamp`.                                                                   |
| `pipeline`               | Derived in mapped asset view | Taken from the `CREATE_ASSET_METADATA` event when events are mapped back onto an asset. | This is intentionally not the latest pipeline from later events.                                |

## Sometimes Generated Automatically

These are not generally backend-generated for all assets, but there are important automated cases.

| Field                        | Type of automation            | How it is populated                                                      | Notes                                                               |
|------------------------------|-------------------------------|--------------------------------------------------------------------------|---------------------------------------------------------------------|
| `asset_guid`                 | Generated in Specify-ARS sync | Generated automatically when a new asset is created from Specify import. | Normal asset creation still expects the caller to provide it.       |
| `status`                     | Generated in Specify-ARS sync | Set to `SPECIFY_CREATED` for new assets created from Specify.            | Normal asset creation/update otherwise uses caller-provided status. |
| `specify_attachment_remarks` | Populated by Specify-ARS sync | Updated from Specify-ARS sync payloads.                                  | Specify-managed field.                                              |
| `specify_attachment_title`   | Populated by Specify-ARS sync | Updated from Specify-ARS sync payloads.                                  | Specify-managed field.                                              |

## Fields That Start Automatic Processing

| Field             | Trigger condition                                                 | Automatic behaviour                                        | Notes                                                                                                       |
|-------------------|-------------------------------------------------------------------|------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------|
| `push_to_specify` | `true` on an updated asset                                        | Can trigger automatic scheduling of sync to Specify.       | This only happens when other conditions are also met.                                                       |
| `asset_locked`    | `true` together with `push_to_specify` and a synced storage state | Allows automatic sync scheduling to Specify.               | Used by `checkAndSync()`.                                                                                   |
| `internal_status` | Changed to `ASSET_RECEIVED`                                       | Starts the flow that queues the asset for downstream sync. | Happens when upload/share completion is acknowledged.                                                       |
| `internal_status` | Changed to `ERDA_SYNCHRONISED`                                    | Marks storage sync as complete.                            | This does not by itself schedule Specify sync, but it is one of the conditions checked by `checkAndSync()`. |
| `status`          | Set to `FOR_DELETION`                                             | Causes a `DELETE_ASSET` event to be created on update.     | Event-driven side effect rather than queue processing.                                                      |

## Important Combined Logic

The main automatic Specify sync trigger is not based on a single field alone.

Automatic sync to Specify is started when all of the following are true:

- `asset_locked = true`
- `push_to_specify = true`
- `internal_status` is `ERDA_SYNCHRONISED` or `SPECIFY_SYNCHRONISED`

When that happens, ARS sends the asset to the queue and updates `internal_status` to `SPECIFY_SYNC_SCHEDULED`.

## Backend References

- `src/main/java/dk/northtech/dasscoassetservice/services/AssetService.java`
- `src/main/java/dk/northtech/dasscoassetservice/services/AssetSyncService.java`
- `src/main/java/dk/northtech/dasscoassetservice/services/SpecifyArsSyncService.java`
- `src/main/java/dk/northtech/dasscoassetservice/domain/Asset.java`
- `documentation/protocols/protocol-asset.md`
