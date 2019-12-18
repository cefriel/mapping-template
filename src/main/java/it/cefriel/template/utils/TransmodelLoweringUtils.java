package it.cefriel.template.utils;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.ParsingException;
import nu.xom.Serializer;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
