package dk.northtech.dasscoassetservice.domain.bulkupdatepayload;

import java.util.List;

public record IssuePatchBlock(
        List<IssueAddition> add,
        List<IssueUpdate> update,
        List<Integer> delete
) {}
