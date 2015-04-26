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
import android.content.res.Resources;
import android.database.Cursor;

import java.util.ArrayList;
import java.util.List;

import the.topmusic.R;
import the.topmusic.model.Playlist;
import the.topmusic.provider.MusicStore;
import the.topmusic.utils.Lists;

/**
 * Used to query Playlist table and
 * return the playlists on a user's device.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class PlaylistLoader extends WrappedAsyncTaskLoader<List<Playlist>> {

    /**
     * The result
     */
    private final ArrayList<Playlist> mPlaylistList = Lists.newArrayList();

    /**
     * The {@link Cursor} used to run the query.
     */
    private Cursor mCursor;

    /**
     * Constructor of <code>PlaylistLoader</code>
     *
     * @param context The {@link Context} to use
     */
    public PlaylistLoader(final Context context) {
        super(context);
    }

    /**
     * Creates the {@link Cursor} used to run the query.
     *
     * @param context The {@link Context} to use.
     * @return The {@link Cursor} used to run the playlist query.
     */
    public static Cursor makePlaylistCursor(final Context context) {
        return MusicStore.getInstance(context).getReadableDatabase().query(MusicStore.TableNames.PLAYLIST,
                new String[]{
                        /* 0 */
                        MusicStore.BasicColumns.ID,
                        /* 1 */
                        MusicStore.BasicColumns.NAME
                }, null, null, null, null, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Playlist> loadInBackground() {
        // Add the deafult playlits to the adapter
        makeDefaultPlaylists();

        // Create the Cursor
        mCursor = makePlaylistCursor(getContext());
        // Gather the data
        if (mCursor != null && mCursor.moveToFirst()) {
            do {
                // Copy the playlist id
                final String id = mCursor.getString(0);

                // Copy the playlist name
                final String name = mCursor.getString(1);

                // Create a new playlist
                final Playlist playlist = new Playlist(id, name);

                // Add everything up
                mPlaylistList.add(playlist);
            } while (mCursor.moveToNext());
        }
        // Close the cursor
        if (mCursor != null) {
            mCursor.close();
            mCursor = null;
        }
        return mPlaylistList;
    }

    /* Adds the favorites and last added playlists */
    private void makeDefaultPlaylists() {
        final Resources resources = getContext().getResources();

        /* Favorites list */
        final Playlist favorites = new Playlist("-1",
                resources.getString(R.string.playlist_favorites));
        mPlaylistList.add(favorites);

    }
}
