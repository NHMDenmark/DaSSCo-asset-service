### Protocol: Event

An event is something that has changed an Asset or its metadata. 

| Fieldname Asset service | Desription                                    | Datatype                                                                                                                                                                                                  | Required | Notes |
| ----------------------- | --------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------- | ----- |
| pipeline                | Name of the pipeline that started the event   | String                                                                                                                                                                                                    | no       |       |
| workstation             | Name of the workstation that was used         | String                                                                                                                                                                                                    | no       |       |
| user                    | Username of the user that initiated the event | String                                                                                                                                                                                                    | yes      |       |
| event                   | What happened to the asset.                   | String enumeration, <br>CREATE_ASSET, <br>UPDATE_ASSET,<br>DELETE_ASSET,<br>AUDIT_ASSET, <br>CREATE_ASSET_METADATA,<br>UPDATE_ASSET_METADATA,<br>DELETE_ASSET_METADATA,<br>BULK_UPDATE_ASSET_METADATA<br> | no       |       |
| (internal) asset_guid   | The asset is connected to an event.           |                                                                                                                                                                                                           | no       |       |
