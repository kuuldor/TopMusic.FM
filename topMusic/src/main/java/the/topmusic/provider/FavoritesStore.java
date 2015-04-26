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

package the.topmusic.provider;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.MediaStore;

import the.topmusic.model.Song;

/**
 * This class is used to to create the database used to make the Favorites
 * playlist.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */

/**
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class FavoritesStore extends SQLiteOpenHelper {

    /* Name of database file */
    public static final String DATABASENAME = "favorites.db";
    /* Version constant to increment when the database should be rebuilt */
    private static final int VERSION = 1;
    private static FavoritesStore sInstance = null;

    /**
     * Constructor of <code>FavoritesStore</code>
     *
     * @param context The {@link Context} to use
     */
    public FavoritesStore(final Context context) {
        super(context, DATABASENAME, null, VERSION);
    }

    /**
     * @param context The {@link Context} to use
     * @return A new instance of this class
     */
    public static synchronized FavoritesStore getInstance(final Context context) {
        if (sInstance == null) {
            sInstance = new FavoritesStore(context.getApplicationContext());
        }
        return sInstance;
    }

    /**
     * Clear the cache.
     *
     * @param context The {@link Context} to use.
     */
    public static void deleteDatabase(final Context context) {
        context.deleteDatabase(DATABASENAME);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(final SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + FavoriteColumns.NAME + " (" + FavoriteColumns.ID
                + " TEXT NOT NULL," + FavoriteColumns.SONGNAME + " TEXT NOT NULL,"
                + FavoriteColumns.ALBUMNAME + " TEXT NOT NULL," + FavoriteColumns.ARTISTNAME
                + " TEXT NOT NULL," + FavoriteColumns.ALBUMID + " TEXT NOT NULL,"
                + FavoriteColumns.ARTISTID + " TEXT NOT NULL," + FavoriteColumns.URL + " TEXT DEFAULT NULL,"
                + FavoriteColumns.IMAGE + " TEXT DEFAULT NULL," + FavoriteColumns.THUMBNAIL + " TEXT DEFAULT NULL,"
                + FavoriteColumns.PLAYCOUNT + " LONG NOT NULL);");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + FavoriteColumns.NAME);
        onCreate(db);
    }

    /**
     * Used to store song Ids in our database
     *
     * @param songId     The album's ID
     * @param songName   The song name
     * @param albumName  The album name
     * @param artistName The artist name
     */
    public void addSongId(final String songId, final String songName, final String albumName,
                          final String artistName, final String albumID,
                          final String artistID) {
        if (songId == null || songName == null || albumName == null || artistName == null || artistID == null || albumID == null) {
            return;
        }

        final Long playCount = getPlayCount(songId);
        final SQLiteDatabase database = getWritableDatabase();
        final ContentValues values = new ContentValues(5);

        database.beginTransaction();

        values.put(FavoriteColumns.ID, songId);
        values.put(FavoriteColumns.SONGNAME, songName);
        values.put(FavoriteColumns.ALBUMNAME, albumName);
        values.put(FavoriteColumns.ARTISTNAME, artistName);
        values.put(FavoriteColumns.ALBUMID, albumID);
        values.put(FavoriteColumns.ARTISTID, artistID);
        values.put(FavoriteColumns.PLAYCOUNT, playCount != 0 ? playCount + 1 : 1);

        database.delete(FavoriteColumns.NAME, FavoriteColumns.ID + " = ?", new String[]{
                songId
        });
        database.insert(FavoriteColumns.NAME, null, values);
        database.setTransactionSuccessful();
        database.endTransaction();

    }

    public void addSong(final Song song) {
        if (song == null) {
            return;
        }

        final Long playCount = getPlayCount(song.mSongId);
        final SQLiteDatabase database = getWritableDatabase();
        final ContentValues values = new ContentValues(5);

        database.beginTransaction();

        values.put(FavoriteColumns.ID, song.mSongId);
        values.put(FavoriteColumns.SONGNAME, song.mSongName);
        values.put(FavoriteColumns.ALBUMNAME, song.mAlbumName);
        values.put(FavoriteColumns.ARTISTNAME, song.mArtistName);
        values.put(FavoriteColumns.ALBUMID, song.mAlbumId);
        values.put(FavoriteColumns.ARTISTID, song.mArtistId);
        values.put(FavoriteColumns.PLAYCOUNT, playCount != 0 ? playCount + 1 : 1);
        if (song.mURL != null) {
            values.put(FavoriteColumns.URL, song.mURL);
        }
        if (song.mImage != null) {
            values.put(FavoriteColumns.IMAGE, song.mImage);
        }

        if (song.mThumbnail != null) {
            values.put(FavoriteColumns.THUMBNAIL, song.mThumbnail);
        }

        database.delete(FavoriteColumns.NAME, FavoriteColumns.ID + " = ?", new String[]{
                song.mSongId
        });
        database.insert(FavoriteColumns.NAME, null, values);
        database.setTransactionSuccessful();
        database.endTransaction();

    }

    /**
     * Used to retrieve a single song Id from our database
     *
     * @param songId The song Id to reference
     * @return The song Id
     */
    public String getSongId(final String songId) {
        if (songId == null) {
            return null;
        }

        final SQLiteDatabase database = getReadableDatabase();
        final String[] projection = new String[]{
                FavoriteColumns.ID
        };
        final String selection = FavoriteColumns.ID + "=?";
        final String[] having = new String[]{
                songId
        };
        Cursor cursor = database.query(FavoriteColumns.NAME, projection, selection, having, null,
                null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            final String id = cursor.getString(cursor.getColumnIndexOrThrow(FavoriteColumns.ID));
            cursor.close();
            cursor = null;
            return id;
        }
        if (cursor != null) {
            cursor.close();
            cursor = null;
        }
        return null;
    }

    /**
     * Used to retrieve the play count
     *
     * @param songId The song Id to reference
     * @return The play count for a song
     */
    public Long getPlayCount(final String songId) {
        if (songId == null) {
            return null;
        }

        final SQLiteDatabase database = getReadableDatabase();
        final String[] projection = new String[]{FavoriteColumns.PLAYCOUNT};
        final String selection = FavoriteColumns.ID + "=?";
        final String[] having = new String[]{
                songId
        };
        Cursor cursor = database.query(FavoriteColumns.NAME, projection, selection, having, null,
                null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            final Long playCount = cursor.getLong(cursor
                    .getColumnIndexOrThrow(FavoriteColumns.PLAYCOUNT));
            cursor.close();
            cursor = null;
            return playCount;
        }
        if (cursor != null) {
            cursor.close();
            cursor = null;
        }

        return (long) 0;
    }

    /**
     * Toggle the current song as favorite
     */
    public void toggleSong(final String songId, final String songName, final String albumName,
                           final String artistName, final String albumID,
                           final String artistID) {
        if (getSongId(songId) == null) {
            addSongId(songId, songName, albumName, artistName, albumID, artistID);
        } else {
            removeItem(songId);
        }

    }

    /**
     * @param songId
     */
    public void removeItem(final String songId) {
        final SQLiteDatabase database = getReadableDatabase();
        database.delete(FavoriteColumns.NAME, FavoriteColumns.ID + " = ?", new String[]{
                songId
        });

    }

    public void toggleSong(Song song) {
        if (getSongId(song.mSongId) == null) {
            addSong(song);
        } else {
            removeItem(song.mSongId);
        }
    }

    public void addSongs(Song[] songList) {
        if (songList == null) {
            return;
        }

        final SQLiteDatabase database = getWritableDatabase();
        final ContentValues values = new ContentValues(5);

        database.beginTransaction();

        for (Song song : songList) {
            final Long playCount = getPlayCount(song.mSongId);
            values.put(FavoriteColumns.ID, song.mSongId);
            values.put(FavoriteColumns.SONGNAME, song.mSongName);
            values.put(FavoriteColumns.ALBUMNAME, song.mAlbumName);
            values.put(FavoriteColumns.ARTISTNAME, song.mArtistName);
            values.put(FavoriteColumns.ALBUMID, song.mAlbumId);
            values.put(FavoriteColumns.ARTISTID, song.mArtistId);
            values.put(FavoriteColumns.PLAYCOUNT, playCount != 0 ? playCount + 1 : 1);
            if (song.mURL != null) {
                values.put(FavoriteColumns.URL, song.mURL);
            }
            if (song.mImage != null) {
                values.put(FavoriteColumns.IMAGE, song.mImage);
            }
            if (song.mThumbnail != null) {
                values.put(FavoriteColumns.THUMBNAIL, song.mThumbnail);
            }

            database.delete(FavoriteColumns.NAME, FavoriteColumns.ID + " = ?", new String[]{
                    song.mSongId
            });
            database.insert(FavoriteColumns.NAME, null, values);
        }
        database.setTransactionSuccessful();
        database.endTransaction();
    }

    public interface FavoriteColumns {

        /* Table name */
        public static final String NAME = "favorites";

        /* Song IDs column */
        public static final String ID = "songid";

        /* Song name column */
        public static final String SONGNAME = MediaStore.Audio.AudioColumns.TITLE;

        /* Album name column */
        public static final String ALBUMNAME = MediaStore.Audio.AudioColumns.ALBUM;

        /* Artist name column */
        public static final String ARTISTNAME = MediaStore.Audio.AudioColumns.ARTIST;

        /* Album ID column */
        public static final String ALBUMID = MediaStore.Audio.AudioColumns.ALBUM_ID;

        /* Artist ID column */
        public static final String ARTISTID = MediaStore.Audio.AudioColumns.ARTIST_ID;

        public static final String URL = "url";

        public static final String IMAGE = "image";

        public static final String THUMBNAIL = "thumbnail";

        /* Play count column */
        public static final String PLAYCOUNT = "play_count";
    }

}
