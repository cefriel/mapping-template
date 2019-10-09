package it.cefriel.template;

import nu.xom.Builder;
import nu.xom.ParsingException;
import nu.xom.Serializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Utils {

    // Remove Prefix
    public String rp(String s) {
        if (s!=null)
            if (s.contains("#")) {
                String[] split = s.split("#");
                return split[1];
            }
        return s;
    }
    // Remove Prefix through regex
    public String sp(String s, String regex) {
        if (s!=null) {
            String[] split = s.split(regex);
            if (split.length > 1)
                return split[1];
        }
        return s;
    }

    // Get timestamp
    public static String getTimestamp() {
        DateTimeFormatter formatterOutput = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        return LocalDateTime.now().format(formatterOutput);
    }

    public static String format(String xml) throws ParsingException, IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Serializer serializer = new Serializer(out);
        serializer.setIndent(4);  // or whatever you like
        serializer.write(new Builder().build(xml, ""));
        return out.toString("UTF-8");
    }

}
