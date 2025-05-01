## ARS to Specify metadata mapping

### NHMD :
| ARS field | Specify attachments field name | Specify attachements field caption | Specify attachments field value | Notes |        
|---|---|---|---|---|
| asset_guid and file_formats | origFilename | File Name | asset_guid + "." + file_formats[0] | This needs to be concatenated from the asset_guid, and the value in file formats first entry in lower case with a dot in between. |
| asset_pid | attachmentLocation | Persistent Identifier |one-to-one mapping | - |
| date_asset_deleted | copyrightDate | Date Media Deleted | one-to-one mapping | - |
| date_asset_taken | fileCreatedDate | Date Media Created | one-to-one mapping | - |
| file_formats | mimeType | File Formats | First entry in the ARS list is mapped here in lowercase. | Specify takes only one value here. ARS is using a list. The first entry in the fileformats list should be used here and it should be turned to lowercase. |
| legality.copyright | copyrightHolder | Copyright Holder | one-to-one mapping | This comes from the legality object in the ARS metadata. |
| legality.credit | credit | Credit | one-to-one mapping | This comes from the legality object in the ARS metadata. |
| legality.license | license | License | one-to-one mapping | This comes from the legality object in the ARS metadata. |
| make_public | isPublic | Make Public | one-to-one mapping | - |
| specify_attachment_remarks | remarks | Remarks | one-to-one mapping | - |
| specify_attachment_title | title | Type/description of attachment | one-to-one mapping | - | 