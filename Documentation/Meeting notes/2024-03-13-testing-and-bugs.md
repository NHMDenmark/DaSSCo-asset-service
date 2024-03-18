**23 Feb 2024, at 15.40,** 
- 
Thomas Bornerup <thomas@northtech.dk> wrote: 

**API changes**
-
- Don’t attempt to download files from ERDA when creating new asset that has no parent.
- Make parent download optional when opening share through the Open Asset endpoint.
- Add delete share endpoint and document it.
- I have changed the name from close share to delete share to better reflect the fact that the files in the share will be deleted and not synchronized to ERDA
 
**Bugs**
-
- Find out is causing issues when creating shares during metadata upload.
- Use correct HTTP-codes on asset upload and other services, 201 on creating, 503 on upstream error, 500 on unknown error etc.
- Do not allow opening shares for an asset_guid does not exist.
- Do not allow opening shares if URL-parameter and POST body doesn’t match, return 400 error instead.
 
**Documentation**
-
- Document error codes
- Document status codes
- Add urls in confluence api doc
- Document workflows
- Document API for downloading asset events
 







2024-04-13 Testing and bugs
- 
- Northtech will create a test suite with integration tests.
- We start reporting bugs as GitHub issues.
- We will start storing meeting notes in GitHub


Updates to fileproxy
-
- New status for reopened asset that has files in ERDA
- Remove uploaded file it CRC doesn't match (feedback from Bhupjit)
- Health checks

Documentation updates
- 
- Document sync status for file info API
- Move documentation to GitHub
