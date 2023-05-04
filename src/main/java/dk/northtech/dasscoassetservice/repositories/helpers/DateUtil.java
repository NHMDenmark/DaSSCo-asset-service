package dk.northtech.dasscoassetservice.repositories.helpers;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;

public class DateUtil {
    public static Instant format(String isoZ) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ssZ");
        TemporalAccessor parse = dateTimeFormatter.parse("2017-08-27T17:43:11Z");
        return null;
    }
}
