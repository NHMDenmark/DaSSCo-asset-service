package dk.northtech.dasscoassetservice.domain.bulkupdatepayload;

import java.util.List;

public record RoleRestrictionPatchBlock(
        List<String> add,
        List<String> delete
) {}
