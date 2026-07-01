# Asset groups flow diagrams

This folder documents the asset-groups feature end-to-end, from the PostgreSQL tables through Java API/service/repository code to the Angular UI.

Open the diagrams with [diagrams.net / draw.io](https://app.diagrams.net/):

- [`asset-groups-flows.drawio`](asset-groups-flows.drawio)

## Diagram pages

1. **Data model** — `asset_group`, `asset_group_asset`, `asset_group_access`, `asset`, and `dassco_user`, including foreign keys and cascade deletes.
2. **List and view groups** — `GET /assetgroups/`, `GET /assetgroups/owned`, table expansion, and navigation to detailed asset view.
3. **Create group** — create from Query page selected assets or Asset Groups page empty group; includes validation, duplicate names, missing assets, and read/write permission branches.
4. **Edit group assets** — add assets, remove assets, shared-group write checks, and delete-on-empty behavior.
5. **Grant and revoke users** — Keycloak user lookup, user persistence in `dassco_user`, grant access, revoke access, owner/admin checks, and write checks before sharing.
6. **Download ZIP and CSV** — ZIP bundle job/poll/download/cancel/failure via file-proxy, plus CSV create/download/delete-temp-file flow.
7. **Role and access checks** — `@RolesAllowed`, group access list, group ownership/admin, and per-asset read/write role checks.
8. **Delete groups and audit assets** — single/bulk delete and selected group asset `bulk/audit` flow.
9. **Failure branches and UI handling** — common `DaSSCoError` sources and how the Angular UI handles them.

## Main source references

### Angular frontend

- `angular/src/app/components/asset-groups/asset-groups.component.ts`
- `angular/src/app/components/asset-groups/asset-groups.component.html`
- `angular/src/app/components/dialogs/asset-group-dialog/asset-group-dialog.component.ts`
- `angular/src/app/components/queries/queries.component.ts`
- `angular/src/app/services/asset-group.service.ts`
- `angular/src/app/services/keycloak-user.service.ts`
- `angular/src/app/services/detailed-view.service.ts`
- `angular/src/app/services/asset-bundle-download.service.ts`
- `angular/src/app/types/types.ts`

### Java backend

- `src/main/java/dk/northtech/dasscoassetservice/webapi/v1/AssetGroups.java`
- `src/main/java/dk/northtech/dasscoassetservice/services/AssetGroupService.java`
- `src/main/java/dk/northtech/dasscoassetservice/repositories/AssetGroupRepository.java`
- `src/main/java/dk/northtech/dasscoassetservice/services/RightsValidationService.java`
- `src/main/java/dk/northtech/dasscoassetservice/repositories/UserRepository.java`
- `src/main/java/dk/northtech/dasscoassetservice/webapi/v1/AssetApi.java`
- `src/main/java/dk/northtech/dasscoassetservice/webapi/v1/Assetupdates.java`
- `src/main/java/dk/northtech/dasscoassetservice/domain/AssetGroup.java`

### Database schema

- `src/main/resources/liquibase/changelog-1.0.0.xml`
- `src/main/resources/liquibase/changelog-3.8.0.xml`

## Maintenance notes

- Keep endpoint paths in the diagrams aligned with `AssetGroups.java` and the Angular `AssetGroupService` methods.
- When changing role logic, update page 7 and cross-check `RightsValidationService`.
- When changing DB schema or cascade behavior, update page 1 and cross-check the Liquibase changelogs.
- When changing file-proxy bundle/CSV endpoints, update page 6 and cross-check `DetailedViewService` and `AssetBundleDownloadService`.
- The diagrams intentionally include adjacent flows that are easy to forget: listing/owned groups, revoking access, removing assets, deleting groups, selected-asset audit, and failure handling.
