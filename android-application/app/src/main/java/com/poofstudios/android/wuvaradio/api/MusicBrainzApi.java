package com.poofstudios.android.wuvaradio.api;

import retrofit.GsonConverterFactory;
import retrofit.Retrofit;

public class MusicBrainzApi {

    static MusicBrainzService musicBrainzService;

    public static MusicBrainzService getService() {
        if (musicBrainzService == null) {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(MusicBrainzService.BASE_ENDPOINT)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            musicBrainzService = retrofit.create(MusicBrainzService.class);
        }
        return musicBrainzService;
    }
}
