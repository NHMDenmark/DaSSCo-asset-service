package dk.northtech.dasscoassetservice.domain.bulkupdatepayload;

import dk.northtech.dasscoassetservice.domain.Legality;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public record BulkUpdatePayload(
        List<String> assetGuids,
        Map<String, Object> fields,
        IssuePatchBlock issues,
        DigitiserPatchBlock digitisers,
        Optional<Legality> legality
) {}