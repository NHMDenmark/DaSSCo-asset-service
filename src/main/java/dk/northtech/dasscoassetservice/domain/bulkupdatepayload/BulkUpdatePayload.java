package dk.northtech.dasscoassetservice.domain.bulkupdatepayload;

import java.util.List;
import java.util.Map;

public record BulkUpdatePayload(
        List<String> assetGuids,
        Map<String, Object> fields,
        IssuePatchBlock issues,
        DigitiserPatchBlock digitisers
) {}