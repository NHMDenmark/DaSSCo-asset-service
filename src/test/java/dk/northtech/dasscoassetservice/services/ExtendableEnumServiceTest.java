package dk.northtech.dasscoassetservice.services;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class ExtendableEnumServiceTest extends AbstractIntegrationTest {
    @Test
    void getFileFormats() {
        Set<String> result = extendableEnumService.getFileFormats();
        assertThat(result).isNotEmpty();
    }

    @Test
    void getStatuses() {
        Set<String> result = extendableEnumService.getStatuses();
        assertThat(result).isNotEmpty();
    }

    @Test
    void persistsEnum() {
        extendableEnumService.persistEnum(ExtendableEnumService.ExtendableEnum.FILE_FORMAT, "GML");
        Set<String> fileFormats = extendableEnumService.getFileFormats();
        assertThat(fileFormats).contains("GML");
        extendableEnumService.persistEnum(ExtendableEnumService.ExtendableEnum.STATUS, "NEW_status");
        Set<String> statuses = extendableEnumService.getStatuses();
        assertThat(statuses).contains("NEW_status");
    }

    @Test
    void updateEnum() {
        extendableEnumService.updateEnum(ExtendableEnumService.ExtendableEnum.FILE_FORMAT, "TXT", "DOCX");
        Set<String> fileFormats = extendableEnumService.getFileFormats();
        assertThat(fileFormats).contains("DOCX");
        assertThat(fileFormats).doesNotContain("TXT");
        //Check that it is actually updated
        extendableEnumService.initCache(ExtendableEnumService.ExtendableEnum.FILE_FORMAT);
        assertThat(fileFormats).contains("DOCX");
    }

    @Test
    void updateEnumStatus() {
        extendableEnumService.updateEnum(ExtendableEnumService.ExtendableEnum.STATUS, "ARCHIVE", "ARCHIVED");
        Set<String> fileFormats = extendableEnumService.getStatuses();
        assertThat(fileFormats).contains("ARCHIVED");
        assertThat(fileFormats).doesNotContain("ARCHIVE");
        //Check that it is actually updated
        extendableEnumService.initCache(ExtendableEnumService.ExtendableEnum.STATUS);
        assertThat(fileFormats).contains("ARCHIVED");
    }

    @Test
    void updateEnumDoesNotExist() {
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> extendableEnumService.updateEnum(ExtendableEnumService.ExtendableEnum.STATUS, "ARCHYVE", "ARCHIVED"));
        assertThat(illegalArgumentException.getMessage()).isEqualTo("asset_status doesnt exist");
        IllegalArgumentException illegalArgumentExceptionFF = assertThrows(IllegalArgumentException.class, () -> extendableEnumService.updateEnum(ExtendableEnumService.ExtendableEnum.FILE_FORMAT, "MP3", "MP4"));
        assertThat(illegalArgumentExceptionFF.getMessage()).isEqualTo("file_format doesnt exist");

    }

    @Test
    void getCaches() {
        Map<String, String> fileFormatCache = extendableEnumService.getFileFormatCache();
        assertThat(fileFormatCache.containsKey("TIF")).isTrue();
        Map<String, String> statusCache = extendableEnumService.getStatusCache();
        assertThat(statusCache.containsKey("WORKING_COPY")).isTrue();

    }
}