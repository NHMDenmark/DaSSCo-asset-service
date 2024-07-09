package dk.northtech.dasscoassetservice.services;


import dk.northtech.dasscoassetservice.cache.SubjectCache;
import jakarta.inject.Inject;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SubjectService {

    private SubjectCache subjectCache;

    @Inject
    public SubjectService (SubjectCache subjectCache){
        this.subjectCache = subjectCache;
    }

    public List<String> listSubjects(){
        return subjectCache.getSubjects();
    }
}
