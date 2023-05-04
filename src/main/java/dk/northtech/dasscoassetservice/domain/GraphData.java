package dk.northtech.dasscoassetservice.domain;

import java.util.List;

public record GraphData(
        List<String> labels,
        List<Integer> data,
        String dataLabel,
        GraphLabelType type
) {
}
