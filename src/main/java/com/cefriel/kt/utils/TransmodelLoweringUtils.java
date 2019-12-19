package com.cefriel.kt.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TransmodelLoweringUtils extends LoweringUtils {

    public static DateTimeFormatter formatterOutput = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    // Get timestamp
    public String getTimestamp() {
        return LocalDateTime.now().format(formatterOutput);
    }

    // Get formatted date
    public String getFormattedDate(int year, int month, int dayOfMonth, int hour, int minute) {
        LocalDateTime dt = LocalDateTime.of(0,0,0,0,0);
        return dt.format(formatterOutput);
    }

    // Format GTFS date
    public String formatGTFSDate(String dateString) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        DateTimeFormatter formatterOutput = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return LocalDate.parse(dateString, formatter).format(formatterOutput);
    }

}
