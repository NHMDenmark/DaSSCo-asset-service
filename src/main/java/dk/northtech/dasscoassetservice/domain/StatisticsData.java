package dk.northtech.dasscoassetservice.domain;

public record StatisticsData(
        String instituteName,
        String pipelineName,
        String workstationName,
        Long createdDate, // event's error_timestamp in millis
        Integer specimens // number of specimens in the specific asset
) {
    public StatisticsData(String instituteName, String pipelineName, String workstationName, Long createdDate, Integer specimens) {
        this.instituteName = instituteName.replaceAll("\"", "");
        this.pipelineName = pipelineName != null ? pipelineName.replaceAll("\"", "") : null;
        this.workstationName = workstationName != null ? workstationName.replaceAll("\"", "") : null;
        this.createdDate = createdDate;
        this.specimens = specimens;
    }
}
