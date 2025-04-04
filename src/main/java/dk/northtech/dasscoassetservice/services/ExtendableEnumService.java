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

    @Inject
    public ExtendableEnumService(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    public enum ExtendableEnum {
        FILE_FORMAT("file_format"),
        ISSUE_NAME("issue_name"),
        STATUS("asset_status");

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

    public Map<String, String> getFileFormatCache() {
        if (fileFormatCache.isEmpty()) {
            initCache(ExtendableEnum.FILE_FORMAT);
        }
        return fileFormatCache;
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
                case STATUS -> {
                    this.statusCache.put(s,s);
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
            case STATUS -> {
                if (getStatuses().contains(value)) {
                    throw new IllegalArgumentException("File format already exists");
                }
            }
        }
        jdbi.withHandle(h -> {
            EnumRepository repository = h.attach(EnumRepository.class);
            repository.persistEnum(ExtendableEnum.FILE_FORMAT, value);
            return h;
        });
        switch (extendableEnum) {
            case FILE_FORMAT -> {
                fileFormatCache.put(value,value);
            }
            case STATUS -> {
                statusCache.put(value,value);
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

    public void deleteFileFormat(ExtendableEnum extendableEnum, String format) {
        jdbi.withHandle(h -> {
            EnumRepository repository = h.attach(EnumRepository.class);
            repository.deleteEnum(extendableEnum,format);
            return h;
        });
    }
}
