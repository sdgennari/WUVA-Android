package com.poofstudios.android.wuvaradio.api;

import com.poofstudios.android.wuvaradio.api.model.RecordingResponse;

import retrofit.Call;
import retrofit.http.GET;
import retrofit.http.Query;

public interface MusicBrainzService {

    String BASE_ENDPOINT = "http://www.musicbrainz.org";

    @GET("/ws/2/recording?limit=1&fmt=json")
    Call<RecordingResponse> getMBID(@Query("query") String query);
}
