# DaSSCo Asset Service Database Documentation

This directory documents the database structures owned or used by the DaSSCo Asset Service.

The relational PostgreSQL model is the source of truth for current asset-service data. Apache AGE is still present as a legacy graph dependency in limited code paths, but it is not created by the main Liquibase changelog and should not be treated as the authoritative current model.

Liquibase also creates its normal bookkeeping tables, `databasechangelog` and `databasechangeloglock`. They are intentionally excluded from the domain diagrams.

## Documentation Map

| File | Purpose |
|---|---|
| [Instances and schemas](instances-and-schemas.md) | Database names, schemas, Liquibase scope, and local/deployment naming caveats. |
| [Relational core](relational-core.md) | Core ARS domain ERD and table catalog for assets, specimens, events, users, roles, and groups. |
| [File, share, upload, and query support](file-share-upload-query.md) | File metadata, directory/share state, cache, saved-query, and large-upload tables. |
| [Mappings schema](mappings-schema.md) | `mappings` schema for ARS-to-Specify institution and collection names. |
| [Database diagrams](database-diagrams.drawio) | Draw.io diagrams referenced by the schema documents. Pages: `Relational Core`, `File Share Upload Query`, `Mappings Schema`, and `AGE Legacy Graph`. |
| [Reference data](reference-data.md) | Seeded lookup values and Java enum mismatches. |
| [Lookup values and constraints](lookup-values-and-constraints.md) | Extendable enum-list tables, soft references, duplicate rules, cascades, and historical replacements. |
| [Apache AGE legacy graph](apache-age-legacy.md) | Legacy graph setup, active AGE usage, labels, edges, and caveats. |
| [Source map](source-map.md) | Source files used to verify the database documentation. |

## Quick Model Summary

| Area | Current role |
|---|---|
| `public` schema | Main relational model for assets, specimens, events, statuses, users, access restrictions, asset groups, file/share/cache/upload tables, saved queries, and Liquibase bookkeeping. |
| `mappings` schema | ARS-to-Specify name mapping tables for institutions and collections. |
| Apache AGE graph `dassco` | Legacy graph inside PostgreSQL. Some code still reads or updates it, but current create/update/query/statistics flows are mostly relational. |

## Important Reading Notes

Some lookup tables are intentionally used as application-level enum lists, even where a database FK is not possible or not used. For example, `asset.file_formats` is a `text[]`, while `file_format` is the maintained list of valid values checked by the application. See [Lookup values and constraints](lookup-values-and-constraints.md).
