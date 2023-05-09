package dk.northtech.dasscoassetservice.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class GraphData {
    Map<String, Integer> institutions;
    Map<String, Integer> pipelines;
    Map<String, Integer> workstations;

    public GraphData(Map<String, Integer> institutions, Map<String, Integer> pipelines, Map<String, Integer> workstations) {
        this.institutions = institutions;
        this.pipelines = pipelines;
        this.workstations = workstations;
    }

    public Map<String, Integer> getInstitutions() {
        return institutions;
    }

    public void setInstitutions(Map<String, Integer> institutions) {
        this.institutions = institutions;
    }

    public Map<String, Integer> getPipelines() {
        return pipelines;
    }

    public void setPipelines(Map<String, Integer> pipelines) {
        this.pipelines = pipelines;
    }

    public Map<String, Integer> getWorkstations() {
        return workstations;
    }

    public void setWorkstations(Map<String, Integer> workstations) {
        this.workstations = workstations;
    }

    @Override
    public String toString() {
        return "GraphData{" +
                "institutions=" + institutions +
                ", pipelines=" + pipelines +
                ", workstations=" + workstations +
                '}';
    }
}
