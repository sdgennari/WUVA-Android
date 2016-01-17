package com.poofstudios.android.wuvaradio.api;

import android.support.annotation.Nullable;
import android.util.Log;
import android.util.LruCache;

import com.poofstudios.android.wuvaradio.api.model.RecordingResponse;
import com.poofstudios.android.wuvaradio.model.Track;

import java.io.UnsupportedEncodingException;

import retrofit.Call;
import retrofit.Callback;
import retrofit.Response;
import retrofit.Retrofit;

/**
 * Singleton class to map MusicBrainz queries to resulting CoverArtUrl
 */
public class CoverArtUrlCache {

    // Vars for url formatting
    private static final int TITLE_MAX_LENGTH = 27;
    private static final String COVER_ART_API_BASE = "http://coverartarchive.org/release-group";
    private static final String COVER_ART_FORMAT = "front-500";

    // Maximum number of elements that can be stored in the cache
    private static final int CACHE_SIZE = 50;

    private final LruCache<Track, String> mCache;

    private MusicBrainzService mMusicBrainzService;

    // Static instance for singleton
    private static CoverArtUrlCache sCoverArtUrlCache;

    public static CoverArtUrlCache getUrlCache() {
        if (sCoverArtUrlCache == null) {
            sCoverArtUrlCache = new CoverArtUrlCache();
        } return sCoverArtUrlCache;
    }

    private CoverArtUrlCache() {
        // Configure LRU cache
        // Default constructor with size sets the maximum number of cache items to size param
        mCache = new LruCache<>(CACHE_SIZE);

        mMusicBrainzService = MusicBrainzApi.getService();
    }

    public String getCoverArtUrl(Track track) {
        return mCache.get(track);
    }

    public void fetchCoverArtUrl(final Track track,
                                 final CacheResultListener listener) {
        // Format MusicBrainz query
        String query = formatMusicBrainzQuery(track.getTitle(), track.getArtist());

        // Create Retrofit callback object
        Call<RecordingResponse> recordingResponseCall = mMusicBrainzService.getMBID(query);

        // Enqueue query and call listener on response
        recordingResponseCall.enqueue(new Callback<RecordingResponse>() {
            @Override
            public void onResponse(Response<RecordingResponse> response, Retrofit retrofit) {
                // Response not null
                if (response != null && response.body() != null) {
                    RecordingResponse recordingResponse = response.body();
                    // At least one recording returned
                    if (recordingResponse.count != 0) {
                        // Get the first recording
                        RecordingResponse.Recording recording = recordingResponse.recordings.get(0);

                        // At least one release
                        if (recording.releases.size() != 0) {
                            RecordingResponse.Release release = recording.releases.get(0);

                            // Use the id from the release group
                            String coverArtUrl = formatCoverArtUrl(release.releaseGroup.id);

                            // Add the cover art url to the cache
                            mCache.put(track, coverArtUrl);

                            // Notify the listener that the response has returned
                            listener.onResult(coverArtUrl);
                            return;
                        }
                    }
                }

                // Notify the listener that the request did not return valid data
                listener.onResult(null);
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e("WUVA", "Error: " + t.getLocalizedMessage());
            }
        });
    }

    public interface CacheResultListener {
        void onResult(@Nullable String coverArtUrl);
    }

    // Url formatting methods below

    // Example MusicBrainz Query Url
    // Song:    Reflektor
    // Title:   Arcade Fire
    // Result:  recording:(reflektor)%20AND%20artist:(arcade%20fire)%20AND%20status:(official)%20AND
    //          %20NOT%20secondarytype:(compilation)

    // Combined Url Cases:
    // 1. Surround each param with parentheses
    //  ex: artist:(...)
    // 2. Add 'AND status:(official)' to query

    // Title and Artist Cases:
    // 1. Remove '&'
    // 2. Remove content in parentheses
    // 3. Remove all content including and after 'F/' or 'W/'
    // 4. Handle max length case

    // Given title and artist, returns 'title AND artist: "B"'
    private String formatMusicBrainzQuery(String title, String artist) {
        try {
            String formattedTitle = formatTitle(title.toLowerCase());
            String formattedArtist = formatArtist(artist.toLowerCase());
            return String.format("%1s AND %2s AND status:(official) AND NOT secondarytype:(compilation)",
                    formattedTitle,
                    formattedArtist);
        } catch (UnsupportedEncodingException e) {
            Log.e("WUVA", e.getLocalizedMessage());
            return null;
        }
    }

    private String formatTitle(String title) throws UnsupportedEncodingException {
        // Remove W/ and F/
        // Remove '&'
        // Remove contents of ()
        title = title.replaceAll("([WF]/.*|&\\s|\\(.*\\))", "").trim();

        // Handle max length case
        if (title.length() >= TITLE_MAX_LENGTH) {
            title += "*";   // Add * to end of title as a wildcard
        }

        return String.format("recording:(%s)", title);
    }

    private String formatArtist(String artist) throws UnsupportedEncodingException {
        // Remove W/ and F/
        // Remove contents of ()
        artist = artist.replaceAll("([WF]/.*|\\(.*\\))", "").trim();

        // Handle max length case
        if (artist.length() >= TITLE_MAX_LENGTH) {
            artist += "*";   // Add * to end of title as a wildcard
        }

        // If artist contains '&', replace 'A & B' with artist:(A) AND artist:(B)
        if (artist.contains("&")) {
            String[] parts = artist.split("&");
            return String.format("artist:(%1s) AND artist:(%2s)", parts[0], parts[1]);
        }
        // Else return singly formatted artist wrapped in quotes
        return String.format("artist:(\"%s\")", artist);
    }


    private String formatCoverArtUrl(String MBID) {
        return String.format("%1s/%2s/%3s",
                COVER_ART_API_BASE,
                MBID,
                COVER_ART_FORMAT);
    }
}
