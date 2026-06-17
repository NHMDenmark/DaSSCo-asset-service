# Bundle Download Size Limit Design

## Context

Asset bundle downloads are started from the Angular app by posting selected asset GUIDs to file-proxy bundle job endpoints. The existing frontend flow then polls the job until it is ready and downloads the generated ZIP.

Large selections can ask file-proxy to prepare hundreds or thousands of assets. If the selected files total hundreds of GB or more, the bundle job can exhaust disk, CPU, network, or memory resources and destabilize file-proxy or adjacent services.

The file-proxy database already stores file sizes in the `file` table. That makes file-proxy the correct service to enforce an aggregate bundle size limit before any download or ZIP preparation work starts.

## Goal

Prevent oversized asset bundle jobs from starting, and give users a clear message that they must reduce the selection.

## Non-Goals

- Do not rely on the Angular UI as the only protection.
- Do not estimate bundle size from file formats or asset counts.
- Do not add asynchronous cleanup as the main safeguard for oversized bundle jobs.
- Do not change single-file download behavior unless it shares the same bundle job path.

## Recommended Approach

File-proxy enforces a configurable maximum total bundle size at bundle job creation time.

Before accepting a bundle job, file-proxy calculates the selected files' aggregate size from the `file` table:

```sql
select coalesce(sum(size_bytes), 0)
from file
where asset_guid = any(:assetGuids)
  and delete_after_sync = false;
```

If the aggregate exceeds the configured maximum, file-proxy rejects the request and does not create a job. Angular displays the returned message through the existing bundle download status UI.

## Alternatives Considered

### UI-only pre-check

This gives fast feedback, but it is not sufficient because callers can bypass Angular and post directly to file-proxy. It also requires exposing reliable size data to Angular.

### Asset-count limit only

This is simple, but it does not match the actual risk. A small number of large RAW/TIFF assets can be worse than many small JPEG assets.

### Post-start cancellation

Cancelling after job creation still allows expensive work to begin. This reduces damage but does not prevent service pressure.

## API Behavior

The bundle job creation endpoints should reject oversized requests with `413 Payload Too Large`.

Suggested response body:

```json
{
  "message": "Selected bundle is too large. Select fewer assets.",
  "totalSizeBytes": 1319413953331,
  "maxSizeBytes": 107374182400,
  "assetCount": 1000
}
```

The same check should apply to internal and external bundle endpoints unless a later product decision introduces role-based limits.

## Configuration

Add a file-proxy configuration value for maximum bundle size, for example:

```properties
asset-bundles.max-size-bytes=107374182400
```

The production value should be chosen based on available disk, expected concurrent usage, and acceptable transfer times. The implementation should use bytes internally to avoid MB/GB rounding mistakes.

## Angular Behavior

The Angular `AssetBundleDownloadService` should continue to post selected GUIDs to file-proxy. If file-proxy returns an oversized-bundle error, Angular should mark the download as failed and show the server message.

The user-facing message should include the selected total and configured maximum when those fields are present:

```text
Selected bundle is 1.2 TB. Maximum allowed is 100 GB. Select fewer assets.
```

An optional preflight endpoint can be added later for earlier warning in dialogs or disabled buttons, but the enforced server-side check must remain in job creation.

## Additional Safeguards

After the size limit is in place, consider adding:

- Maximum asset count per bundle job.
- Maximum concurrent bundle jobs.
- Maximum total in-flight bundle bytes.
- Metrics for rejected oversized jobs and active bundle workload.

These are secondary controls. The aggregate size limit is the first required safeguard.

## Testing

File-proxy tests should cover:

- A bundle below the configured size limit creates a job.
- A bundle exactly at the configured size limit creates a job.
- A bundle above the configured size limit returns `413` and creates no job.
- Deleted files are excluded from the aggregate.
- Multiple files for the same asset are all included.
- Missing file rows are handled deliberately, either by counting as zero or rejecting as invalid.

Angular tests should cover:

- Oversized-bundle responses show a clear failed download message.
- Existing generic bundle preparation errors still display correctly.

## Open Decisions

- The exact production maximum bundle size.
- Whether missing file rows should be treated as zero bytes or as a validation error.
- Whether internal/admin users should eventually have a higher limit than external users.
