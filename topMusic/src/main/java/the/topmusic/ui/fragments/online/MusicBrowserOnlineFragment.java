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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import the.topmusic.R;
import the.topmusic.ui.activities.BaseActivity;
import the.topmusic.ui.activities.SearchOnlineActivity;
import the.topmusic.utils.MusicUtils;
import the.topmusic.utils.NavUtils;
import the.topmusic.utils.PreferenceUtils;

import static the.topmusic.adapters.PagerAdapter.OnlineMusicFragments;

/**
 * This class is used to hold the {@link android.support.v4.view.ViewPager} used for swiping between the
 * playlists, recent, artists, albums, songs, and genre {@link com.actionbarsherlock.app.SherlockFragment}
 * s for phones.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 * @NOTE: The reason the sort orders are taken care of in this fragment rather
 * than the individual fragments is to keep from showing all of the menu
 * items on tablet interfaces. That being said, I have a tablet interface
 * worked out, but I'm going to keep it in the Play Store version of
 * TopMusic for a couple of weeks or so before merging it with CM.
 */
public class MusicBrowserOnlineFragment extends OnlineFragment implements
        BaseActivity.SearchProvider {

    /**
     * Empty constructor as per the {@link android.support.v4.app.Fragment} documentation
     */
    public MusicBrowserOnlineFragment() {
    }

    @Override
    protected int getStartPage() {
        return mPreferences.getStartPage(0);
    }

    @Override
    protected void setPages(Bundle savedInstanceState) {
        final OnlineMusicFragments[] mFragments = OnlineMusicFragments.values();
        for (final OnlineMusicFragments mFragment : mFragments) {
            mPagerAdapter.add(mFragment.getFragmentClass(), null);
        }
        mPagerAdapter.setPageTitles(getResources().getStringArray(R.array.online_page_titles));
    }

    @Override
    protected boolean listFragmentOnPosition(int position) {
        return (position > 0 && position < 4);
    }


    @Override
    protected boolean hasMenu() {
        return true;
    }

    @Override
    public void onPrepareOptionsMenu(final Menu menu) {
        super.onPrepareOptionsMenu(menu);
        mResources.setFavoriteIcon(menu);
    }

    /**
     * {@inheritDoc}
     */
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        // Favorite action
        inflater.inflate(R.menu.favorite, menu);
        // flip view
        inflater.inflate(R.menu.flipover, menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_favorite:
                // Toggle the current track as a favorite and update the menu
                // item
                MusicUtils.toggleFavorite();
                getSherlockActivity().invalidateOptionsMenu();
                return true;

            case R.id.menu_flipphone:
                SherlockFragmentActivity activity = getSherlockActivity();
                PreferenceUtils.getInstace(activity).setHomePhone();
                NavUtils.goHome(activity);
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean isChartPage() {
        return mViewPager.getCurrentItem() == 0;
    }

    private ChartFragment getChartFragment() {
        return (ChartFragment) mPagerAdapter.getFragment(0);
    }


    private boolean isArtistPage() {
        return mViewPager.getCurrentItem() == 1;
    }

    private the.topmusic.ui.fragments.online.ArtistFragment getArtistFragment() {
        return (the.topmusic.ui.fragments.online.ArtistFragment) mPagerAdapter.getFragment(1);
    }

    private boolean isAlbumPage() {
        return mViewPager.getCurrentItem() == 2;
    }

    private the.topmusic.ui.fragments.online.AlbumFragment getAlbumFragment() {
        return (the.topmusic.ui.fragments.online.AlbumFragment) mPagerAdapter.getFragment(2);
    }

    private boolean isGenrePage() {
        return mViewPager.getCurrentItem() == 3;
    }

    private GenreFragment getGenreFragment() {
        return (GenreFragment) mPagerAdapter.getFragment(3);
    }

    private boolean isPlaylistPage() {
        return mViewPager.getCurrentItem() == 4;
    }

    private boolean isDownloadPage() {
        return mViewPager.getCurrentItem() == 5;
    }

    public void searchText(Activity activity, String query) {
        final Intent intent = new Intent(activity, SearchOnlineActivity.class);
        intent.putExtra("keyword", query);
        String queryType;
        if (isAlbumPage()) {
            queryType = "album";
        } else if (isArtistPage()) {
            queryType = "artist";
        } else {
            queryType = "song";
        }
        intent.putExtra("type", queryType);

        activity.startActivity(intent);
    }
}
