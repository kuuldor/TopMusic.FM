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
import the.topmusic.utils.Lists;
import the.topmusic.utils.MusicUtils;

/**
 * Used to return the current playlist or queue.
 * 
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class QueueLoader extends WrappedAsyncTaskLoader<List<Song>> {

    /**
     * The result
     */
    private final ArrayList<Song> mSongList = Lists.newArrayList();

    /**
     * Constructor of <code>QueueLoader</code>
     * 
     * @param context The {@link Context} to use
     */
    public QueueLoader(final Context context) {
        super(context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Song> loadInBackground() {
        // Create the Cursor
        Song[] songs = MusicUtils.getQueue();
        // Gather the data
        Collections.addAll(mSongList, songs);
        return mSongList;
    }
}
