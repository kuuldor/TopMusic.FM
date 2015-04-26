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

package the.topmusic.loaders;

import android.content.Context;
import android.database.Cursor;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio.AudioColumns;

import java.util.ArrayList;
import java.util.List;

import the.topmusic.model.Song;
import the.topmusic.utils.Lists;
import the.topmusic.utils.PreferenceUtils;

/**
 * Used to query {@link MediaStore.Audio.Media.EXTERNAL_CONTENT_URI} and return
 * the songs on a user's device.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class SongLoader extends WrappedAsyncTaskLoader<List<Song>> {

    /**
     * The result
     */
    private final ArrayList<Song> mSongList = Lists.newArrayList();

    /**
     * The {@link Cursor} used to run the query.
     */
    private Cursor mCursor;

    /**
     * Constructor of <code>SongLoader</code>
     *
     * @param context The {@link Context} to use
     */
    public SongLoader(final Context context) {
        super(context);
    }

    /**
     * Creates the {@link Cursor} used to run the query.
     *
     * @param context The {@link Context} to use.
     * @return The {@link Cursor} used to run the song query.
     */
    public static Cursor makeSongCursor(final Context context) {
        final StringBuilder mSelection = new StringBuilder();
        mSelection.append(AudioColumns.IS_MUSIC + "=1");
        mSelection.append(" AND " + AudioColumns.TITLE + " != ''"); //$NON-NLS-2$
        return context.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                new String[]{
                        /* 0 */
                        BaseColumns._ID,
                        /* 1 */
                        AudioColumns.TITLE,
                        /* 2 */
                        AudioColumns.ARTIST,
                        /* 3 */
                        AudioColumns.ALBUM,
                        /* 4 */
                        AudioColumns.ARTIST_ID,
                        /* 5 */
                        AudioColumns.ALBUM_ID,
                        AudioColumns.DURATION
                }, mSelection.toString(), null,
                PreferenceUtils.getInstace(context).getSongSortOrder());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Song> loadInBackground() {
        // Create the Cursor
        mCursor = makeSongCursor(getContext());
        // Gather the data
        if (mCursor != null && mCursor.moveToFirst()) {
            do {
                // Copy the song Id
                final String id = mCursor.getString(0);

                // Copy the song name
                final String songName = mCursor.getString(1);

                // Copy the artist name
                final String artist = mCursor.getString(2);

                // Copy the album name
                final String album = mCursor.getString(3);

                final String artistID = mCursor.getString(4);
                final String albumID = mCursor.getString(5);
                final String duration = mCursor.getString(6);

                // Create a new song
                final Song song = new Song(id, songName, artist, artistID, album, albumID, duration);

                // Add everything up
                mSongList.add(song);
            } while (mCursor.moveToNext());
        }
        // Close the cursor
        if (mCursor != null) {
            mCursor.close();
            mCursor = null;
        }
        return mSongList;
    }
}
