package dk.northtech.dasscoassetservice.services;

import com.google.common.base.Strings;
import dk.northtech.dasscoassetservice.repositories.EnumRepository;
import jakarta.inject.Inject;
import org.jdbi.v3.core.Jdbi;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ExtendableEnumService {
    private Jdbi jdbi;
    private Map<String, String> statusCache = new ConcurrentHashMap<>();

    private Map<String, String> fileFormatCache = new ConcurrentHashMap<>();
    private Map<String, String> subjectCache = new ConcurrentHashMap<>();

    private Map<String, String> issueCategoryCache = new ConcurrentHashMap<>();
    private Map<String, String> preparationTypeCache = new ConcurrentHashMap<>();

    @Inject
    public ExtendableEnumService(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    public enum ExtendableEnum {
        FILE_FORMAT("file_format"),
        ISSUE_CATEGORY("issue_category"),
        STATUS("asset_status"),
        SUBJECT("subject"),
        PREPARATION_TYPE("preparation_type");

        ExtendableEnum(String enumName) {
            this.enumName = enumName;
        }

        public final String enumName;

    }

    public Set<String> getFileFormats() {
        if (fileFormatCache.isEmpty()) {
            initCache(ExtendableEnum.FILE_FORMAT);
        }
        return new HashSet<>(this.fileFormatCache.values());
    }

    public Set<String> getStatuses() {
        if (statusCache.isEmpty()) {
            initCache(ExtendableEnum.STATUS);
        }
        return new HashSet<>(this.statusCache.values());
    }

    public Set<String> getSubjects() {
        if (subjectCache.isEmpty()) {
            initCache(ExtendableEnum.SUBJECT);
        }
        return new HashSet<>(this.subjectCache.values());
    }

    public Set<String> getPreparation_types() {
        return new HashSet<>(this.getPreparationTypeCache().values());
    }


    public Set<String> getIssueCategories() {
        if (issueCategoryCache.isEmpty()) {
            initCache(ExtendableEnum.ISSUE_CATEGORY);
        }
        return new HashSet<>(this.issueCategoryCache.values());
    }

    public Map<String, String> getFileFormatCache() {
        if (fileFormatCache.isEmpty()) {
            initCache(ExtendableEnum.FILE_FORMAT);
        }
        return fileFormatCache;
    }

    public Map<String, String> getPreparationTypeCache() {
        if (preparationTypeCache.isEmpty()) {
            initCache(ExtendableEnum.PREPARATION_TYPE);
        }
        return preparationTypeCache;
    }

    public Map<String, String> getStatusCache() {
        if (statusCache.isEmpty()) {
            initCache(ExtendableEnum.STATUS);
        }
        return statusCache;
    }

    public void initCache(ExtendableEnum extendableEnum) {
        List<String> strings = jdbi.withHandle(handle -> {
            EnumRepository attach = handle.attach(EnumRepository.class);
            return attach.listEnum(extendableEnum);
        });
        strings.forEach(s -> {
            switch (extendableEnum) {
                case FILE_FORMAT -> {
                    this.fileFormatCache.put(s,s);
                }
                case SUBJECT -> {
                    this.subjectCache.put(s,s);
                }
                case STATUS -> {
                    this.statusCache.put(s,s);
                }
                case ISSUE_CATEGORY -> {
                    this.issueCategoryCache.put(s,s);
                }
                case PREPARATION_TYPE -> {
                    this.preparationTypeCache.put(s,s);
                }
            }
        });
    }


    public void persistEnum(ExtendableEnum extendableEnum, String value) {
        if (Strings.isNullOrEmpty(value)) {
            throw new IllegalArgumentException("Name cannot be null");
        }
        switch (extendableEnum) {
            case FILE_FORMAT -> {
                if (getFileFormats().contains(value)) {
                    throw new IllegalArgumentException("File format already exists");
                }
            }
            case SUBJECT -> {
                if (getSubjects().contains(value)) {
                    throw new IllegalArgumentException("Subject already exists");
                }
            }
            case STATUS -> {
                if (getStatuses().contains(value)) {
                    throw new IllegalArgumentException("Status already exists");
                }
            }
            case ISSUE_CATEGORY -> {
                if(getIssueCategories().contains(value)) {
                    throw new IllegalArgumentException("Issue category already exists");

                }
            }
            case PREPARATION_TYPE -> {
                if(getPreparation_types().contains(value)) {
                    throw new IllegalArgumentException("Preparation_type already exists");

                }
            }
        }
        jdbi.withHandle(h -> {
            EnumRepository repository = h.attach(EnumRepository.class);
            repository.persistEnum(extendableEnum, value);
            return h;
        });
        switch (extendableEnum) {
            case FILE_FORMAT -> {
                fileFormatCache.put(value,value);
            }
            case SUBJECT -> {
                subjectCache.put(value,value);
            }
            case STATUS -> {
                statusCache.put(value,value);
            }
            case ISSUE_CATEGORY -> {
                issueCategoryCache.put(value,value);
            }
            case PREPARATION_TYPE -> {
                preparationTypeCache.put(value,value);
            }
        }
    }

    public boolean checkExists(ExtendableEnum extendableEnum, String value) {
        switch (extendableEnum) {
            case FILE_FORMAT -> {
                return getFileFormats().contains(value);
            }
            case STATUS -> {
                return getStatuses().contains(value);
            }
            case SUBJECT -> {
                return getSubjects().contains(value);
            }
            case ISSUE_CATEGORY -> {
                return getIssueCategories().contains(value);
            }
            case PREPARATION_TYPE -> {
                return getPreparation_types().contains(value);
            }
            default -> {
                throw new RuntimeException("Enum name is null");
            }
        }
    }
    public void updateEnum(ExtendableEnum extendableEnum, String existing, String new_name) {
        if (Strings.isNullOrEmpty(new_name)) {
            throw new IllegalArgumentException(extendableEnum.enumName + " cannot be null");
        }
        if(!checkExists(extendableEnum, existing)) {
            throw new IllegalArgumentException(extendableEnum.enumName + " doesnt exist");
        }
        jdbi.withHandle(h -> {
            EnumRepository repository = h.attach(EnumRepository.class);
            repository.updateEnum(extendableEnum, existing, new_name);
            return h;
        });
        switch (extendableEnum) {
            case FILE_FORMAT -> {
                this.fileFormatCache.remove(existing);
                this.fileFormatCache.put(new_name, new_name);
            }
            case STATUS -> {
                this.statusCache.remove(existing);
                this.statusCache.put(new_name, new_name);
            }
        }
    }
}
