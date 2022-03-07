/*
 * Copyright (c) 2019-2021 Cefriel.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cefriel.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TransmodelTemplateUtils extends TemplateUtils {

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
