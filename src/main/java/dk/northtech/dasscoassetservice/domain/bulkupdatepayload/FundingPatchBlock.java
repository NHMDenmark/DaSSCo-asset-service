package dk.northtech.dasscoassetservice.domain.bulkupdatepayload;

import java.util.List;

public record FundingPatchBlock(
        List<Integer> add,
        List<Integer> delete
) {}
