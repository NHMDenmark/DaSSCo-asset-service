package dk.northtech.dasscoassetservice.cache;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class SubjectCache {

    private final ConcurrentHashMap<String, String> subjectMap = new ConcurrentHashMap<>();

    public ConcurrentHashMap<String, String> getSubjectMap() {
        return subjectMap;
    }

    public List<String> getSubjects() {
        return subjectMap.values().stream().toList();
    }

    public void putSubjectsInCacheIfAbsent(String subject) {
        subjectMap.putIfAbsent(subject, subject);
    }

    public void clearCache(){
        this.subjectMap.clear();
    }
}
