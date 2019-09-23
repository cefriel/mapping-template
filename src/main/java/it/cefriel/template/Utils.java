package it.cefriel.template;

public class Utils {

    // Remove Prefix
    public String rp(String s) {
        if (s.contains("#")) {
            String[] split = s.split("#");
            return split[1];
        }
        return s;
    }

}
