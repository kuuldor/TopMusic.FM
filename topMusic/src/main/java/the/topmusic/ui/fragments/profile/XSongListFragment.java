package the.topmusic.ui.fragments.profile;

import android.view.ContextMenu;
import android.view.Menu;
import android.view.SubMenu;
import android.view.View;
import android.widget.AdapterView;

import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import the.topmusic.R;
import the.topmusic.menu.CreateNewPlaylist;
import the.topmusic.menu.FragmentMenuItems;
import the.topmusic.model.Song;
import the.topmusic.model.XMusicEntry;
import the.topmusic.provider.FavoritesStore;
import the.topmusic.ui.fragments.FragmentIndex;
import the.topmusic.utils.MusicUtils;

/**
 * Created by lucd on 10/14/14.
 */
public class XSongListFragment extends XMusicEntryFragment {
    /**
     * Used to keep context menu items from bleeding into other fragments
     */
    private static final int GROUP_ID = FragmentIndex.XSONGLIST_FRAGMENT_IDX;

    /**
     * Position of a context menu item
     */
    private int mSelectedPosition;

    /**
     * Id of a context menu item
     */
    private String mSelectedId;

    /**
     * Song, album, and artist name used in the context menu
     */
    private String mSongName, mAlbumName, mArtistName, mAlbumID, mArtistID;

    /**
     * {@inheritDoc}
     */
    public void onCreateOptionsMenu(final com.actionbarsherlock.view.Menu menu, final MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        // Queue All
        inflater.inflate(R.menu.queue_all, menu);
        // Add the song to a playlist
        final com.actionbarsherlock.view.SubMenu subMenu = menu.addSubMenu(GROUP_ID, FragmentMenuItems.ADD_TO_PLAYLIST,
                Menu.NONE, R.string.menu_playlist_all);
        MusicUtils.makePlaylistMenu(getSherlockActivity(), GROUP_ID, subMenu, true);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        Song[] songList = getSongList();
        if (songList == null) {
            return true;
        }
        switch (item.getItemId()) {
            case R.id.menu_queue_all_songs:
                // Add all songs to queue;
                MusicUtils.addToQueue(getSherlockActivity(), songList);
                return true;
            case FragmentMenuItems.ADD_TO_FAVORITES:
                FavoritesStore.getInstance(getSherlockActivity()).addSongs(songList);
                return true;
            case FragmentMenuItems.NEW_PLAYLIST:
                CreateNewPlaylist.getInstance(songList).show(getFragmentManager(), "CreatePlaylist");
                return true;
            case FragmentMenuItems.PLAYLIST_SELECTED:
                final String mPlaylistId = item.getIntent().getStringExtra("playlist");
                MusicUtils.addToPlaylist(getSherlockActivity(), songList, mPlaylistId);
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreateContextMenu(final ContextMenu menu, final View v,
                                    final ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        // Get the position of the selected item
        final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        selectEntryAtPosition(info.position);

        final String type = mEntry.mMusicEntryType;
        if (type.equalsIgnoreCase("song")) {
            mSongName = mEntry.mMusicEntryName;
            mAlbumName = mEntry.mJSONItem.optString("album", null);
            mArtistName = mEntry.mJSONItem.optString("artist", null);
            mAlbumID = mEntry.mJSONItem.optString("albumID", null);
            mArtistID = mEntry.mJSONItem.optString("artistID", null);
        } else if (type.equalsIgnoreCase("album")) {
            mSongName = null;
            mAlbumName = mEntry.mMusicEntryName;
            mArtistName = mEntry.mJSONItem.optString("artist", null);
            mAlbumID = mEntry.mMusicEntryId;
            mArtistID = mEntry.mJSONItem.optString("artistID", null);
        } else if (type.equalsIgnoreCase("artist")) {
            mSongName = null;
            mAlbumName = null;
            mArtistName = mEntry.mMusicEntryName;
            mAlbumID = null;
            mArtistID = mEntry.mMusicEntryId;
        } else {
            mSongName = null;
            mAlbumName = null;
            mArtistName = null;
        }

        if (mSongName != null) {
            // Play
            menu.add(GROUP_ID, FragmentMenuItems.PLAY_SELECTION, Menu.NONE,
                    getString(R.string.context_menu_play_selection));

            // Add the song to the queue
            menu.add(GROUP_ID, FragmentMenuItems.ADD_TO_QUEUE, Menu.NONE,
                    getString(R.string.add_to_queue));

            // Add the song to a playlist
            final SubMenu subMenu = menu.addSubMenu(GROUP_ID, FragmentMenuItems.ADD_TO_PLAYLIST,
                    Menu.NONE, R.string.add_to_playlist);
            MusicUtils.makePlaylistMenu(getSherlockActivity(), GROUP_ID, subMenu, true);
//
//            // Make the song a ringtone
//            menu.add(GROUP_ID, FragmentMenuItems.USE_AS_RINGTONE, Menu.NONE,
//                    getString(R.string.context_menu_use_as_ringtone));
        }
    }

    @Override
    public boolean onContextItemSelected(final android.view.MenuItem item) {
        if (item.getGroupId() == GROUP_ID) {
            Song selectedSong = null;
            selectedSong = new Song(mEntry);

            switch (item.getItemId()) {
                case FragmentMenuItems.PLAY_SELECTION:
                    MusicUtils.playAll(getSherlockActivity(), new Song[]{
                            selectedSong
                    }, 0, false);
                    return true;
                case FragmentMenuItems.ADD_TO_QUEUE:
                    MusicUtils.addToQueue(getSherlockActivity(), new Song[]{
                            selectedSong
                    });
                    return true;
                case FragmentMenuItems.ADD_TO_FAVORITES:
                    FavoritesStore.getInstance(getSherlockActivity()).addSongId(
                            mSelectedId, mSongName, mAlbumName, mArtistName, mAlbumID, mArtistID);
                    return true;
                case FragmentMenuItems.NEW_PLAYLIST:
                    CreateNewPlaylist.getInstance(new Song[]{
                            selectedSong
                    }).show(getFragmentManager(), "CreatePlaylist");
                    return true;
                case FragmentMenuItems.PLAYLIST_SELECTED:
                    final String mPlaylistId = item.getIntent().getStringExtra("playlist");
                    MusicUtils.addToPlaylist(getSherlockActivity(), new Song[]{
                            selectedSong
                    }, mPlaylistId);
                    return true;
//                case FragmentMenuItems.USE_AS_RINGTONE:
////                    MusicUtils.setRingtone(getSherlockActivity(), mSelectedId);
//                    return true;
                default:
                    break;
            }
        }
        return super.onContextItemSelected(item);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onItemClick(final AdapterView<?> parent, final View view, final int position,
                            final long id) {
        if (position == 0) {
            return;
        }

        selectEntryAtPosition(position);

        Song[] songList = getSongList();
        MusicUtils.playAll(getSherlockActivity(), songList, position - 1, false);

    }

    protected void selectEntryAtPosition(int position) {
        mSelectedPosition = position - 1;
        mEntry = getAdapter().getItem(mSelectedPosition);
        mSelectedId = mEntry.mMusicEntryId;
    }

    protected Song[] getSongList() {
        Song[] list = null;
        int count = getAdapter().getCount() - 1;
        list = new Song[count];
        for (int i = 0; i < count; i++) {
            XMusicEntry entry = getAdapter().getItem(i);
            list[i] = new Song(entry);
        }

        return list;
    }
}
