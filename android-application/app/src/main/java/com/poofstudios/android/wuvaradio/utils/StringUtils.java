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
        String[] words = string.split(" ");
        String result = "";
        for(String word : words) {
            result += word.substring(0, 1).toUpperCase() + word.substring(1) + " ";
        }
        return result.trim();
    }
}
