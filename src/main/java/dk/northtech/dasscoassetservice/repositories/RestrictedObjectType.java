package dk.northtech.dasscoassetservice.repositories;

public enum RestrictedObjectType {
    ASSET("Asset","guid")
    , COLLECTION("Collection","name")
    , GROUP("Group","name")
    , INSTITUTION("Institution","name")
    , SPECIMEN("Specimen","specimen_barcode");
    public final String objectName;
    public final String identifierName;

    RestrictedObjectType(String objectName, String identifierName) {
        this.identifierName = identifierName;
        this.objectName = objectName;
    }
}
