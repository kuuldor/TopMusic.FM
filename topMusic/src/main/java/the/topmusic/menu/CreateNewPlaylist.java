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

package the.topmusic.menu;

import android.app.Dialog;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;

import the.topmusic.R;
import the.topmusic.format.Capitalize;
import the.topmusic.model.Song;
import the.topmusic.provider.MusicStore;
import the.topmusic.utils.MusicUtils;


/**
 * @author Andrew Neal (andrewdneal@gmail.com) TODO - The playlist names are
 *         automatically capitalized to help when you want to play one via voice
 *         actions, but it really needs to work either way. As in, capitalized
 *         or not.
 */
public class CreateNewPlaylist extends BasePlaylistDialog {

    // The playlist list
    private Song[] mPlaylistList = new Song[]{};

    /**
     * @param list The list of tracks to add to the playlist
     * @return A new instance of this dialog.
     */
    public static CreateNewPlaylist getInstance(final Song[] list) {
        final CreateNewPlaylist frag = new CreateNewPlaylist();
        final Bundle args = new Bundle();
        args.putParcelableArray("playlist_list", list);
        frag.setArguments(args);
        return frag;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onSaveInstanceState(final Bundle outcicle) {
        outcicle.putString("defaultname", mPlaylist.getText().toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initObjects(final Bundle savedInstanceState) {
        mPlaylistList = (Song[]) getArguments().getParcelableArray("playlist_list");
        mDefaultname = savedInstanceState != null ? savedInstanceState.getString("defaultname")
                : makePlaylistName();
        if (mDefaultname == null) {
            getDialog().dismiss();
            return;
        }
        final String prromptformat = getString(R.string.create_playlist_prompt);
        mPrompt = String.format(prromptformat, mDefaultname);
    }

    @Override
    public void onSaveClick() {
        final String playlistName = mPlaylist.getText().toString();
        if (playlistName != null && playlistName.length() > 0) {
            final String playlistId = MusicUtils.getIdForPlaylist(getSherlockActivity(),
                    playlistName);
            if (playlistId != null) {
                MusicUtils.clearPlaylist(getSherlockActivity(), playlistId);
                MusicUtils.addToPlaylist(getSherlockActivity(), mPlaylistList, playlistId);
            } else {
                final String newId = MusicUtils.createPlaylist(getSherlockActivity(),
                        Capitalize.capitalize(playlistName));
                MusicUtils.addToPlaylist(getSherlockActivity(), mPlaylistList, newId);
            }
            closeKeyboard();
            getDialog().dismiss();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onTextChangedListener() {
        final String playlistName = mPlaylist.getText().toString();
        mSaveButton = mPlaylistDialog.getButton(Dialog.BUTTON_POSITIVE);
        if (mSaveButton == null) {
            return;
        }
        if (playlistName.trim().length() == 0) {
            mSaveButton.setEnabled(false);
        } else {
            mSaveButton.setEnabled(true);
            if (MusicUtils.getIdForPlaylist(getSherlockActivity(), playlistName) != null) {
                mSaveButton.setText(R.string.overwrite);
            } else {
                mSaveButton.setText(R.string.save);
            }
        }
    }

    private String makePlaylistName() {
        final String template = getString(R.string.new_playlist_name_template);
        int num = 1;
        final String[] projection = new String[]{
                MusicStore.BasicColumns.NAME
        };
        final SQLiteDatabase db = MusicStore.getInstance(getSherlockActivity()).getReadableDatabase();
        final String selection = MusicStore.BasicColumns.NAME + " != ''";
        Cursor cursor = db.query(MusicStore.TableNames.PLAYLIST, projection,
                selection, null, null, null, MusicStore.BasicColumns.NAME);
        if (cursor == null) {
            return null;
        }

        String suggestedname;
        suggestedname = String.format(template, num++);
        boolean done = false;
        while (!done) {
            done = true;
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                final String playlistname = cursor.getString(0);
                if (playlistname.compareToIgnoreCase(suggestedname) == 0) {
                    suggestedname = String.format(template, num++);
                    done = false;
                }
                cursor.moveToNext();
            }
        }
        cursor.close();
        cursor = null;
        return suggestedname;
    }
}
