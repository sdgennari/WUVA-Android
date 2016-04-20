package com.poofstudios.android.wuvaradio.utils;

public class StringUtils {

    public static String capitalizeEveryWord(String string) {
        if (string == null) {
            return null;
        }
        if (string.isEmpty()) {
            return string;
        }

        string = string.toLowerCase();

        // Handle f/<name> and w/<name> explicitly
        string = string.replaceAll("f/", "f/ ").replaceAll("w/", "w/ ");

        String[] words = string.split(" ");
        String result = "";
        for(String word : words) {
            if (word.isEmpty()) {
                continue;
            } else if (word.equals("f/") || word.equals("w/")) {
                result += String.format("%s ", word);
            } else {
                result += String.format("%s%s ", word.substring(0, 1).toUpperCase(), word.substring(1));
            }
        }
        return result.trim();
    }
}
