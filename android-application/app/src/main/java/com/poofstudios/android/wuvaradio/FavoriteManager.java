package com.poofstudios.android.wuvaradio;

import android.content.Context;

import com.poofstudios.android.wuvaradio.model.Favorite;

import java.util.HashSet;

/**
 * Singleton class that manages user favorites
 */
public class FavoriteManager {

    // Instance of context to use with shared prefs
    private static Context mContext;

    // Set of current user favorites
    private static HashSet<Favorite> mFavorites;

    static FavoriteManager mFavoriteManager;

    public static FavoriteManager getFavoriteManager(Context context) {
        if (mFavoriteManager == null) {
            mFavoriteManager = new FavoriteManager(context);
        }
        return mFavoriteManager;
    }

    // Use a private constructor other classes cannot call it
    private FavoriteManager(Context context) {
        mContext = context;

        // Setup the HashMap
        mFavorites = new HashSet<>();

        // Load data from shared preferences
        // TODO Load favorites from local storage
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
        // TODO Update local storage
    }

    /**
     * Checks if a favorite is in the user's favorite list
     * @param favorite favorite object to check
     * @return true if the favorite is in the user's list, false otherwise
     */
    public boolean isFavorite(Favorite favorite) {
        return mFavorites.contains(favorite);
    }
}
