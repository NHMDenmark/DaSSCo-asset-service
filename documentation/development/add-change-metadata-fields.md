# Add or change asset metadata fields

This guide lists the code paths that usually need to change when adding, updating, removing, or exposing a metadata field on an asset.

Use it when a field must become a first-class ARS/API field. If the value is temporary, pipeline-specific, or not meant to be searchable/public/validated, consider using `Asset.tags` first; it is persisted as JSON and already exposed on the normal asset metadata API.

## Start here

### Pre-flight questions

Before changing code, answer these questions. They determine which sections you need and help avoid adding half-supported fields.

- Is the value permanent, validated, searchable, public, or part of the ARS/API contract? If not, consider `Asset.tags` instead.
- Is it stored directly on the `asset` row, or is it derived from events, files, specimens, or another table?
- Is it nullable for existing assets, required on create, or required only after some workflow step?
- Who owns the value: ARS, Specify, the pipeline, users, or an external system?
- Can users edit it after creation? Can they bulk edit it?
- Should anonymous/external users see it?
- Should users be able to search/query it?
- Does Angular need to display or edit it?

### Quick decision tree

1. **Is it a simple value stored on the asset row?** Follow [Simple stored field](#simple-stored-field-on-asset).
2. **Is it one of a controlled list of strings?** Follow [Enumerated / list values](#enumerated--list-values).
3. **Is it derived from events, files, specimens, or another table?** Follow [Special field shapes](#special-field-shapes).
4. **Should anonymous/external users see it?** Also update [Public asset metadata](#public-asset-metadata).
5. **Should it be searchable, bulk editable, shown in the UI, or synced to Specify?** Also update the matching row in [Optional capabilities](#optional-capabilities).
6. **Are you removing or renaming an existing field?** Follow [Removing or renaming a field](#removing-or-renaming-a-field).

### Minimal happy path for a simple internal field

If the field is a nullable, internal value stored directly on `asset`, you usually only need:

1. Update the protocol/specification docs.
2. Add a Liquibase column and include the changelog in `changelog-master.xml`.
3. Add the field to `Asset.java` with `@Schema`.
4. Read it in `AssetMapper`.
5. Insert and update it in `AssetRepository`.
6. Copy and validate it in `AssetService.updateAsset(...)`.
7. Add or update targeted create/read/update tests.

After that, use the optional table below only for capabilities the field actually needs.

<a id="optional-capabilities"></a>

### Optional capabilities

| If the field must... | Also update | How to decide |
|---|---|---|
| Be searchable/queryable | `QueryItemField`, `QueriesService`, query tests | Users need to filter assets by this value through the query API/UI. |
| Be bulk editable | `BulkUpdateService`, bulk update tests, Angular bulk UI if exposed | Users need to change the value across many assets at once. |
| Be visible to anonymous/external users | Public asset domain/repository/mapper/API/CSV and public Angular types/templates | The value is safe to expose outside authenticated/internal ARS views. |
| Sync to or from Specify | `SpecifyArsSyncService`, Specify mapping docs, sync tests | Specify can send the value, consume it, or is the authoritative source. |
| Be shown or edited in Angular | Angular asset types, detail views, forms, services | Users need to see or edit it in the web UI. |
| Use controlled/list values | Lookup table/list service/frontend dropdown support | Values must come from a controlled vocabulary instead of free text. |
| Come from another table or relationship | The relevant repository/service read/write/delete paths | The value is not a simple `asset` column. |

## Simple stored field on `asset`

Use this as the core path for the common case: adding `new_field` as a nullable string column on `asset`.

### 1. Document the field semantics first

Update the protocol/specification so the behaviour is clear before code changes:

- `documentation/protocols/protocol-asset.md` — field name, description, type, whether it is required, defaults, who sets it.
- If it maps to Specify, update `documentation/specify mapping/ARS_to_Specify.md`.

Decide whether the field is nullable, required on create, mutable after creation, user-editable, bulk-editable, searchable, public, or event-derived.

### 2. Add the database change

Create a new Liquibase changelog under `src/main/resources/liquibase/`, then include it in `src/main/resources/liquibase/changelog-master.xml` after the latest changelog.

```xml
<changeSet id="x.y.z:ADD_ASSET_NEW_FIELD" author="your-initials" context="default">
    <addColumn tableName="asset">
        <column name="new_field" type="text">
            <constraints nullable="true"/>
        </column>
    </addColumn>
</changeSet>
```

Guidelines:

- Prefer nullable columns for existing assets unless you also backfill and add a default.
- Use a separate changeset for backfills or indexes.
- If a field is required, backfill old rows before adding a not-null constraint.
- Update seed/dev SQL under `src/main/resources/sql/` only when those scripts explicitly need the new value.

### 3. Add the API/domain field

Update `src/main/java/dk/northtech/dasscoassetservice/domain/Asset.java`:

```java
@Schema(description = "...", example = "...")
public String new_field;
```

Also consider:

- `toString()` for debugging.
- `equals()` / `hashCode()` only if tests or comparisons must include the new field.
- Use `Instant` for timestamps, `boolean` for default-false flags, and `Boolean` only if you need a nullable tri-state.

Because `Asset` is used directly by the JAX-RS endpoints, this makes the field part of the internal JSON API and generated OpenAPI schema.

### 4. Map reads from the database

Update `src/main/java/dk/northtech/dasscoassetservice/repositories/helpers/AssetMapper.java`:

```java
asset.new_field = rs.getString("new_field");
```

Most asset reads already select `asset.*`, but fields from joined tables still require explicit SELECT aliases.

### 5. Persist on create

Update `src/main/java/dk/northtech/dasscoassetservice/repositories/AssetRepository.java`:

- `INSERT_BASE_ASSET` column list.
- `INSERT_BASE_ASSET` values list.
- `insertBaseAsset(...)` bindings.

Example:

```java
// SQL column list
, new_field

// SQL values list
, :new_field

// binding
.bind("new_field", asset.new_field)
```

### 6. Persist on metadata update

In `AssetRepository.java`, update:

- `UPDATE_ASSET_SQL` set clause.
- `update_asset_internal(...)` binding.

Example:

```java
, new_field = :new_field
```

```java
.bind("new_field", asset.new_field)
```

Then update `src/main/java/dk/northtech/dasscoassetservice/services/AssetService.java` in `updateAsset(...)` so the incoming value is copied to the persisted `existing` asset:

```java
existing.new_field = updatedAsset.new_field;
```

Add validation in `validateAssetFields(...)` or `validateAsset(...)` if the field is required, constrained, or related to another table. For fields that are required only when creating an asset, also update `validateNewAssetAndSetIds(...)`.

### 7. Update related read composition if needed

`AssetService.getAsset(...)` and `AssetService.getAssets(...)` add relationships after `AssetMapper` runs. A simple column does not need extra work, but a related/list field usually does.

If the field is not stored directly on `asset`, update the appropriate repository/service section and deletion cleanup. See [Special field shapes](#special-field-shapes).

### 8. Make it searchable if required

Update search metadata in:

- `src/main/java/dk/northtech/dasscoassetservice/domain/QueryItemField.java`
- `src/main/java/dk/northtech/dasscoassetservice/services/QueriesService.java` in `getNodeProperties()`

For a simple `asset` column:

```java
NEW_FIELD("new_field", "new_field", "asset")
```

and add a `QueryProperty` with the correct data type.

If the field needs a join, add the join logic in `QueriesService.getAssetsFromQuery(...)`. If it needs custom operator handling, update `QueryInner.toBasicPostgreSQLQueryString(...)`.

### 9. Make it bulk-editable if required

`src/main/java/dk/northtech/dasscoassetservice/services/BulkUpdateService.java` currently treats `BulkUpdatePayload.fields` as database column names in `patchAssetFields(...)`.

For a simple DB column this may work without extra code, but still check:

- The field value type binds correctly to JDBI/Postgres.
- The frontend sends the database column name, not the Java display name.
- Any required validation is not bypassed by bulk update.
- Special keys currently handled there include `keycloakUser`, `audited`, and `legality`.

If the field needs validation/conversion or touches another table, add explicit handling in `BulkUpdateService` instead of relying on the generic dynamic SQL.

### 10. Update Specify sync if required

If Specify may update or consume the field, update:

- `src/main/java/dk/northtech/dasscoassetservice/services/SpecifyArsSyncService.java`
  - Add a `case "${new_field}"` in `mapAsset(...)`.
  - Decide whether ARS or Specify is authoritative.
- `documentation/specify mapping/ARS_to_Specify.md`

<a id="public-asset-metadata"></a>

### 11. Expose it in public metadata if required

Only do this for fields safe for anonymous/external viewing.

Update:

- `src/main/java/dk/northtech/dasscoassetservice/domain/PublicAsset.java`
- `src/main/java/dk/northtech/dasscoassetservice/repositories/PublicAssetRepository.java`
- `src/main/java/dk/northtech/dasscoassetservice/repositories/helpers/PublicAssetMapper.java`
- `src/main/java/dk/northtech/dasscoassetservice/webapi/v1/PublicAssetApi.java` CSV header/value writer
- `angular/src/app/types/types.ts` (`PublicAssetMetadata`) and external detailed-view templates if shown in UI

### 12. Update frontend if the field is visible/editable

Common frontend touch points:

- `angular/src/app/types/types.ts` — `Asset` and/or `PublicAssetMetadata` interface.
- `angular/src/app/components/detailed-view/` — internal asset detail display.
- `angular/src/app/components/detailed-view/extern-detailed-view/` — public detail display.
- `angular/src/app/components/bulk-update/` and `angular/src/app/services/bulk-update.service.ts` — bulk edit forms/types.
- Query UI normally gets searchable properties from the backend, but dropdown values may come from cache endpoints.

### 13. Update tests and generated API output

Suggested tests:

- `src/test/java/dk/northtech/dasscoassetservice/services/AssetServiceTest.java` — create, read, update, validation.
- `src/test/java/dk/northtech/dasscoassetservice/services/QueriesServiceTest.java` — if searchable.
- `src/test/java/dk/northtech/dasscoassetservice/services/BulkUpdateServiceTest.java` — if bulk-editable.
- `src/test/java/dk/northtech/dasscoassetservice/services/SpecifyArsSyncServiceTest.java` — if synced with Specify.
- `src/test/java/dk/northtech/dasscoassetservice/services/ExtendableEnumServiceTest.java` — for list/enum changes.
- `src/test/java/dk/northtech/dasscoassetservice/services/SpecimenServiceTest.java` — for specimen/preparation fields.

Run at least the targeted tests, for example:

```bash
./mvnw test -Dtest=AssetServiceTest
```

The OpenAPI files in `target/classes/openapi.*` are generated by Maven; do not edit them by hand.

## Reference sections for non-simple changes

The sections below are reference material. Skip them for a simple stored field unless the pre-flight questions or optional table point you here.

### Enumerated / list values

There are two common tasks: adding a value to an existing list, or adding an entirely new list type.

#### Add a value to an existing list

If the value must exist in every environment after deployment, add it through Liquibase. If it is an operational/admin action, use the lists API where available.

| List | DB table | Backend code | API notes | Special cases |
|---|---|---|---|---|
| Asset status | `asset_status` | `ExtendableEnumService.ExtendableEnum.STATUS`, `AssetStatus.java` | `GET /v1/lists/statuses`, `POST /v1/lists/status`, older `GET /v1/assets/statusList` | Also update Angular `AssetStatus`; `asset.status` has an FK. |
| Internal asset status | `internal_asset_status` | `InternalStatus.java` | Internal/status endpoints | Used by file sync and Specify sync state machines; add transitions carefully. |
| File format | `file_format` | `ExtendableEnumService.ExtendableEnum.FILE_FORMAT`, `AssetService.validateAsset(...)` | `GET/POST /v1/lists/fileformats` | `asset.file_formats` is `text[]`; validation is in service, not an FK. Update Angular `FileFormat` if frontend enum is used. |
| Issue category | `issue_category` | `ExtendableEnumService.ExtendableEnum.ISSUE_CATEGORY`, `AssetService.validateIssue(...)` | `GET/POST /v1/lists/issuecategories` | `issue.category` has an FK. |
| Preparation type | `preparation_type` | `ExtendableEnumService.ExtendableEnum.PREPARATION_TYPE`, `SpecimenService.validateSpecimen(...)` | `GET/POST /v1/lists/preparationtypes` | Used by specimen `preparation_types` and `asset_specimen.preparation_type`. |
| External publisher | `publisher` | `ExtendableEnumService.ExtendableEnum.EXTERNAL_PUBLISHER`, `AssetService.validateAsset(...)` | `GET/POST /v1/lists/externalpublishers` | Asset links live in `asset_publisher`. |
| Subject | `subject` | `ExtendableEnumService.ExtendableEnum.SUBJECT`, `SubjectCache` | Listed by `GET /v1/assets/subjectList` and bulk update subjects | No current `Lists` POST endpoint. Create requires the subject to exist because `asset.subject` has an FK; update currently auto-persists missing subjects. |
| Payload type | no active table/FK | `PayloadTypeCache` | `GET /v1/assets/payloadTypeList` | Currently free text cached from assets. If it should become controlled, add a table, service validation, endpoints, and migration. |
| Role | `role` | `RoleService`, role repositories | Role restriction APIs | Some code paths auto-create roles when restrictions are set. |
| Event type | `event_type` | `DasscoEvent.java` | Event APIs | Event-derived metadata also needs `Asset.mapEvents()` and query mapping. |

#### Add a new list type

1. Create the lookup table and seed values in Liquibase.
2. Add a value to `ExtendableEnumService.ExtendableEnum` if it should use the generic enum service.
3. Add cache fields and getters in `ExtendableEnumService` if the list should be cached.
4. Add endpoints in `src/main/java/dk/northtech/dasscoassetservice/webapi/v1/Lists.java` if users/services need to maintain it.
5. Add validation in the service that consumes the list.
6. Add frontend dropdown/cache support if the UI needs it.
7. Add tests in `ExtendableEnumServiceTest` and the consuming service tests.

### Special field shapes

#### Event-derived fields

Fields such as `asset_created_by`, `asset_updated_by`, `date_asset_created_ars`, `date_metadata_updated`, `metadata_updated_by`, `audited`, and `date_audited` are derived from `event`, not stored directly on `asset`.

Touch points:

- `src/main/java/dk/northtech/dasscoassetservice/domain/DasscoEvent.java`
- Liquibase `event_type` inserts for new events.
- `src/main/java/dk/northtech/dasscoassetservice/domain/Asset.java` in `mapEvents()`.
- `src/main/java/dk/northtech/dasscoassetservice/domain/QueryInner.java` in `eventTypeByMetadataColumn(...)` for event searches.
- Event insertion in the relevant service/repository flow.

#### FK-backed scalar fields

Some API fields look like scalar strings but are stored as foreign-key ids or are read through joins. Examples include `collection`, `digitiser`, `workstation`, and some pipeline values.

For these fields, check:

- The domain field name exposed in `Asset.java` may differ from the stored column (`digitiser` is read from `dassco_user.username`, while `asset.digitiser_id` is stored).
- Read SQL must select the joined value with the alias expected by `AssetMapper`.
- Create/update services must resolve and validate ids before calling repository methods (`validateAndSetCollectionId(...)`, `validateNewAssetAndSetIds(...)`, `setIds(...)`, or service-specific lookup code).
- Repository insert/update SQL should persist the id column, not the display value.
- Query/search needs the relevant join in `QueriesService.getAssetsFromQuery(...)` if the field is searchable.

#### Relationship/list fields

These are not simple `asset` columns and need create/update/read/delete logic:

| Field | Storage | Main code paths |
|---|---|---|
| `funding` | `funding`, `asset_funding` | `FundingService`, `FundingRepository`, `AssetService.persistAsset/updateAsset/getAsset/getAssets`, `AssetRepository.deleteAsset` |
| `complete_digitiser_list` | `digitiser_list`, `dassco_user` | `UserRepository`, `DigitiserListRepository`, `AssetService.persistAsset/updateAsset/getAsset/getAssets`, bulk update digitiser actions |
| `issues` | `issue` | `IssueRepository`, `AssetService.validateIssue/updateAsset/getAsset/getAssets`, bulk update issue actions |
| `role_restrictions` | `asset_role_restriction`, `role` | `RoleRepository`, `RoleService`, `RightsValidationService`, `AssetService`, delete cleanup |
| `external_publishers` | `publisher`, `asset_publisher` | `PublisherRepository`, `AssetService.validateAsset/updateAsset/getAsset/getAssets`, public/CSV if exposed |
| `parent_guids` | `parent_child` | `AssetRepository.insert_parent_child/delete_parent_child/getParents`, `AssetService.updateAsset/getAsset/getAssets/deleteAsset` |
| `asset_specimen` / `multi_specimen` | `asset_specimen`, `specimen` | `SpecimenService`, `SpecimenRepository`, `AssetService`; `multi_specimen` is computed from linked specimens |
| `legality` | `legality`, `asset.legality_id` | `LegalityRepository`, `AssetService.updateAsset`, public API legal display/search |
| `mime_type` | `file.mime_type` | Read as an aggregate in asset/public repositories; not an `asset` column |

For these fields, update both single-asset and multi-asset read paths. Multi-asset reads often build maps manually in `AssetService.getAssets(...)`.

### Removing or renaming a field

1. Check API consumers and Specify mappings first.
2. Add a migration/backfill period if clients still send the old name.
3. Remove or rename in `Asset.java`, repositories, mappers, service copy/validation logic, query fields, bulk update, public API, frontend, and tests.
4. Add Liquibase `dropColumn` only after data has been migrated or intentionally discarded.
5. Remove generated/static references only in source; do not edit `target/classes/openapi.*`.

## Final checklist

Before merging, verify:

- [ ] Protocol docs describe the field/value and ownership.
- [ ] Liquibase adds/backfills/seeds the DB change and is included in `changelog-master.xml`.
- [ ] `Asset.java` exposes the API field with `@Schema`.
- [ ] `AssetMapper` reads it.
- [ ] `AssetRepository` inserts and updates it.
- [ ] `AssetService.updateAsset(...)` copies it and validation covers required/list constraints.
- [ ] Relationship cleanup is handled in `AssetRepository.deleteAsset(...)` when relevant.
- [ ] Query metadata/search is updated if the field should be searchable.
- [ ] Bulk update is updated or explicitly not supported.
- [ ] Public API/CSV is updated if the field is externally visible.
- [ ] Specify sync/mapping is updated if Specify can send/read it.
- [ ] Angular types/components/forms are updated if the UI uses it.
- [ ] Targeted tests cover create, read, update, and special behaviour.
