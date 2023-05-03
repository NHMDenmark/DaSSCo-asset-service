package dk.northtech.dasscoassetservice.repositories;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;

class AssetRepositoryTest {

    @Test
    void persistAsset() {
        Instant now = Instant.now();
        String format = DateTimeFormatter.ISO_INSTANT.format(now);
        System.out.println(format);
    }



}