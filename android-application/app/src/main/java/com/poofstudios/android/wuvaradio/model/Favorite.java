package com.poofstudios.android.wuvaradio.model;

import com.google.gson.annotations.SerializedName;

public class Favorite {

    @SerializedName("title")
    public String mTitle;

    @SerializedName("artist")
    public String mArtist;

    long createdAt;

    public Favorite(String title, String artist) {
        this.mTitle = title;
        this.mArtist = artist;

        // Mark when this song was last created
        createdAt = System.currentTimeMillis();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Favorite) {
            Favorite favorite = (Favorite) o;
            return (this.mTitle.equals(favorite.mTitle) && this.mArtist.equals(favorite.mArtist));
        }
        return false;
    }

    // Must implement hashCode since FavoriteManager uses a HashMap
    @Override
    public int hashCode() {
        int titleHash = mTitle.hashCode();
        int artistHash = mArtist.hashCode();
        return 37*titleHash + artistHash;
    }
}
