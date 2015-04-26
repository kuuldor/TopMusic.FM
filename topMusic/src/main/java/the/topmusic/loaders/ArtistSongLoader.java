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
 * the songs for a particular artist.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class ArtistSongLoader extends WrappedAsyncTaskLoader<List<Song>> {

    /**
     * The result
     */
    private final ArrayList<Song> mSongList = Lists.newArrayList();
    /**
     * The Id of the artist the songs belong to.
     */
    private final String mArtistID;
    /**
     * The {@link Cursor} used to run the query.
     */
    private Cursor mCursor;

    /**
     * Constructor of <code>ArtistSongLoader</code>
     *
     * @param context  The {@link android.content.Context} to use.
     * @param artistId The Id of the artist the songs belong to.
     */
    public ArtistSongLoader(final Context context, final String artistId) {
        super(context);
        mArtistID = artistId;
    }

    /**
     * @param context  The {@link android.content.Context} to use.
     * @param artistId The Id of the artist the songs belong to.
     * @return The {@link Cursor} used to run the query.
     */
    public static Cursor makeArtistSongCursor(final Context context, final String artistId) {
        // Match the songs up with the artist
        final StringBuilder selection = new StringBuilder();
        selection.append(AudioColumns.IS_MUSIC + "=1");
        selection.append(" AND " + AudioColumns.TITLE + " != ''");
        selection.append(" AND " + AudioColumns.ARTIST_ID + "=" + artistId);
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
                        AudioColumns.ALBUM_ID,
                        AudioColumns.ARTIST_ID
                }, selection.toString(), null,
                PreferenceUtils.getInstace(context).getArtistSongSortOrder());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Song> loadInBackground() {
        // Create the Cursor
        mCursor = makeArtistSongCursor(getContext(), mArtistID);
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

                final String albumID = mCursor.getString(4);

                // Create a new song
                final Song song = new Song(id, songName, artist, mArtistID, album, albumID, null);

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
