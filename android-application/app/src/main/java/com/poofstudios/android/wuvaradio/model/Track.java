package com.poofstudios.android.wuvaradio.model;

import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

public class Track {

    @SerializedName("title")
    private String mTitle;

    @SerializedName("artist")
    private String mArtist;

    @Nullable
    @SerializedName("cover_art_url")
    private String mCoverArtUrl;

    public Track(CharSequence title, CharSequence artist) {
        this(String.valueOf(title), String.valueOf(artist));
    }

    public Track(CharSequence title, CharSequence artist,
                 @Nullable String coverArtUrl) {
        this(String.valueOf(title), String.valueOf(artist), coverArtUrl);
    }

    public Track(String title, String artist) {
        this(title, artist, null);
    }

    public Track(String title, String artist, @Nullable String coverArtUrl) {
        this.mTitle = title;
        this.mArtist = artist;
        this.mCoverArtUrl = coverArtUrl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof Track) {
            Track track = (Track) o;
            if (this.mTitle.equals(track.mTitle) && this.mArtist.equals(track.mArtist)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hashcode = mTitle.hashCode();
        hashcode = hashcode * 37 + mArtist.hashCode();
        return hashcode;
    }

    // Accessors & Modifiers
    public String getTitle() {
        return mTitle;
    }

    public String getArtist() {
        return mArtist;
    }

    @Nullable
    public String getCoverArtUrl() {
        return mCoverArtUrl;
    }

    public void setCoverArtUrl(@Nullable String coverArtUrl) {
        this.mCoverArtUrl = coverArtUrl;
    }
}
