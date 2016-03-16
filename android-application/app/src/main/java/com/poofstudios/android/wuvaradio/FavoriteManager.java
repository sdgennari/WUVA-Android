package com.poofstudios.android.wuvaradio;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.poofstudios.android.wuvaradio.model.Favorite;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * Singleton class that manages user favorites
 */
public class FavoriteManager {

    // Set of current user favorites
    private static HashSet<Favorite> mFavorites;

    private static SharedPreferences mPrefs;

    static FavoriteManager mFavoriteManager;

    public static FavoriteManager getFavoriteManager(Context context) {
        if (mFavoriteManager == null) {
            mFavoriteManager = new FavoriteManager(context);
        }
        return mFavoriteManager;
    }

    // Use a private constructor other classes cannot call it
    private FavoriteManager(Context context) {

        // Load data from shared preferences
        mPrefs = PreferenceManager.getDefaultSharedPreferences(context);

        restoreFavorites();

        if (mFavorites == null) {
            mFavorites = new HashSet<>();
        }
    }

    public void saveFavorites() {
        SharedPreferences.Editor prefsEditor = mPrefs.edit();
        Gson gson = new Gson();
        String json = gson.toJson(mFavorites);
        prefsEditor.putString("favorites", json);
        prefsEditor.apply();
    }

    public void restoreFavorites() {
        Gson gson = new Gson();
        String json = mPrefs.getString("favorites", "");
        Type type = new TypeToken<HashSet<Favorite>>(){}.getType();
        mFavorites = gson.fromJson(json, type);
    }

    /**
     * Adds or removes a favorite
     * @param favorite favorite object to update
     * @param isFavorite true if favorite should be added, false if it should be removed
     */
    public void setFavorite(Favorite favorite, boolean isFavorite) {
        if (isFavorite) {
            mFavorites.add(favorite);
        } else {
            mFavorites.remove(favorite);
        }
        saveFavorites();
    }

    /**
     * Checks if a favorite is in the user's favorite list
     * @param favorite favorite object to check
     * @return true if the favorite is in the user's list, false otherwise
     */
    public boolean isFavorite(Favorite favorite) {
        return mFavorites.contains(favorite);
    }

    /**
     * Returns a set containing the user's favorite songs
     * @return set containing all favorite songs
     */
    public HashSet<Favorite> getFavorites() {
        return mFavorites;
    }
}
