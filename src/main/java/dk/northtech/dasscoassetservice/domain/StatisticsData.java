package dk.northtech.dasscoassetservice.domain;

public record StatisticsData(
        String instituteName,
        String pipelineName,
        String workstationName,
        String createdDate, // event's timestamp
        Integer specimens // number of specimens in the specific asset
) {
    public StatisticsData(String instituteName, String pipelineName, String workstationName, String createdDate, Integer specimens) {
        this.instituteName = instituteName.replaceAll("\"", "");
        this.pipelineName = pipelineName != null ? pipelineName.replaceAll("\"", "") : null;
        this.workstationName = workstationName != null ? workstationName.replaceAll("\"", "") : null;
        this.createdDate = createdDate.replaceAll("\"", "");
        this.specimens = specimens;
    }
}
