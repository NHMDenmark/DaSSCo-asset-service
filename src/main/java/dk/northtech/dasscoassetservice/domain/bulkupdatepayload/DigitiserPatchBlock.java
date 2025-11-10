package dk.northtech.dasscoassetservice.domain.bulkupdatepayload;

import java.util.List;

public record DigitiserPatchBlock(
        List<DigitiserAddition> add,
        List<Integer> delete
) {}
