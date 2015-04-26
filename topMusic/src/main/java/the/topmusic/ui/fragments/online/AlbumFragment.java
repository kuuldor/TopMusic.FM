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

package the.topmusic.ui.fragments.online;

import android.content.Context;
import android.os.Bundle;

import the.topmusic.Config;
import the.topmusic.R;
import the.topmusic.ui.fragments.FragmentIndex;
import the.topmusic.utils.NavUtils;

/**
 * This class is used to display all of the albums on a user's device.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class AlbumFragment extends XMusicListFragment {

    /**
     * Used to keep context menu items from bleeding into other fragments
     */
    private static final int GROUP_ID = FragmentIndex.ALBUM_FRAGMENT_IDX;

    /**
     * Grid view column count. ONE - list, TWO - normal grid, FOUR - landscape
     */
    private static final int ONE = 1, TWO = 2, FOUR = 4;

    /**
     * LoaderCallbacks identifier
     */
    private static final int LOADER = 0;

    @Override
    protected void onXMEntryClick() {
        if (mEntry.mMusicEntryType.equalsIgnoreCase("album")) {
            //Open Album profile
            NavUtils.openXMusicEntry(getSherlockActivity(), mEntry, "Album");
            //remove the entry from stack
            mEntry = navigationStack.pop();
        } else {
            super.onXMEntryClick();
        }
    }

    @Override
    protected int getListItemLayout() {
        return R.layout.list_item_normal;
    }

    @Override
    protected Bundle createRootArgs(Context context) {
        return createParams(context, "TopMusicArtistsRoot", "Top Artists", Config.ALBUM_URL_PATH);
    }
}
