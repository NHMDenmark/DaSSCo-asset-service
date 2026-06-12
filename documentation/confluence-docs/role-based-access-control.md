### Role Based Access Control

| ENV | URL |
|--|--|
| TEST | https://idp.test.dassco.dk |
| PROD | xx |

ARS access is controlled by Keycloak realm roles and by checks in the ARS codebase.

## Keycloak concepts

### Realms

Realms are a way to have multiple systems and user databases on one Keycloak instance. For DaSSCo we have two realms:

- `dassco` - used by DaSSCo applications such as ARS / DaSSCo Storage. Add application users and application roles here.
- `master` - the Keycloak administration realm. Admin-console users are created here, not in the `dassco` realm.

### Clients

Clients split a realm into applications. For example, DaSSCo Storage / ARS has its own client. If Specify used the same IDP it should have its own client, while still using users from the same `dassco` realm.

### Realm roles vs client roles

DaSSCo currently uses **realm roles**. Use a realm role when the role can be shared by DaSSCo applications. Use a client role only if the role is specific to a single Keycloak client.

## Keycloak administration access

Realm: `master`

Users who manage Keycloak itself must exist in the `master` realm and be assigned appropriate Keycloak admin roles. See the current Keycloak role guide: [Server Administration Guide](https://www.keycloak.org/docs/latest/server_admin/#_admin_permissions).

## ARS / DaSSCo Storage access

Realm: `dassco`

Create ARS users in the `dassco` realm and assign the realm roles needed by the user.

| Role | Intended access | Current status / notes |
|--|--|--|
| `dassco-user` | Role for students and externals that may need access for bulk downloads or similar workflows. This role should be able to download assets, read assets in the UI, search for assets via the UI, create/delete groups, and update groups with assets/users. It should **not** be able to change metadata or update assets in any other way. Other endpoints should not be available to this role. | Currently most endpoints and functionalities are available to this role, so permissions need tightening. |
| `dassco-admin` | Administrator role. This role should allow all endpoints and functionalities by default. | Current status is that it can see/interact with things with `READ` or `WRITE` restrictions set. |
| `dassco-developer` | Elevated user role with access to most functionality. Long term this should likely be split into an actual developer role and a curator-style role. It should **not** be able to delete metadata, delete files from the fileshare of an asset, force sync Specify, or create/update institutions, collections, workstations or pipelines. It can access all other endpoints and functionality provided by the endpoints or UI. | Current status has many incorrect permissions, mostly missing permissions but also a few permissions that are too broad. |
| `service-user` | Client/service role that can do most things. This may currently be too broad and can be trimmed later. It should **not** be able to delete metadata, delete files from the fileshare of an asset, or create/update institutions, collections, workstations or pipelines. It can access all other endpoints. | Service accounts, e.g. services running on the Refinery. |

For all four roles, when a `role_restriction` has been set for an institution, collection, specimen or asset, the role follows the normal access procedure: the user/service must have the appropriate restricted role in order to interact with that restricted resource. Endpoint access through `@RolesAllowed` does not automatically bypass asset/resource-level role restrictions.

The role names used by ARS are defined in:

- `src/main/java/dk/northtech/dasscoassetservice/domain/SecurityRoles.java`

```java
public interface SecurityRoles {
    String ADMIN = "dassco-admin";
    String DEVELOPER = "dassco-developer";
    String USER = "dassco-user";
    String SERVICE = "service-user";
}
```

## How to add a new role

Example requirement: create a role that can **unlock assets** and **view fileproxy/ARS logs**.

### 1. Create the role in Keycloak

In the `dassco` realm:

1. Go to **Realm roles**.
2. Create the new role, for example `dassco-asset-unlocker-log-viewer`.
3. Assign that role to the relevant users or service accounts.

### 2. Add the role constant in ARS

Update `src/main/java/dk/northtech/dasscoassetservice/domain/SecurityRoles.java`:

```java
String ASSET_UNLOCKER_LOG_VIEWER = "dassco-asset-unlocker-log-viewer";
```

### 3. Add the role to endpoint `rolesAllowed` / `@RolesAllowed`

Permissions for ARS endpoints are set with `jakarta.annotation.security.RolesAllowed` annotations in `src/main/java/dk/northtech/dasscoassetservice/webapi/v1/`.

To allow the new role to unlock assets, update `Assetupdates.java` on the unlock endpoint:

```java
@PUT
@Path("{assetGuid}/unlock")
@RolesAllowed({
    SecurityRoles.ADMIN,
    SecurityRoles.DEVELOPER,
    SecurityRoles.SERVICE,
    SecurityRoles.USER,
    SecurityRoles.ASSET_UNLOCKER_LOG_VIEWER
})
public Response unlockAssetMetadata(...)
```

To allow the new role to view logs, update `Logs.java` on the log endpoints:

```java
@GET
@RolesAllowed({
    SecurityRoles.DEVELOPER,
    SecurityRoles.ADMIN,
    SecurityRoles.ASSET_UNLOCKER_LOG_VIEWER
})
public Response getLogFiles(...)

@GET
@Path("{fileName}")
@RolesAllowed({
    SecurityRoles.DEVELOPER,
    SecurityRoles.ADMIN,
    SecurityRoles.ASSET_UNLOCKER_LOG_VIEWER
})
public Response getLogFile(...)
```

If an endpoint has OpenAPI-generated metadata containing a `rolesAllowed` property, add the new role there as well so the generated API documentation matches the runtime annotation. The runtime authorization source of truth is still the Java `@RolesAllowed` annotation.

### 4. Check whether service-level role restrictions also apply

Some functionality is not only endpoint-protected. Assets can also have role restrictions, exposed as `role_restrictions` and managed through bulk update functionality. This controls read/access restrictions on specific assets and is separate from endpoint access.

Relevant places:

- Backend bulk-update role endpoints: `src/main/java/dk/northtech/dasscoassetservice/webapi/v1/BulkUpdateAssetApi.java`
- Asset access logic/services: search for `role_restrictions`, `hasAccess`, or role checks in `src/main/java/dk/northtech/dasscoassetservice/services/`
- Frontend display/editing of role restrictions:
  - `angular/src/app/components/bulk-update/`
  - `angular/src/app/pipes/role-restriction.pipe.ts`

If the new role should be selectable as an asset role restriction, make sure it exists in Keycloak and is returned by the roles endpoint used by the frontend.

### 5. Test the role

Recommended checks:

1. User without the role gets `403 Forbidden` for the newly protected functionality.
2. User with the role can call only the intended endpoints.
3. Existing roles still work.
4. OpenAPI documentation lists the expected roles if `rolesAllowed` metadata is generated.

## Places where ARS permissions are set

### Jersey security registration

`@RolesAllowed` support is enabled in:

- `src/main/java/dk/northtech/dasscoassetservice/configuration/JerseyApplicationConfig.java`

It registers:

```java
register(RolesAllowedDynamicFeature.class);
```

### Role constants

Role names are centralized in:

- `src/main/java/dk/northtech/dasscoassetservice/domain/SecurityRoles.java`

Always add new code-level roles here and reference the constant from annotations.

### Endpoint annotations

Endpoint-level permissions are set in the web API classes under:

- `src/main/java/dk/northtech/dasscoassetservice/webapi/v1/`

Search for:

```bash
rg "@RolesAllowed|rolesAllowed" src/main/java angular/src
```

## Current protected endpoint areas

The main protected endpoint areas are:

| API class | Path area | Current role pattern |
|--|--|--|
| `AssetApi.java` | `/v1/assets` | Admin/developer for internal status and in-progress; admin/user/developer/service for normal asset list/read access endpoints. `dassco-user` should keep read/search/download-style access only. |
| `Assetupdates.java` | `/v1/assetmetadata` | Unlock: admin/developer/service/user. Status, audit and create/update flows: mostly admin/service/user depending on operation. Delete: admin/developer/service; hard metadata delete: admin only. Intended target: developer and service should not delete metadata or files; user should not change metadata or update assets. |
| `Logs.java` | `/logs` | Admin/developer. Add new operational roles here when they need log access. |
| `AssetGroups.java` | `/v1/assetgroups` | Admin/developer/service/user for group operations. |
| `BulkUpdateAssetApi.java` | `/v1/assets/bulkupdate` | Class-level admin/developer/service/user. |
| `Collections.java` | `/v1/institutions/{institutionName}/collections` | List allows admin/developer/service/user; create/update/delete are mostly admin/developer/service. Intended target: developer and service should not create/update institutions, collections, workstations or pipelines. |
| `Institutions.java` | `/v1/institutions` | Admin/developer/service. Intended target: only admin should create/update institutions. |
| `Workstations.java` | `/v1/institutions/{institutionName}/workstations` | Admin/developer/service. Intended target: only admin should create/update workstations. |
| `Pipelines.java` | `/v1/institutions/{institutionName}/pipelines` | Admin/developer/service. Intended target: only admin should create/update pipelines. |
| `Lists.java` | `/v1/lists` | Read list values often allows admin/developer/service/user; write/delete mostly admin/developer/service. |
| `Externalpublishers.java` | `/v1/assetmetadata/{assetGuid}/externalpublishers` | Admin/developer/service. |
| `StatisticsDataApi.java` | `/v1/graphdata` | Admin/developer/service. |
| `AssetChangeApi.java` | `/v1/event/change` | Admin/developer/service. |
| `EventApi.java` | `/v1/events` | Admin/developer for event types. |
| `OpenAPI.java` | `/openapi.json`, `/openapi.yaml` | OpenAPI docs are admin/developer/service where protected. |

Some endpoints currently have no `@RolesAllowed` annotation. Treat those as public or protected by another layer only after verifying the security configuration. Before adding sensitive functionality, explicitly add `@RolesAllowed`.

## Example summary: asset unlocker + log viewer

To implement `dassco-asset-unlocker-log-viewer`:

1. Create realm role `dassco-asset-unlocker-log-viewer` in Keycloak `dassco` realm.
2. Add `ASSET_UNLOCKER_LOG_VIEWER` to `SecurityRoles.java`.
3. Add `SecurityRoles.ASSET_UNLOCKER_LOG_VIEWER` to:
   - `Assetupdates.java` endpoint `PUT /v1/assetmetadata/{assetGuid}/unlock`
   - `Logs.java` endpoints `GET /logs` and `GET /logs/{fileName}`
4. Add the role to any generated/documented `rolesAllowed` property for those operations, if present.
5. Test with users both with and without the new role.
