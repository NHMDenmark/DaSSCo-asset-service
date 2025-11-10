package dk.northtech.dasscoassetservice.domain.bulkupdatepayload;

import java.util.List;
import java.util.Map;

public record IssueUpdate(
        List<Integer> issueIds,
        Map<String, Object> values
) {}