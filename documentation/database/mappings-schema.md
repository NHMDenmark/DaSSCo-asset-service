# Mappings Schema

The `mappings` schema keeps ARS-facing names and Specify-facing names separate, then maps ARS names to Specify names.

The schema is created by `src/main/resources/liquibase/changelog-2.0.0.xml`.

## Diagram

See [database-diagrams.drawio](database-diagrams.drawio), page `Mappings Schema`.

## Table Catalog

| Table | Purpose | Columns | Key constraints and notes |
|---|---|---|---|
| `mappings.institutions_specify` | Specify-side institution names. | `id`, `name` | PK `id`. `name` is not unique. |
| `mappings.institutions_ars` | ARS-side institution names. | `id`, `name` | PK `id`. `name` is not unique. |
| `mappings.institutions_mapping` | Mapping between Specify and ARS institution names. | `id`, `institution_specify_id`, `institution_ars_id` | FKs to both institution name tables; unique `institution_ars_id`. Each ARS institution name maps to at most one Specify institution name. One Specify institution name can be mapped from multiple ARS names. |
| `mappings.collections_specify` | Specify-side collection names. | `id`, `name` | PK `id`. `name` is not unique. |
| `mappings.collections_ars` | ARS-side collection names. | `id`, `name` | PK `id`. `name` is not unique. |
| `mappings.collections_mapping` | Mapping between Specify and ARS collection names. | `id`, `collection_specify_id`, `collection_ars_id` | FKs to both collection name tables; unique `collection_ars_id`. Each ARS collection name maps to at most one Specify collection name. One Specify collection name can be mapped from multiple ARS names. |

## Notes

No mapping seed rows were found in the main changelog. The schema appears to be populated externally or through application workflows outside the Liquibase default seeds.

The readonly user grants created in `changelog-1.0.0.xml` do not include `mappings`, because this schema is created later and no additional grants are added in `changelog-2.0.0.xml`.
