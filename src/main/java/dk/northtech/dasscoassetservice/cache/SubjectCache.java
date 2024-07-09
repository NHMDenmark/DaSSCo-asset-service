package dk.northtech.dasscoassetservice.cache;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class SubjectCache {

    private Map<String, String> subjectMap = new HashMap<>();

    public Map<String, String> getSubjectMap() {
        return subjectMap;
    }

    public void setSubjectMap(Map<String, String> subjectMap) {
        this.subjectMap = subjectMap;
    }

    public List<String> getSubjects() {
        return subjectMap.values().stream().toList();
    }

    public void putSubjectsInCache(String subject) {
        subjectMap.put(subject, subject);
    }
}
