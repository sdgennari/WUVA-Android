package com.poofstudios.android.wuvaradio.model;

import android.support.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

public class Favorite {

    @NonNull
    @SerializedName("track")
    private Track mTrack;

    @SerializedName("created_at")
    private long createdAt;

    public Favorite(@NonNull Track track) {
        this.mTrack = track;

        // Mark when this song was last created
        createdAt = System.currentTimeMillis();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Favorite) {
            Favorite favorite = (Favorite) o;
            // This will only compare the title and artist of the track
            return this.mTrack.equals(favorite.mTrack);
        }
        return false;
    }

    // Must implement hashCode since FavoriteManager uses a HashMap
    @Override
    public int hashCode() {
        return this.mTrack.hashCode();
    }

    public long getCreatedAt() {
        return this.createdAt;
    }

    public Track getTrack() {
        return this.mTrack;
    }

    public void setTrackCoverArtUrl(String coverArtUrl) {
        this.mTrack.setCoverArtUrl(coverArtUrl);
    }
}
