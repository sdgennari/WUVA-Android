package com.poofstudios.android.wuvaradio.api;

import android.support.annotation.Nullable;
import android.util.Log;
import android.util.LruCache;

import com.poofstudios.android.wuvaradio.api.model.RecordingResponse;
import com.poofstudios.android.wuvaradio.model.Track;
import com.poofstudios.android.wuvaradio.utils.UrlUtils;

import retrofit.Call;
import retrofit.Callback;
import retrofit.Response;
import retrofit.Retrofit;

/**
 * Singleton class to map MusicBrainz queries to resulting CoverArtUrl
 */
public class CoverArtUrlCache {

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
        String query = UrlUtils.formatMusicBrainzQuery(track.getTitle(), track.getArtist());

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
                            String coverArtUrl = UrlUtils.formatCoverArtUrl(release.id);

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
}
