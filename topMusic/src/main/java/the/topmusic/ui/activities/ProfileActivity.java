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

package the.topmusic.ui.activities;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.View;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import the.topmusic.Config;
import the.topmusic.R;
import the.topmusic.adapters.PagerAdapter;
import the.topmusic.cache.ImageFetcher;
import the.topmusic.menu.PhotoSelectionDialog;
import the.topmusic.menu.PhotoSelectionDialog.ProfileType;
import the.topmusic.model.Song;
import the.topmusic.ui.fragments.profile.AlbumSongFragment;
import the.topmusic.ui.fragments.profile.ArtistAlbumFragment;
import the.topmusic.ui.fragments.profile.ArtistSongFragment;
import the.topmusic.ui.fragments.profile.FavoriteFragment;
import the.topmusic.ui.fragments.profile.GenreSongFragment;
import the.topmusic.ui.fragments.profile.LastAddedFragment;
import the.topmusic.ui.fragments.profile.PlaylistSongFragment;
import the.topmusic.ui.fragments.profile.XChoiceEntryFragment;
import the.topmusic.ui.fragments.profile.XMusicEntryFragment;
import the.topmusic.ui.fragments.profile.XSongListFragment;
import the.topmusic.utils.MusicUtils;
import the.topmusic.utils.PreferenceUtils;
import the.topmusic.utils.SortOrder;
import the.topmusic.utils.TopMusicUtils;
import the.topmusic.widgets.ProfileTabCarousel;
import the.topmusic.widgets.ProfileTabCarousel.Listener;

/**
 * The {@link SherlockFragmentActivity} is used to display the data for specific
 * artists, albums, playlists, and genres. This class is only used on phones.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class ProfileActivity extends BaseActivity implements OnPageChangeListener, Listener {

    private static final int NEW_PHOTO = 1;

    /**
     * View pager
     */
    private ViewPager mViewPager;

    /**
     * Pager adpater
     */
    private PagerAdapter mPagerAdapter;

    /**
     * Profile header carousel
     */
    private ProfileTabCarousel mTabCarousel;

//    /**
//     * Artist name passed into the class
//     */
//    private String mArtistName;
//
//    /**
//     * The main profile title
//     */
//    private String mProfileName;

    /**
     * Image cache
     */
    private ImageFetcher mImageFetcher;

    private PreferenceUtils mPreferences;
    private ProfileDataSource mDataSource;

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Temporay until I can work out a nice landscape layout
//        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Get the preferences
        mPreferences = PreferenceUtils.getInstace(this);

        // Initialze the image fetcher
        mImageFetcher = TopMusicUtils.getImageFetcher(this);

        // Initialize the Bundle
        Bundle arguments = savedInstanceState != null ? savedInstanceState : getIntent().getExtras();
        // Get the MIME type
        String type = arguments.getString(Config.MIME_TYPE);

        mDataSource = new DataSourceFactory(type).createDataSource(arguments);

        // Initialize the pager adapter
        mPagerAdapter = new PagerAdapter(this);

        // Initialze the carousel
        mTabCarousel = (ProfileTabCarousel) findViewById(R.id.acivity_profile_base_tab_carousel);
        mTabCarousel.reset();
        mTabCarousel.getPhoto().setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(final View v) {
                mDataSource.photoClickHandler();
            }
        });
        // Set up the action bar
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        mDataSource.setupPages();

        // Initialize the ViewPager
        mViewPager = (ViewPager) findViewById(R.id.acivity_profile_base_pager);
        // Attch the adapter
        mViewPager.setAdapter(mPagerAdapter);
        // Offscreen limit
        mViewPager.setOffscreenPageLimit(mPagerAdapter.getCount() - 1);
        // Attach the page change listener
        mViewPager.setOnPageChangeListener(this);
        // Attach the carousel listener
        mTabCarousel.setListener(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onPause() {
        super.onPause();
        mImageFetcher.flush();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int setContentView() {
        return R.layout.activity_profile_base;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onPrepareOptionsMenu(final Menu menu) {
        String title = mDataSource.playMenuTitle();
        if (title != null) {
            // Set the shuffle all title to "play all" if a playlist.
            final MenuItem shuffle = menu.findItem(R.id.menu_shuffle);

            shuffle.setTitle(title);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        String title = mDataSource.playMenuTitle();
        if (title != null) {
            // Shuffle
            getSupportMenuInflater().inflate(R.menu.shuffle, menu);
        }
        // Sort orders
        int sortMenu = mDataSource.sortMenuId();
        if (sortMenu != 0) {
            getSupportMenuInflater().inflate(sortMenu, menu);
        }

        return super.onCreateOptionsMenu(menu);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                goBack();
                return true;
            case R.id.menu_shuffle:
                mDataSource.playAllSong(this);

                return true;
            case R.id.menu_sort_by_az:
            case R.id.menu_sort_by_za:
            case R.id.menu_sort_by_album:
            case R.id.menu_sort_by_year:
            case R.id.menu_sort_by_duration:
            case R.id.menu_sort_by_date_added:
                mDataSource.setSortOrder(item.getItemId());
                return true;
            case R.id.menu_sort_by_track_list:
                mPreferences.setAlbumSongSortOrder(SortOrder.AlbumSongSortOrder.SONG_TRACK_LIST);
                getAlbumSongFragment().refresh();
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
    protected void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putAll(mDataSource.getArguments());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        goBack();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onPageScrolled(final int position, final float positionOffset,
                               final int positionOffsetPixels) {
        if (mViewPager.isFakeDragging()) {
            return;
        }

        final int scrollToX = (int) ((position + positionOffset) * mTabCarousel
                .getAllowedHorizontalScrollLength());
        mTabCarousel.scrollTo(scrollToX, 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onPageSelected(final int position) {
        mTabCarousel.setCurrentTab(position);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onPageScrollStateChanged(final int state) {
        if (state == ViewPager.SCROLL_STATE_IDLE) {
            mTabCarousel.restoreYCoordinate(25, mViewPager.getCurrentItem());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onTouchDown() {
        if (!mViewPager.isFakeDragging()) {
            mViewPager.beginFakeDrag();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onTouchUp() {
        if (mViewPager.isFakeDragging()) {
            mViewPager.endFakeDrag();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onScrollChanged(final int l, final int t, final int oldl, final int oldt) {
        if (mViewPager.isFakeDragging()) {
            mViewPager.fakeDragBy(oldl - l);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onTabSelected(final int position) {
        mViewPager.setCurrentItem(position);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == NEW_PHOTO) {
            if (resultCode == RESULT_OK) {
                final Uri selectedImage = data.getData();
                final String[] filePathColumn = {
                        MediaStore.Images.Media.DATA
                };

                Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null,
                        null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    final int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                    final String picturePath = cursor.getString(columnIndex);
                    cursor.close();
                    cursor = null;

                    String key = mDataSource.cacheKey();

                    final Bitmap bitmap = ImageFetcher.decodeSampledBitmapFromFile(picturePath);
                    mImageFetcher.addBitmapToCache(key, bitmap);
                    if (mDataSource.isAlbum()) {
                        mTabCarousel.getAlbumArt().setImageBitmap(bitmap);
                    } else {
                        mTabCarousel.getPhoto().setImageBitmap(bitmap);
                    }
                }
            } else {
                selectOldPhoto();
            }
        }
    }

    /**
     * Starts an activity for result that returns an image from the Gallery.
     */
    public void selectNewPhoto() {
        // First remove the old image
        removeFromCache();
        // Now open the gallery
        final Intent intent = new Intent(Intent.ACTION_GET_CONTENT, null);
        intent.setType("image/*");
        startActivityForResult(intent, NEW_PHOTO);
        // Make sure the notfication starts
        if (MusicUtils.isPlaying()) {
            MusicUtils.startBackgroundService(this);
        }
    }

    /**
     * Fetchs for the artist or album art, other wise sets the default header
     * image.
     */
    public void selectOldPhoto() {
        // First remove the old image
        removeFromCache();
        // Apply the old photo
        if (mDataSource.isArtist()) {
            mTabCarousel.setArtistProfileHeader(this, mDataSource.artistName());
        } else if (mDataSource.isAlbum()) {
            mTabCarousel.setAlbumProfileHeader(this, mDataSource.profileName(), mDataSource.artistName());
        } else {
            mTabCarousel.setPlaylistOrGenreProfileHeader(this, mDataSource.profileName());
        }
    }

    /**
     * When the user chooses {@code #selectOldPhoto()} while viewing an album
     * profile, the image is, most likely, reverted back to the locally found
     * artwork. This is specifically for fetching the image from Last.fm.
     */
    public void fetchAlbumArt() {
        // First remove the old image
        removeFromCache();
        // Fetch for the artwork
        mTabCarousel.fetchAlbumPhoto(this, mDataSource.profileName());
    }

    /**
     * Searches Google for the artist or album
     */
    public void googleSearch() {
        String query = mDataSource.googleQuery();

        final Intent googleSearch = new Intent(Intent.ACTION_WEB_SEARCH);
        googleSearch.putExtra(SearchManager.QUERY, query);
        startActivity(googleSearch);
        // Make sure the notification starts.
        MusicUtils.startBackgroundService(this);
    }

    /**
     * Removes the header image from the cache.
     */
    private void removeFromCache() {
        String key = mDataSource.cacheKey();

        mImageFetcher.removeFromCache(key);
        // Give the disk cache a little time before requesting a new image.
        SystemClock.sleep(80);
    }

    /**
     * Finishes the activity and overrides the default animation.
     */
    private void goBack() {
        finish();
    }

    private ArtistSongFragment getArtistSongFragment() {
        return (ArtistSongFragment) mPagerAdapter.getFragment(0);
    }

    private ArtistAlbumFragment getArtistAlbumFragment() {
        return (ArtistAlbumFragment) mPagerAdapter.getFragment(1);
    }

    private AlbumSongFragment getAlbumSongFragment() {
        return (AlbumSongFragment) mPagerAdapter.getFragment(0);
    }

    public XMusicEntryFragment getChartSongsFragment() {
        return (XMusicEntryFragment) mPagerAdapter.getFragment(0);
    }

    public XChoiceEntryFragment getChartListFragment() {
        return (XChoiceEntryFragment) mPagerAdapter.getFragment(1);
    }

    public XMusicEntryFragment getHotSongsFragment() {
        return (XMusicEntryFragment) mPagerAdapter.getFragment(1);
    }

    @Override
    protected void searchText(String query) {
        SherlockFragment fragment = (SherlockFragment) mPagerAdapter.getFragment(mPagerAdapter.getCurrentPage());
        if (fragment instanceof SearchProvider) {
            SearchProvider searchProvider = (SearchProvider) fragment;
            searchProvider.searchText(this, query);
        } else {
            super.searchText(query);
        }
    }

    private abstract class ProfileDataSource {
        /**
         * The Bundle to pass into the Fragments
         */
        protected Bundle mArguments;
        protected String mType;
        protected String mProfileName;

        protected ProfileDataSource(Bundle arguments) {
            this.mArguments = arguments;
            mType = mArguments.getString(Config.MIME_TYPE);
            mProfileName = mArguments.getString(Config.NAME);
        }

        Bundle getArguments() {
            return mArguments;
        }

        String dataType() {
            return mType;
        }

        String profileName() {
            return mProfileName;
        }

        String cacheKey() {
            return profileName();
        }

        String googleQuery() {
            return profileName();
        }

        abstract String artistName();

        abstract String playMenuTitle();

        abstract int sortMenuId();

        abstract void photoClickHandler();

        abstract void setupPages();

        abstract boolean isArtist();

        abstract boolean isAlbum();


        public String itemID() {
            return mArguments.getString(Config.ID);
        }

        public abstract void playAllSong(Context context);

        public void setSortOrder(int itemId) {
        }
    }

    private class DataSourceFactory {
        private String mDataSourceType;

        public DataSourceFactory(String type) {
            mDataSourceType = type;
        }

        /**
         * @return True if the MIME type is vnd.android.cursor.dir/artists, false
         * otherwise.
         */
        private boolean isArtist() {
            return mDataSourceType.equals(MediaStore.Audio.Artists.CONTENT_TYPE);
        }

        /**
         * @return True if the MIME type is vnd.android.cursor.dir/albums, false
         * otherwise.
         */
        private boolean isAlbum() {
            return mDataSourceType.equals(MediaStore.Audio.Albums.CONTENT_TYPE);
        }

        /**
         * @return True if the MIME type is vnd.android.cursor.dir/gere, false
         * otherwise.
         */
        private boolean isGenre() {
            return mDataSourceType.equals(MediaStore.Audio.Genres.CONTENT_TYPE);
        }

        /**
         * @return True if the MIME type is vnd.android.cursor.dir/playlist, false
         * otherwise.
         */
        private boolean isPlaylist() {
            return mDataSourceType.equals("Playlist");
        }

        private boolean isOnlineChart() {
            return mDataSourceType.equalsIgnoreCase("Chart");
        }

        private boolean isOnlineAlbum() {
            return mDataSourceType.equalsIgnoreCase("Album");
        }

        private boolean isOnlineArtist() {
            return mDataSourceType.equalsIgnoreCase("Artist");
        }

        /**
         * @return True if the MIME type is "Favorites", false otherwise.
         */
        private boolean isFavorites() {
            return mDataSourceType.equals(getString(R.string.playlist_favorites));
        }

        /**
         * @return True if the MIME type is "LastAdded", false otherwise.
         */
        private boolean isLastAdded() {
            return mDataSourceType.equals(getString(R.string.playlist_last_added));
        }

        ProfileDataSource createDataSource(Bundle arguments) {
            ProfileDataSource dataSource = null;
            do {
                if (isArtist()) {
                    dataSource = new ArtistDataSource(arguments);
                    break;
                }
                if (isAlbum()) {
                    dataSource = new AlbumDataSource(arguments);
                    break;
                }
                if (isPlaylist()) {
                    dataSource = new PlaylistDataSource(arguments);
                    break;
                }
                if (isGenre()) {
                    dataSource = new GenreDataSource(arguments);
                    break;
                }
                if (isFavorites()) {
                    dataSource = new FavoritesDataSource(arguments);
                    break;
                }
                if (isLastAdded()) {
                    dataSource = new LastAddedDataSource(arguments);
                    break;
                }
                if (isOnlineChart()) {
                    dataSource = new OnlineChartDataSource(arguments);
                    break;
                }
                if (isOnlineAlbum()) {
                    dataSource = new OnlineAlbumDataSource(arguments);
                    break;
                }
                if (isOnlineArtist()) {
                    dataSource = new OnlineArtistDataSource(arguments);
                    break;
                }
            } while (false);

            return dataSource;
        }
    }

    private class ArtistDataSource extends ProfileDataSource {
        private String mArtistName;

        public ArtistDataSource(Bundle arguments) {
            super(arguments);
            mArtistName = mArguments.getString(Config.ARTIST_NAME);
        }

        private boolean isArtistSongPage() {
            return mViewPager.getCurrentItem() == 0;
        }

        private boolean isArtistAlbumPage() {
            return mViewPager.getCurrentItem() == 1;
        }

        @Override
        String cacheKey() {
            return artistName();
        }

        @Override
        String googleQuery() {
            return artistName();
        }

        @Override
        String artistName() {
            return mArtistName;
        }

        @Override
        String playMenuTitle() {
            return getString(R.string.menu_shuffle);
        }

        @Override
        int sortMenuId() {
            int id = 0;
            if (isArtistSongPage()) {
                id = R.menu.artist_song_sort_by;
            } else if (isArtistAlbumPage()) {
                id = R.menu.artist_album_sort_by;
            }
            return id;
        }

        @Override
        void photoClickHandler() {
            ProfileType profileType;
            profileType = ProfileType.ARTIST;
            PhotoSelectionDialog.newInstance(artistName(),
                    profileType).show(getSupportFragmentManager(), "PhotoSelectionDialog");
        }

        @Override
        void setupPages() {
            // Add the carousel images
            mTabCarousel.setArtistProfileHeader(ProfileActivity.this, mArtistName);

            // Artist profile fragments
            mPagerAdapter.add(ArtistSongFragment.class, mArguments);
            mPagerAdapter.add(ArtistAlbumFragment.class, mArguments);

            // Action bar title
            mResources.setTitle(mArtistName);
        }

        @Override
        boolean isArtist() {
            return true;
        }

        @Override
        boolean isAlbum() {
            return false;
        }

        @Override
        public void playAllSong(Context context) {
            final String stringId = itemID();
            Song[] list = MusicUtils.getSongListForArtist(context, stringId);
            if (list != null && list.length > 0) {
                MusicUtils.playAll(context, list, 0, true);
            }
        }

        @Override
        public void setSortOrder(int itemId) {
            switch (itemId) {
                case R.id.menu_sort_by_az:
                    if (isArtistSongPage()) {
                        mPreferences.setArtistSongSortOrder(SortOrder.ArtistSongSortOrder.SONG_A_Z);
                        getArtistSongFragment().refresh();
                    } else if (isArtistAlbumPage()) {
                        mPreferences.setArtistAlbumSortOrder(SortOrder.ArtistAlbumSortOrder.ALBUM_A_Z);
                        getArtistAlbumFragment().refresh();
                    }
                    break;
                case R.id.menu_sort_by_za:
                    if (isArtistSongPage()) {
                        mPreferences.setArtistSongSortOrder(SortOrder.ArtistSongSortOrder.SONG_Z_A);
                        getArtistSongFragment().refresh();
                    } else if (isArtistAlbumPage()) {
                        mPreferences.setArtistAlbumSortOrder(SortOrder.ArtistAlbumSortOrder.ALBUM_Z_A);
                        getArtistAlbumFragment().refresh();
                    }
                    break;
                case R.id.menu_sort_by_album:
                    if (isArtistSongPage()) {
                        mPreferences.setArtistSongSortOrder(SortOrder.ArtistSongSortOrder.SONG_ALBUM);
                        getArtistSongFragment().refresh();
                    }
                    break;
                case R.id.menu_sort_by_year:
                    if (isArtistSongPage()) {
                        mPreferences.setArtistSongSortOrder(SortOrder.ArtistSongSortOrder.SONG_YEAR);
                        getArtistSongFragment().refresh();
                    } else if (isArtistAlbumPage()) {
                        mPreferences.setArtistAlbumSortOrder(SortOrder.ArtistAlbumSortOrder.ALBUM_YEAR);
                        getArtistAlbumFragment().refresh();
                    }
                    break;
                case R.id.menu_sort_by_duration:
                    if (isArtistSongPage()) {
                        mPreferences
                                .setArtistSongSortOrder(SortOrder.ArtistSongSortOrder.SONG_DURATION);
                        getArtistSongFragment().refresh();
                    } else {
                        mPreferences.setAlbumSongSortOrder(SortOrder.AlbumSongSortOrder.SONG_DURATION);
                        getAlbumSongFragment().refresh();
                    }
                    break;
                case R.id.menu_sort_by_date_added:
                    if (isArtistSongPage()) {
                        mPreferences.setArtistSongSortOrder(SortOrder.ArtistSongSortOrder.SONG_DATE);
                        getArtistSongFragment().refresh();
                    }
                    break;
            }
        }
    }

    private class AlbumDataSource extends ProfileDataSource {
        private String mArtistName;

        public AlbumDataSource(Bundle arguments) {
            super(arguments);
            mArtistName = mArguments.getString(Config.ARTIST_NAME);
        }

        @Override
        String cacheKey() {
            return profileName() + Config.ALBUM_ART_SUFFIX;
        }

        @Override
        String googleQuery() {
            return profileName() + " " + artistName();
        }

        @Override
        String artistName() {
            return mArtistName;
        }

        @Override
        String playMenuTitle() {
            return getString(R.string.menu_shuffle);
        }

        @Override
        int sortMenuId() {
            return R.menu.album_song_sort_by;
        }

        @Override
        void photoClickHandler() {
            ProfileType profileType;
            profileType = ProfileType.ALBUM;
            PhotoSelectionDialog.newInstance(profileName(),
                    profileType).show(getSupportFragmentManager(), "PhotoSelectionDialog");
        }

        @Override
        void setupPages() {
            // Add the carousel images
            mTabCarousel.setAlbumProfileHeader(ProfileActivity.this, mProfileName, mArtistName);

            // Album profile fragments
            mPagerAdapter.add(AlbumSongFragment.class, mArguments);

            // Action bar title = album name
            mResources.setTitle(mProfileName);
            // Action bar subtitle = year released
            mResources.setSubtitle(mArguments.getString(Config.ALBUM_YEAR));
        }

        @Override
        boolean isArtist() {
            return false;
        }

        @Override
        boolean isAlbum() {
            return true;
        }

        @Override
        public void playAllSong(Context context) {
            final String stringId = itemID();
            Song[] list = MusicUtils.getSongListForAlbum(context, stringId);
            if (list != null && list.length > 0) {
                MusicUtils.playAll(context, list, 0, true);
            }
        }

        @Override
        public void setSortOrder(int itemId) {
            switch (itemId) {
                case R.id.menu_sort_by_az:
                    mPreferences.setAlbumSongSortOrder(SortOrder.AlbumSongSortOrder.SONG_A_Z);
                    getAlbumSongFragment().refresh();
                    break;

                case R.id.menu_sort_by_za:
                    mPreferences.setAlbumSongSortOrder(SortOrder.AlbumSongSortOrder.SONG_Z_A);
                    getAlbumSongFragment().refresh();
                    break;

                case R.id.menu_sort_by_duration:
                    mPreferences.setAlbumSongSortOrder(SortOrder.AlbumSongSortOrder.SONG_DURATION);
                    getAlbumSongFragment().refresh();
                    break;
            }
        }
    }

    private class PlaylistDataSource extends ProfileDataSource {
        public PlaylistDataSource(Bundle arguments) {
            super(arguments);
        }

        @Override
        String artistName() {
            return null;
        }

        @Override
        String playMenuTitle() {
            return getString(R.string.menu_play_all);
        }

        @Override
        int sortMenuId() {
            return 0;
        }

        @Override
        void photoClickHandler() {
            ProfileType profileType;
            profileType = ProfileType.OTHER;
            PhotoSelectionDialog.newInstance(profileName(),
                    profileType).show(getSupportFragmentManager(), "PhotoSelectionDialog");
        }

        @Override
        void setupPages() {
            // Add the carousel images
            mTabCarousel.setPlaylistOrGenreProfileHeader(ProfileActivity.this, mProfileName);

            // Playlist profile fragments
            mPagerAdapter.add(PlaylistSongFragment.class, mArguments);

            // Action bar title = playlist name
            mResources.setTitle(mProfileName);
        }

        @Override
        boolean isArtist() {
            return false;
        }

        @Override
        boolean isAlbum() {
            return false;
        }

        @Override
        public void playAllSong(Context context) {
            final String stringId = itemID();
            MusicUtils.playPlaylist(context, stringId);
        }
    }

    private class FavoritesDataSource extends ProfileDataSource {
        public FavoritesDataSource(Bundle arguments) {
            super(arguments);
        }

        @Override
        String artistName() {
            return null;
        }

        @Override
        String playMenuTitle() {
            return getString(R.string.menu_play_all);
        }

        @Override
        int sortMenuId() {
            return 0;
        }

        @Override
        void photoClickHandler() {
            ProfileType profileType;
            profileType = ProfileType.OTHER;
            PhotoSelectionDialog.newInstance(profileName(),
                    profileType).show(getSupportFragmentManager(), "PhotoSelectionDialog");
        }

        @Override
        void setupPages() {
            // Add the carousel images
            mTabCarousel.setPlaylistOrGenreProfileHeader(ProfileActivity.this, mProfileName);

            // Favorite fragment
            mPagerAdapter.add(FavoriteFragment.class, null);

            // Action bar title = Favorites
            mResources.setTitle(mProfileName);
        }

        @Override
        boolean isArtist() {
            return false;
        }

        @Override
        boolean isAlbum() {
            return false;
        }

        @Override
        public void playAllSong(Context context) {
            MusicUtils.playFavorites(context);
        }
    }

    private class LastAddedDataSource extends ProfileDataSource {
        public LastAddedDataSource(Bundle arguments) {
            super(arguments);
        }

        @Override
        String artistName() {
            return null;
        }

        @Override
        String playMenuTitle() {
            return getString(R.string.menu_play_all);
        }

        @Override
        int sortMenuId() {
            return 0;
        }

        @Override
        void photoClickHandler() {
            ProfileType profileType;
            profileType = ProfileType.OTHER;
            PhotoSelectionDialog.newInstance(profileName(),
                    profileType).show(getSupportFragmentManager(), "PhotoSelectionDialog");
        }

        @Override
        void setupPages() {
            // Add the carousel images
            mTabCarousel.setPlaylistOrGenreProfileHeader(ProfileActivity.this, mProfileName);

            // Last added fragment
            mPagerAdapter.add(LastAddedFragment.class, null);

            // Action bar title = Last added
            mResources.setTitle(mProfileName);
        }

        @Override
        boolean isArtist() {
            return false;
        }

        @Override
        boolean isAlbum() {
            return false;
        }

        @Override
        public void playAllSong(Context context) {
            MusicUtils.playLastAdded(context);
        }
    }

    private class GenreDataSource extends ProfileDataSource {
        public GenreDataSource(Bundle arguments) {
            super(arguments);
        }

        @Override
        String artistName() {
            return null;
        }

        @Override
        String playMenuTitle() {
            return getString(R.string.menu_shuffle);
        }

        @Override
        int sortMenuId() {
            return 0;
        }

        @Override
        void photoClickHandler() {
            ProfileType profileType;
            profileType = ProfileType.OTHER;
            PhotoSelectionDialog.newInstance(profileName(),
                    profileType).show(getSupportFragmentManager(), "PhotoSelectionDialog");
        }

        @Override
        void setupPages() {
            // Add the carousel images
            mTabCarousel.setPlaylistOrGenreProfileHeader(ProfileActivity.this, mProfileName);

            // Genre profile fragments
            mPagerAdapter.add(GenreSongFragment.class, mArguments);

            // Action bar title = playlist name
            mResources.setTitle(mProfileName);
        }

        @Override
        boolean isArtist() {
            return false;
        }

        @Override
        boolean isAlbum() {
            return false;
        }

        @Override
        public void playAllSong(Context context) {
            final String stringId = itemID();
            Song[] list = MusicUtils.getSongListForGenre(context, stringId);
            if (list != null && list.length > 0) {
                MusicUtils.playAll(context, list, 0, true);
            }
        }
    }

    private class OnlineChartDataSource extends ProfileDataSource {
        public OnlineChartDataSource(Bundle arguments) {
            super(arguments);
        }

        @Override
        String artistName() {
            return null;
        }

        @Override
        String playMenuTitle() {
            return null;
        }

        @Override
        int sortMenuId() {
            return 0;
        }

        @Override
        void photoClickHandler() {
        }

        @Override
        void setupPages() {
            // Add the carousel images
            mTabCarousel.setChartProfileHeader(ProfileActivity.this, mArguments);

            // Chart profile fragments
            mPagerAdapter.add(XSongListFragment.class, mArguments);
            mPagerAdapter.add(XChoiceEntryFragment.class, mArguments);

            // Action bar title = chart name
            mResources.setTitle(mProfileName);
        }

        @Override
        boolean isArtist() {
            return false;
        }

        @Override
        boolean isAlbum() {
            return false;
        }

        @Override
        public void playAllSong(Context context) {

        }
    }

    private class OnlineAlbumDataSource extends ProfileDataSource {
        private String mArtistName;

        public OnlineAlbumDataSource(Bundle arguments) {
            super(arguments);
            mArtistName = mArguments.getString(Config.ARTIST_NAME);
        }

        @Override
        String artistName() {
            return mArtistName;
        }

        @Override
        String playMenuTitle() {
            return null;
        }

        @Override
        int sortMenuId() {
            return 0;
        }

        @Override
        void photoClickHandler() {

        }

        @Override
        void setupPages() {
            // Add the carousel images
            mTabCarousel.setOnlineAlbumProfileHeader(ProfileActivity.this, mArguments);

            mPagerAdapter.add(XSongListFragment.class, mArguments);

            mResources.setTitle(mProfileName);
        }

        @Override
        boolean isArtist() {
            return false;
        }

        @Override
        boolean isAlbum() {
            return true;
        }

        @Override
        public void playAllSong(Context context) {

        }
    }

    private class OnlineArtistDataSource extends ProfileDataSource {
        public OnlineArtistDataSource(Bundle arguments) {
            super(arguments);
        }

        @Override
        String artistName() {
            return mProfileName;
        }

        @Override
        String playMenuTitle() {
            return null;
        }

        @Override
        int sortMenuId() {
            return 0;
        }

        @Override
        void photoClickHandler() {

        }

        @Override
        void setupPages() {
            // Add the carousel images
            mTabCarousel.setOnlineArtistProfileHeader(ProfileActivity.this, mArguments);

            mPagerAdapter.add(XMusicEntryFragment.class, mArguments);

            //Copy the arguments and clear the URL;
            Bundle bundle = new Bundle(mArguments);
            bundle.putString(Config.ENTRY_URL, "");

            mPagerAdapter.add(XSongListFragment.class, bundle);

            mResources.setTitle(mProfileName);
        }

        @Override
        boolean isArtist() {
            return true;
        }

        @Override
        boolean isAlbum() {
            return false;
        }

        @Override
        public void playAllSong(Context context) {

        }
    }
}
