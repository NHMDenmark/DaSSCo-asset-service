package dk.northtech.dasscoassetservice.domain.bulkupdatepayload;

import java.util.List;

public record DigitiserAddition(
        Integer dasscoUserId,
        List<String> assetGuids
) {}