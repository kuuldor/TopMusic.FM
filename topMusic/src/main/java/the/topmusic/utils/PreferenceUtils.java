/*
 * Copyright (C) 2012 Andrew Neal Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package the.topmusic.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

import the.topmusic.R;
import the.topmusic.ui.fragments.AlbumFragment;
import the.topmusic.ui.fragments.ArtistFragment;
import the.topmusic.ui.fragments.SongFragment;
import the.topmusic.ui.fragments.phone.MusicBrowserPhoneFragment;
import the.topmusic.ui.fragments.profile.AlbumSongFragment;
import the.topmusic.ui.fragments.profile.ArtistAlbumFragment;
import the.topmusic.ui.fragments.profile.ArtistSongFragment;


/**
 * A collection of helpers designed to get and set various preferences across
 * TopMusic.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public final class PreferenceUtils {

    /* Default start page (Artist page) */
    public static final int DEFFAULT_PAGE = 2;

    /* Saves the last page the pager was on in {@link MusicBrowserPhoneFragment} */
    public static final String START_PAGE = "start_page";

    // Sort order for the artist list
    public static final String ARTIST_SORT_ORDER = "artist_sort_order";

    // Sort order for the artist song list
    public static final String ARTIST_SONG_SORT_ORDER = "artist_song_sort_order";

    // Sort order for the artist album list
    public static final String ARTIST_ALBUM_SORT_ORDER = "artist_album_sort_order";

    // Sort order for the album list
    public static final String ALBUM_SORT_ORDER = "album_sort_order";

    // Sort order for the album song list
    public static final String ALBUM_SONG_SORT_ORDER = "album_song_sort_order";

    // Sort order for the song list
    public static final String SONG_SORT_ORDER = "song_sort_order";

    // Sets the type of layout to use for the artist list
    public static final String ARTIST_LAYOUT = "artist_layout";

    // Sets the type of layout to use for the album list
    public static final String ALBUM_LAYOUT = "album_layout";

    // Sets the type of layout to use for the recent list
    public static final String RECENT_LAYOUT = "recent_layout";

    // Key used to download images only on Wi-Fi
    public static final String ONLY_ON_WIFI = "only_on_wifi";

    // Key that gives permissions to download missing album covers
    public static final String DOWNLOAD_MISSING_ARTWORK = "download_missing_artwork";

    // Key that gives permissions to download missing artist images
    public static final String DOWNLOAD_MISSING_ARTIST_IMAGES = "download_missing_artist_images";

    // Enables lock screen controls on Honeycomb and above
    public static final String USE_LOCKSREEN_CONTROLS = "use_lockscreen_controls";

    // Key used to set the overall theme color
    public static final String DEFAULT_THEME_COLOR = "default_theme_color";
    public static final String SERVER_LOCATION = "server_location";

    public static final String HOME_BROWSER_TYPE = "home_browser_type";

    private static PreferenceUtils sInstance;

    private final SharedPreferences mPreferences;

    /**
     * Constructor for <code>PreferenceUtils</code>
     *
     * @param context The {@link Context} to use.
     */
    public PreferenceUtils(final Context context) {
        mPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    /**
     * @param context The {@link Context} to use.
     * @return A singelton of this class
     */
    public static PreferenceUtils getInstace(final Context context) {
        if (sInstance == null) {
            sInstance = new PreferenceUtils(context.getApplicationContext());
        }
        return sInstance;
    }

    /**
     * Returns the last page the user was on when the app was exited.
     *
     * @return The page to start on when the app is opened.
     */
    public final int getStartPage() {
        return mPreferences.getInt(START_PAGE, DEFFAULT_PAGE);
    }

    /**
     * Saves the current page the user is on when they close the app.
     *
     * @param value The last page the pager was on when the onDestroy is called
     *              in {@link MusicBrowserPhoneFragment}.
     */
    public void setStartPage(final int value) {
        TopMusicUtils.execute(false, new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(final Void... unused) {
                final SharedPreferences.Editor editor = mPreferences.edit();
                editor.putInt(START_PAGE, value);
                SharedPreferencesCompat.apply(editor);

                return null;
            }
        }, (Void[]) null);
    }

    public final int getStartPage(int defaultPage) {
        return mPreferences.getInt(START_PAGE, defaultPage);
    }

    /**
     * Sets the new theme color.
     *
     * @param value The new theme color to use.
     */
    public void setDefaultThemeColor(final int value) {
        TopMusicUtils.execute(false, new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(final Void... unused) {
                final SharedPreferences.Editor editor = mPreferences.edit();
                editor.putInt(DEFAULT_THEME_COLOR, value);
                SharedPreferencesCompat.apply(editor);

                return null;
            }
        }, (Void[]) null);
    }

    /**
     * Returns the current theme color.
     *
     * @param context The {@link Context} to use.
     * @return The default theme color.
     */
    public final int getDefaultThemeColor(final Context context) {
        return mPreferences.getInt(DEFAULT_THEME_COLOR,
                context.getResources().getColor(R.color.holo_blue_light));
    }

    /**
     * @return True if the user has checked to only download images on Wi-Fi,
     * false otherwise
     */
    public final boolean onlyOnWifi() {
        return mPreferences.getBoolean(ONLY_ON_WIFI, true);
    }

    public final String serverLocaion(final Context context) {
        return mPreferences.getString(SERVER_LOCATION, context.getString(R.string.mooo_url));
    }

    /**
     * @param value True if the user only wants to download images on Wi-Fi,
     *              false otherwise
     */
    public void setOnlyOnWifi(final boolean value) {
        TopMusicUtils.execute(false, new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(final Void... unused) {
                final SharedPreferences.Editor editor = mPreferences.edit();
                editor.putBoolean(ONLY_ON_WIFI, value);
                SharedPreferencesCompat.apply(editor);

                return null;
            }
        }, (Void[]) null);
    }

    /**
     * @return True if the user has checked to download missing album covers,
     * false otherwise.
     */
    public final boolean downloadMissingArtwork() {
        return mPreferences.getBoolean(DOWNLOAD_MISSING_ARTWORK, true);
    }

    /**
     * @param value True if the user only wants to download missing album
     *              covers, false otherwise.
     */
    public void setDownloadMissingArtwork(final boolean value) {
        TopMusicUtils.execute(false, new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(final Void... unused) {
                final SharedPreferences.Editor editor = mPreferences.edit();
                editor.putBoolean(DOWNLOAD_MISSING_ARTWORK, value);
                SharedPreferencesCompat.apply(editor);

                return null;
            }
        }, (Void[]) null);
    }

    /**
     * @return True if the user has checked to download missing artist images,
     * false otherwise.
     */
    public final boolean downloadMissingArtistImages() {
        return mPreferences.getBoolean(DOWNLOAD_MISSING_ARTIST_IMAGES, true);
    }

    /**
     * @param value True if the user only wants to download missing artist
     *              images , false otherwise.
     */
    public void setDownloadMissingArtistImages(final boolean value) {
        TopMusicUtils.execute(false, new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(final Void... unused) {
                final SharedPreferences.Editor editor = mPreferences.edit();
                editor.putBoolean(DOWNLOAD_MISSING_ARTIST_IMAGES, value);
                SharedPreferencesCompat.apply(editor);

                return null;
            }
        }, (Void[]) null);
    }

    /**
     * @return True if the user has checked to use lockscreen controls, false
     * otherwise.
     */
    public final boolean enableLockscreenControls() {
        return mPreferences.getBoolean(USE_LOCKSREEN_CONTROLS, true);
    }

    /**
     * @param value True if the user has checked to use lockscreen controls,
     *              false otherwise.
     */
    public void setLockscreenControls(final boolean value) {
        TopMusicUtils.execute(false, new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(final Void... unused) {
                final SharedPreferences.Editor editor = mPreferences.edit();
                editor.putBoolean(USE_LOCKSREEN_CONTROLS, value);
                SharedPreferencesCompat.apply(editor);

                return null;
            }
        }, (Void[]) null);
    }

    /**
     * Saves the sort order for a list.
     *
     * @param key   Which sort order to change
     * @param value The new sort order
     */
    private void setSortOrder(final String key, final String value) {
        TopMusicUtils.execute(false, new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(final Void... unused) {
                final SharedPreferences.Editor editor = mPreferences.edit();
                editor.putString(key, value);
                SharedPreferencesCompat.apply(editor);

                return null;
            }
        }, (Void[]) null);
    }

    /**
     * @return The sort order used for the artist list in {@link ArtistFragment}
     */
    public final String getArtistSortOrder() {
        return mPreferences.getString(ARTIST_SORT_ORDER, SortOrder.ArtistSortOrder.ARTIST_A_Z);
    }

    /**
     * Sets the sort order for the artist list.
     *
     * @param value The new sort order
     */
    public void setArtistSortOrder(final String value) {
        setSortOrder(ARTIST_SORT_ORDER, value);
    }

    /**
     * @return The sort order used for the artist song list in
     * {@link ArtistSongFragment}
     */
    public final String getArtistSongSortOrder() {
        return mPreferences.getString(ARTIST_SONG_SORT_ORDER,
                SortOrder.ArtistSongSortOrder.SONG_A_Z);
    }

    /**
     * Sets the sort order for the artist song list.
     *
     * @param value The new sort order
     */
    public void setArtistSongSortOrder(final String value) {
        setSortOrder(ARTIST_SONG_SORT_ORDER, value);
    }

    /**
     * @return The sort order used for the artist album list in
     * {@link ArtistAlbumFragment}
     */
    public final String getArtistAlbumSortOrder() {
        return mPreferences.getString(ARTIST_ALBUM_SORT_ORDER,
                SortOrder.ArtistAlbumSortOrder.ALBUM_A_Z);
    }

    /**
     * Sets the sort order for the artist album list.
     *
     * @param value The new sort order
     */
    public void setArtistAlbumSortOrder(final String value) {
        setSortOrder(ARTIST_ALBUM_SORT_ORDER, value);
    }

    /**
     * @return The sort order used for the album list in {@link AlbumFragment}
     */
    public final String getAlbumSortOrder() {
        return mPreferences.getString(ALBUM_SORT_ORDER, SortOrder.AlbumSortOrder.ALBUM_A_Z);
    }

    /**
     * Sets the sort order for the album list.
     *
     * @param value The new sort order
     */
    public void setAlbumSortOrder(final String value) {
        setSortOrder(ALBUM_SORT_ORDER, value);
    }

    /**
     * @return The sort order used for the album song in
     * {@link AlbumSongFragment}
     */
    public final String getAlbumSongSortOrder() {
        return mPreferences.getString(ALBUM_SONG_SORT_ORDER,
                SortOrder.AlbumSongSortOrder.SONG_TRACK_LIST);
    }

    /**
     * Sets the sort order for the album song list.
     *
     * @param value The new sort order
     */
    public void setAlbumSongSortOrder(final String value) {
        setSortOrder(ALBUM_SONG_SORT_ORDER, value);
    }

    /**
     * @return The sort order used for the song list in {@link SongFragment}
     */
    public final String getSongSortOrder() {
        return mPreferences.getString(SONG_SORT_ORDER, SortOrder.SongSortOrder.SONG_A_Z);
    }

    /**
     * Sets the sort order for the song list.
     *
     * @param value The new sort order
     */
    public void setSongSortOrder(final String value) {
        setSortOrder(SONG_SORT_ORDER, value);
    }

    /**
     * Saves the layout type for a list
     *
     * @param key   Which layout to change
     * @param value The new layout type
     */
    private void setLayoutType(final String key, final String value) {
        TopMusicUtils.execute(false, new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(final Void... unused) {
                final SharedPreferences.Editor editor = mPreferences.edit();
                editor.putString(key, value);
                SharedPreferencesCompat.apply(editor);

                return null;
            }
        }, (Void[]) null);
    }

    /**
     * Sets the layout type for the artist list
     *
     * @param value The new layout type
     */
    public void setArtistLayout(final String value) {
        setLayoutType(ARTIST_LAYOUT, value);
    }

    /**
     * Sets the layout type for the album list
     *
     * @param value The new layout type
     */
    public void setAlbumLayout(final String value) {
        setLayoutType(ALBUM_LAYOUT, value);
    }

    /**
     * Sets the layout type for the recent list
     *
     * @param value The new layout type
     */
    public void setRecentLayout(final String value) {
        setLayoutType(RECENT_LAYOUT, value);
    }

    /**
     * @param context The {@link Context} to use.
     * @param which   Which list to check.
     * @return True if the layout type is the simple layout, false otherwise.
     */
    public boolean isSimpleLayout(final String which, final Context context) {
        final String simple = "simple";
        final String defaultValue = "grid";
        return mPreferences.getString(which, defaultValue).equals(simple);
    }

    /**
     * @param context The {@link Context} to use.
     * @param which   Which list to check.
     * @return True if the layout type is the simple layout, false otherwise.
     */
    public boolean isDetailedLayout(final String which, final Context context) {
        final String detailed = "detailed";
        final String defaultValue = "grid";
        return mPreferences.getString(which, defaultValue).equals(detailed);
    }

    /**
     * @param context The {@link Context} to use.
     * @param which   Which list to check.
     * @return True if the layout type is the simple layout, false otherwise.
     */
    public boolean isGridLayout(final String which, final Context context) {
        final String grid = "grid";
        final String defaultValue = "simple";
        return mPreferences.getString(which, defaultValue).equals(grid);
    }

    private void setHomeBrowserType(final String value) {
        TopMusicUtils.execute(false, new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(final Void... unused) {
                final SharedPreferences.Editor editor = mPreferences.edit();
                editor.putString(HOME_BROWSER_TYPE, value);
                SharedPreferencesCompat.apply(editor);

                return null;
            }
        }, (Void[]) null);
    }

    public final void setHomeOnline() {
        setHomeBrowserType("online");
    }

    public final void setHomePhone() {
        setHomeBrowserType("phone");
    }

    public final String homeBrowserType(final Context context) {
        return mPreferences.getString(HOME_BROWSER_TYPE, "online");
    }
}
