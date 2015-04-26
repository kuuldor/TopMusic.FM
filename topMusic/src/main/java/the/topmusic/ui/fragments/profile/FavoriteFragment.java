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

package the.topmusic.ui.fragments.profile;

import android.app.Activity;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;

import java.util.List;

import the.topmusic.R;
import the.topmusic.adapters.ProfileSongAdapter;
import the.topmusic.loaders.FavoritesLoader;
import the.topmusic.menu.CreateNewPlaylist;
import the.topmusic.menu.DeleteDialog;
import the.topmusic.menu.FragmentMenuItems;
import the.topmusic.model.Song;
import the.topmusic.provider.FavoritesStore;
import the.topmusic.recycler.RecycleHolder;
import the.topmusic.ui.fragments.FragmentIndex;
import the.topmusic.utils.MusicUtils;
import the.topmusic.utils.NavUtils;
import the.topmusic.widgets.ProfileTabCarousel;
import the.topmusic.widgets.VerticalScrollListener;

/**
 * This class is used to display all of the songs in {@link FavoritesStore
 * }.
 * 
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class FavoriteFragment extends SherlockFragment implements LoaderCallbacks<List<Song>>,
        OnItemClickListener {

    /**
     * Used to keep context menu items from bleeding into other fragments
     */
    private static final int GROUP_ID = FragmentIndex.FAVORITE_FRAGMENT_IDX;

    /**
     * LoaderCallbacks identifier
     */
    private static final int LOADER = 0;

    /**
     * Fragment UI
     */
    private ViewGroup mRootView;

    /**
     * The adapter for the list
     */
    private ProfileSongAdapter mAdapter;

    /**
     * The list view
     */
    private ListView mListView;

    /**
     * Represents a song
     */
    private Song mSong;

    /**
     * Position of a context menu item
     */
    private int mSelectedPosition;

    /**
     * Id of a context menu item
     */
    private String mSelectedId;

    /**
     * Artist name used in the context menu
     */
    private String mArtistName;

    /**
     * Profile header
     */
    private ProfileTabCarousel mProfileTabCarousel;

    /**
     * Empty constructor as per the {@link Fragment} documentation
     */
    public FavoriteFragment() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onAttach(final Activity activity) {
        super.onAttach(activity);
        mProfileTabCarousel = (ProfileTabCarousel)activity
                .findViewById(R.id.acivity_profile_base_tab_carousel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Create the adpater
        mAdapter = new ProfileSongAdapter(getSherlockActivity(), R.layout.list_item_simple);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
            final Bundle savedInstanceState) {
        // The View for the fragment's UI
        mRootView = (ViewGroup)inflater.inflate(R.layout.list_base, null);
        // Initialize the list
        mListView = (ListView)mRootView.findViewById(R.id.list_base);
        // Set the data behind the list
        mListView.setAdapter(mAdapter);
        // Release any references to the recycled Views
        mListView.setRecyclerListener(new RecycleHolder());
        // Listen for ContextMenus to be created
        mListView.setOnCreateContextMenuListener(this);
        // Play the selected song
        mListView.setOnItemClickListener(this);
        // To help make scrolling smooth
        mListView.setOnScrollListener(new VerticalScrollListener(null, mProfileTabCarousel, 0));
        // Remove the scrollbars and padding for the fast scroll
        mListView.setVerticalScrollBarEnabled(false);
        mListView.setFastScrollEnabled(false);
        mListView.setPadding(0, 0, 0, 0);
        return mRootView;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Enable the options menu
        setHasOptionsMenu(true);
        // Start the loader
        getLoaderManager().initLoader(LOADER, null, this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreateContextMenu(final ContextMenu menu, final View v,
            final ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        // Get the position of the selected item
        final AdapterContextMenuInfo info = (AdapterContextMenuInfo)menuInfo;
        mSelectedPosition = info.position - 1;
        // Creat a new song
        mSong = mAdapter.getItem(mSelectedPosition);
        mSelectedId = mSong.mSongId;
        mArtistName = mSong.mArtistName;

        // Play the song
        menu.add(GROUP_ID, FragmentMenuItems.PLAY_SELECTION, Menu.NONE,
                getString(R.string.context_menu_play_selection));

        // Add the song to the queue
        menu.add(GROUP_ID, FragmentMenuItems.ADD_TO_QUEUE, Menu.NONE,
                getString(R.string.add_to_queue));

        // Add the song to a playlist
        final SubMenu subMenu = menu.addSubMenu(GROUP_ID, FragmentMenuItems.ADD_TO_PLAYLIST,
                Menu.NONE, R.string.add_to_playlist);
        MusicUtils.makePlaylistMenu(getSherlockActivity(), GROUP_ID, subMenu, false);

        // View more content by the song artist
        menu.add(GROUP_ID, FragmentMenuItems.MORE_BY_ARTIST, Menu.NONE,
                getString(R.string.context_menu_more_by_artist));

        // Make the song a ringtone
        menu.add(GROUP_ID, FragmentMenuItems.USE_AS_RINGTONE, Menu.NONE,
                getString(R.string.context_menu_use_as_ringtone));

        // Remove from favorites
        menu.add(GROUP_ID, FragmentMenuItems.REMOVE_FROM_FAVORITES, Menu.NONE,
                getString(R.string.remove_from_favorites));

        // Delete the song
        menu.add(GROUP_ID, FragmentMenuItems.DELETE, Menu.NONE,
                getString(R.string.context_menu_delete));
    }

    @Override
    public boolean onContextItemSelected(final android.view.MenuItem item) {
        if (item.getGroupId() == GROUP_ID) {
            switch (item.getItemId()) {
                case FragmentMenuItems.PLAY_SELECTION:
                    MusicUtils.playAll(getSherlockActivity(), new Song[]{
                            mSong
                    }, 0, false);
                    return true;
                case FragmentMenuItems.ADD_TO_QUEUE:
                    MusicUtils.addToQueue(getSherlockActivity(), new Song[]{
                            mSong
                    });
                    return true;
                case FragmentMenuItems.NEW_PLAYLIST:
                    CreateNewPlaylist.getInstance(new Song[]{
                            mSong
                    }).show(getFragmentManager(), "CreatePlaylist");
                    return true;
                case FragmentMenuItems.PLAYLIST_SELECTED:
                    final String mPlaylistId = item.getIntent().getStringExtra("playlist");
                    MusicUtils.addToPlaylist(getSherlockActivity(), new Song[]{
                            mSong
                    }, mPlaylistId);
                    return true;
                case FragmentMenuItems.MORE_BY_ARTIST:
                    NavUtils.openArtistProfile(getSherlockActivity(), mArtistName);
                    return true;
                case FragmentMenuItems.USE_AS_RINGTONE:
                    MusicUtils.setRingtone(getSherlockActivity(), mSelectedId);
                    return true;
                case FragmentMenuItems.REMOVE_FROM_FAVORITES:
                    FavoritesStore.getInstance(getSherlockActivity()).removeItem(
                            mSelectedId);
                    getLoaderManager().restartLoader(LOADER, null, this);
                    return true;
                case FragmentMenuItems.DELETE:
                    DeleteDialog.newInstance(mSong.mSongName, new String[] {
                        mSelectedId
                    }, null).show(getFragmentManager(), "DeleteDialog");
                    SystemClock.sleep(10);
                    mAdapter.notifyDataSetChanged();
                    getLoaderManager().restartLoader(LOADER, null, this);
                    return true;
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
        final Song[] list = getSongList();
        MusicUtils.playAll(getSherlockActivity(), list, position - 1, false);
    }

    protected Song[] getSongList() {
        Song[] list = null;
        int count = mAdapter.getCount() - 1;
        list = new Song[count];
        for (int i = 0; i < count; i++) {
            list[i] = mAdapter.getItem(i);
        }

        return list;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Loader<List<Song>> onCreateLoader(final int id, final Bundle args) {
        return new FavoritesLoader(getSherlockActivity());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onLoadFinished(final Loader<List<Song>> loader, final List<Song> data) {
        // Check for any errors
        if (data.isEmpty()) {
            // Set the empty text
            final TextView empty = (TextView)mRootView.findViewById(R.id.empty);
            empty.setText(getString(R.string.empty_favorite));
            mListView.setEmptyView(empty);
            return;
        }

        // Start fresh
        mAdapter.unload();
        // Return the correct count
        mAdapter.setCount(data);
        // Add the data to the adpater
        for (final Song song : data) {
            mAdapter.add(song);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onLoaderReset(final Loader<List<Song>> loader) {
        // Clear the data in the adapter
        mAdapter.unload();
    }

}
