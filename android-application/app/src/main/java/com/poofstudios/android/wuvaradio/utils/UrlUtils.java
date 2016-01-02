package com.poofstudios.android.wuvaradio.utils;

import android.util.Log;

import java.io.UnsupportedEncodingException;

public class UrlUtils {

    private static final int TITLE_MAX_LENGTH = 27;
    private static final String COVER_ART_API_BASE = "http://coverartarchive.org/release";
    private static final String COVER_ART_FORMAT = "front-500";

    public static String formatCoverArtUrl(String MBID) {
        return String.format("%1s/%2s/%3s",
                COVER_ART_API_BASE,
                MBID,
                COVER_ART_FORMAT);
    }

    public static String formatMusicBrainzQuery(String title, String artist) {
        try {
            String formattedTitle = formatTitle(title.toLowerCase());
            String formattedArtist = formatArtist(artist.toLowerCase());
            return String.format("%1s AND %2s",
                    formattedTitle,
                    formattedArtist);
        } catch (UnsupportedEncodingException e) {
            Log.e("WUVA", e.getLocalizedMessage());
            return null;
        }
    }

    private static String formatTitle(String title) throws UnsupportedEncodingException {
        if (title.length() < TITLE_MAX_LENGTH) {
            title = String.format("\"%s\"", title);
        } else {
            // If title is max length, then:
            // 1. Do not use "" to surround the song name
            // 2. Add a '*' to the end
            title = String.format("%s*", title);
        }
        return title;
    }

    private static String formatArtist(String artist) throws UnsupportedEncodingException {
        // Only take first artist before / (W/ or F/ indicates multiple artists)
        if (artist.contains("/")) {
            artist = artist.substring(0, artist.indexOf("/")-2);
        }

        // Handle +/& in artist name, if it exists
        String artistResult;
        if (artist.contains("+")) {
            artistResult = String.format("artist:\"%1s\"+OR+artist:\"%2s\"",
                    artist,
                    artist.replace('+', '&'));
        } else if (artist.contains("&")) {
            artistResult = String.format("artist:\"%1s\"+OR+artist:\"%2s\"",
                    artist,
                    artist.replace('&', '+'));
        } else {
            artistResult = String.format("artist:\"%s\"", artist);
        }
        return artistResult;
    }
}
