# Large file upload guide

This guide explains how large files are uploaded through the file proxy using **TUS**.

TUS is a resumable upload protocol. If an upload is interrupted, the client can continue from where it stopped instead of starting over.

## Endpoint

```text
/file_proxy/api/large-files/{institutionName}/{collectionName}/{assetGuid}/upload
```

| Path value | Meaning |
| --- | --- |
| `institutionName` | Institution that owns or manages the asset |
| `collectionName` | Collection the asset belongs to |
| `assetGuid` | Unique asset identifier |

Example:

```text
/file_proxy/api/large-files/NHM/Birds/123e4567-e89b-12d3-a456-426614174000/upload
```

## What the client must send

When starting an upload, the client must send:

| Header | Purpose |
| --- | --- |
| `Upload-Length` | The total file size in bytes |
| `Upload-Metadata` | File information, such as file name and destination path |

Important metadata values:

| Metadata | Meaning |
| --- | --- |
| `filename` | Name of the uploaded file |
| `path` | Where the file should be stored under the asset |

The metadata values are Base64 encoded, as required by TUS.

## Upload flow

### 1. Start the upload

The client sends a `POST` request to the endpoint.

The file proxy then:

1. Checks that `Upload-Length` is present.
2. Checks whether the asset has enough available storage.
3. Creates a TUS upload session.
4. Returns a `Location` header.
5. Registers the upload as active.

The `Location` header is the unique URL for this upload. The client uses it for the rest of the upload.

If the file size is missing, or if there is not enough storage, the upload is rejected before file data is accepted.

### 2. Upload the file data

The client sends the actual file content to the `Location` URL using `PATCH` requests.

The file may be sent in chunks. This is useful for very large files.

### 3. Resume if interrupted

If the upload is interrupted, the client sends a `HEAD` request to the `Location` URL.

The server responds with how many bytes it has already received. The client then continues from that point.

### 4. Cancel if needed

The client can cancel an upload with a `DELETE` request to the `Location` URL.

The file proxy then removes the upload from the active upload list.

### 5. Finish the upload

When all expected bytes have been received, the file proxy automatically finalizes the upload.

It then:

1. Reads the completed file from temporary TUS storage.
2. Reads the upload metadata, including the file path.
3. Calculates the file size in megabytes.
4. Detects the content type from the file path/name.
5. Sends the file to the normal large file upload service.
6. Removes the upload from the active upload list.
7. Deletes the temporary TUS upload data.

After this, the file is stored as a normal asset file.

## Supported actions

| Method | Purpose |
| --- | --- |
| `POST` | Start a new upload |
| `PATCH` | Upload file data |
| `HEAD` | Check upload progress |
| `DELETE` | Cancel an upload |
| `OPTIONS` | Check supported TUS features |
| `PUT` | Passed through to the TUS service if used by a client |

Most clients only need `POST`, `PATCH`, and `HEAD`.

## Temporary storage

While the upload is in progress, the file is stored temporarily here:

```text
{project root}/{mount folder}/tus
```

The mount folder comes from application configuration.

Temporary upload data is deleted after the file has been successfully handed over to the file service.

## User information

The upload uses the currently authenticated user.

When the completed file is stored, the user’s Keycloak ID is sent to the file service so the system knows who performed the upload.

## Example flow

1. A user selects a large file for an asset.
2. The client asks the file proxy to start an upload and sends the file size.
3. The file proxy checks that there is enough storage.
4. The file proxy creates an upload session and returns a `Location` URL.
5. The client uploads the file in one or more chunks.
6. If the upload stops, the client asks how much was received and continues from there.
7. When the file is complete, the file proxy stores it as an asset file.
8. Temporary upload data is cleaned up.

## Simple curl example

### Start upload

```bash
curl -i -X POST \
  /file_proxy/api/large-files/NHM/Birds/123e4567-e89b-12d3-a456-426614174000/upload \
  -H "Tus-Resumable: 1.0.0" \
  -H "Upload-Length: 524288000" \
  -H "Upload-Metadata: filename YmlyZC5qcGc=,path aW1hZ2VzL2JpcmQuanBn"
```

Example metadata:

| Metadata | Plain value | Base64 value |
| --- | --- | --- |
| `filename` | `bird.jpg` | `YmlyZC5qcGc=` |
| `path` | `images/bird.jpg` | `aW1hZ2VzL2JpcmQuanBn` |

The response contains a `Location` header.

### Upload file data

```bash
curl -i -X PATCH \
  {Location-from-previous-response} \
  -H "Tus-Resumable: 1.0.0" \
  -H "Content-Type: application/offset+octet-stream" \
  -H "Upload-Offset: 0" \
  --data-binary @bird.jpg
```

### Check progress

```bash
curl -i -X HEAD \
  {Location-from-previous-response} \
  -H "Tus-Resumable: 1.0.0"
```

### Cancel upload

```bash
curl -i -X DELETE \
  {Location-from-previous-response} \
  -H "Tus-Resumable: 1.0.0"
```

## Common issues

| Issue | Result |
| --- | --- |
| Missing `Upload-Length` | Upload is rejected |
| Not enough storage | Upload is rejected before it starts |
| Missing `path` metadata | File may not be registered at the intended destination |
| Interrupted connection | Client should resume using the current upload offset |

## Note about content type

The content type is detected from the file path/name, for example from `.jpg` or `.png`. It is not based on reading the full file contents.
