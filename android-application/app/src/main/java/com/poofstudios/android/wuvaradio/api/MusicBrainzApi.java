package com.poofstudios.android.wuvaradio.api;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.logging.HttpLoggingInterceptor;

import retrofit.GsonConverterFactory;
import retrofit.Retrofit;

// Singleton class that creates MusicBrainzService object
public class MusicBrainzApi {

    static MusicBrainzService musicBrainzService;

    public static MusicBrainzService getService() {
        if (musicBrainzService == null) {
            // Uncomment these lines to enable request logging
            /*
            OkHttpClient client = new OkHttpClient();
            HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
            interceptor.setLevel(HttpLoggingInterceptor.Level.BASIC);
            client.interceptors().add(interceptor);
            */

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(MusicBrainzService.BASE_ENDPOINT)
//                    .client(client)         // Uncomment to eneable request logging
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            musicBrainzService = retrofit.create(MusicBrainzService.class);
        }
        return musicBrainzService;
    }
}
