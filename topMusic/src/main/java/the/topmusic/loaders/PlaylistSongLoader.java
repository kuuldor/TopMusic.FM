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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import the.topmusic.model.Song;
import the.topmusic.provider.MusicStore;
import the.topmusic.utils.Lists;

/**
 * Used to query Playlist table and
 * return the songs for a particular playlist.
 * 
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class PlaylistSongLoader extends WrappedAsyncTaskLoader<List<Song>> {

    /**
     * The result
     */
    private final ArrayList<Song> mSongList = Lists.newArrayList();

    /**
     * The Id of the playlist the songs belong to.
     */
    private final String mPlaylistID;

    /**
     * Constructor of <code>SongLoader</code>
     *
     * @param context    The {@link android.content.Context} to use
     * @param playlistId
     */
    public PlaylistSongLoader(final Context context, final String playlistId) {
        super(context);
        mPlaylistID = playlistId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Song> loadInBackground() {

        Song[] songs = MusicStore.getInstance(getContext()).getSongListForPlaylist(mPlaylistID);
        // Gather the data
        if (songs != null && songs.length > 0) {
            Collections.addAll(mSongList, songs);
        }

        return mSongList;
    }

}
