package dk.northtech.dasscoassetservice.repositories;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

class AssetRepositoryTest {

    @Test
    void persistAsset() {
        Instant now = Instant.now();
        String format = DateTimeFormatter.ISO_INSTANT.format(now);
        System.out.println(format);
    }

}