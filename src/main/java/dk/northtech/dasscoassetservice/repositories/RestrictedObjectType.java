package dk.northtech.dasscoassetservice.repositories;

public enum RestrictedObjectType {
    ASSET("asset","asset_guid")
    , COLLECTION("Collection","collection_id")
    , GROUP("asset_group","asset_group_id")
    , INSTITUTION("institution","institution_name")
    , SPECIMEN("specimen","specimen_barcode");
    public final String objectName;
    public final String identifierName;

    RestrictedObjectType(String objectName, String identifierName) {
        this.identifierName = identifierName;
        this.objectName = objectName;
    }



}
