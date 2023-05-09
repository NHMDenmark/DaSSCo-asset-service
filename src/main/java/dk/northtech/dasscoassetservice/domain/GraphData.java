package dk.northtech.dasscoassetservice.domain;

import java.util.Map;

public class GraphData {
    Map<String, Integer> institutes;
    Map<String, Integer> pipelines;
    Map<String, Integer> workstations;

    public GraphData(Map<String, Integer> institutes, Map<String, Integer> pipelines, Map<String, Integer> workstations) {
        this.institutes = institutes;
        this.pipelines = pipelines;
        this.workstations = workstations;
    }

    public Map<String, Integer> getInstitutes() {
        return institutes;
    }

    public void setInstitutes(Map<String, Integer> institutes) {
        this.institutes = institutes;
    }

    public void addInstituteAmts(String instituteName, Integer amount) {
        if (this.institutes.containsKey(instituteName)) {
            this.institutes.put(instituteName, this.institutes.get(instituteName) + amount);
        }
    }

    public Map<String, Integer> getPipelines() {
        return pipelines;
    }

    public void setPipelines(Map<String, Integer> pipelines) {
        this.pipelines = pipelines;
    }

    public void addPipelineAmts(String pipelineName, Integer amount) {
        if (this.pipelines.containsKey(pipelineName)) {
            this.pipelines.put(pipelineName, this.pipelines.get(pipelineName) + amount);
        }
    }

    public Map<String, Integer> getWorkstations() {
        return workstations;
    }

    public void setWorkstations(Map<String, Integer> workstations) {
        this.workstations = workstations;
    }

    public void addWorkstationAmts(String workstationName, Integer amount) {
        if (this.workstations.containsKey(workstationName)) {
            this.workstations.put(workstationName, this.workstations.get(workstationName) + amount);
        }
    }

    @Override
    public String toString() {
        return "GraphData{" +
                "institutions=" + institutes +
                ", pipelines=" + pipelines +
                ", workstations=" + workstations +
                '}';
    }
}
