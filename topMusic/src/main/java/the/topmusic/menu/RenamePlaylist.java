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
import android.content.ContentValues;
import android.database.Cursor;
import android.os.Bundle;

import the.topmusic.R;
import the.topmusic.format.Capitalize;
import the.topmusic.provider.MusicStore;
import the.topmusic.utils.MusicUtils;


/**
 * Alert dialog used to rename playlits.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class RenamePlaylist extends BasePlaylistDialog {

    private String mOriginalName;

    private long mRenameId;

    /**
     * @param id The Id of the playlist to rename
     * @return A new instance of this dialog.
     */
    public static RenamePlaylist getInstance(final Long id) {
        final RenamePlaylist frag = new RenamePlaylist();
        final Bundle args = new Bundle();
        args.putLong("rename", id);
        frag.setArguments(args);
        return frag;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onSaveInstanceState(final Bundle outcicle) {
        outcicle.putString("defaultname", mPlaylist.getText().toString());
        outcicle.putLong("rename", mRenameId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initObjects(final Bundle savedInstanceState) {
        mRenameId = savedInstanceState != null ? savedInstanceState.getLong("rename")
                : getArguments().getLong("rename", -1);
        mOriginalName = getPlaylistNameFromId(mRenameId);
        mDefaultname = savedInstanceState != null ? savedInstanceState.getString("defaultname")
                : mOriginalName;
        if (mRenameId < 0 || mOriginalName == null || mDefaultname == null) {
            getDialog().dismiss();
            return;
        }
        final String promptformat = getString(R.string.create_playlist_prompt);
        mPrompt = String.format(promptformat, mOriginalName, mDefaultname);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onSaveClick() {
        final String playlistName = mPlaylist.getText().toString();
        if (playlistName != null && playlistName.length() > 0) {
            final ContentValues values = new ContentValues(1);
            values.put(MusicStore.BasicColumns.NAME, Capitalize.capitalize(playlistName));
            values.put(MusicStore.BasicColumns.ID, mRenameId);
            MusicStore.getInstance(getSherlockActivity()).getWritableDatabase().update(MusicStore.TableNames.PLAYLIST, values,
                    MusicStore.BasicColumns.ID + "=?", new String[]{
                            Long.valueOf(mRenameId).toString()
                    });
            closeKeyboard();
            getDialog().dismiss();
        }
    }

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

    /**
     * @param id The Id of the playlist
     * @return The name of the playlist
     */
    private String getPlaylistNameFromId(final long id) {
        Cursor cursor = MusicStore.getInstance(getSherlockActivity()).getReadableDatabase().query(
                MusicStore.TableNames.PLAYLIST, new String[]{
                        MusicStore.BasicColumns.NAME
                }, MusicStore.BasicColumns.ID + "=?", new String[]{
                        Long.valueOf(id).toString()
                }, null, null, MusicStore.BasicColumns.NAME);
        String playlistName = null;
        if (cursor != null) {
            cursor.moveToFirst();
            if (!cursor.isAfterLast()) {
                playlistName = cursor.getString(0);
            }
            cursor.close();
        }
        cursor = null;
        return playlistName;
    }

}
