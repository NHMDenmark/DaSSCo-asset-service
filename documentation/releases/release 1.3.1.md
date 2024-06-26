**Release 1.3.1** 
-
2024-04-12

**API Changes:**
-
- Delete uploaded file if CRC check fails.
  -If the failed file was overwriting an existing file, the existing file will be recreated.
- proxy_allocation_status_text renamed to allocation_status_text to match asset service naming.
- Include asset allocation size when getting status for single asset
 
**Bugfixes:**
-
- Add missing share_allocation_mb to asset status.
- Reject allocation if existing files and parent files take up too much space.
- Reject negative storage allocations.
- Fix internal error when getting in progress assets
- Fix assets being shared multiple times
  - Remove duplicated shared assets from test database
 
**Improvements:**
-
- Better error messages when file proxy is down.
- 
