package the.topmusic.ui.fragments.online;

import android.app.Activity;
import android.os.Bundle;
import android.util.Base64;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import java.util.Arrays;

import the.topmusic.Config;
import the.topmusic.R;
import the.topmusic.ui.activities.OnBackPressedListener;
import the.topmusic.ui.fragments.FragmentIndex;
import the.topmusic.utils.MusicUtils;

/**
 * Created by lucd on 11/6/14.
 */
public class SearchOnlineFragment extends OnlineFragment implements
        OnBackPressedListener {

    public static final String STATE_KEYWORD = "keyword";
    public static final String STATE_TYPE = "type";
    /**
     * Used to keep context menu items from bleeding into other fragments
     */
    private static final int GROUP_ID = FragmentIndex.XSEARCH_FRAGMENT_IDX;
    private static final String[] queryTypes = {"song", "artist", "album"};
    private String mKeyword;
    private String mQueryType;

    public void setQuery(String keyword, String queryType) {
        mKeyword = keyword;
        mQueryType = queryType;
    }

    public void setQuery(String keyword) {
        mKeyword = keyword;
    }

    @Override
    public void onPrepareOptionsMenu(final Menu menu) {
        super.onPrepareOptionsMenu(menu);
        mResources.setFavoriteIcon(menu);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        // Favorite action
        inflater.inflate(R.menu.favorite, menu);
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
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean isSongsPage() {
        return mViewPager.getCurrentItem() == 0;
    }

    private int getPageForQueryType(final String queryType) {
        int page = 0;
        if (queryType != null) {
            page = Arrays.asList(queryTypes).indexOf(queryType);
        }
        return page;
    }

    @Override
    protected int getStartPage() {
        return getPageForQueryType(mQueryType);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(STATE_KEYWORD, mKeyword);
        outState.putString(STATE_TYPE, mQueryType);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void setPages(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mKeyword = savedInstanceState.getString(STATE_KEYWORD);
            mQueryType = savedInstanceState.getString(STATE_TYPE);
        }
        Activity context = getActivity();
        Bundle songArgs = createParamsForType(context, "song");
        Bundle artistArgs = createParamsForType(context, "artist");
        Bundle albumArgs = createParamsForType(context, "album");

        mPagerAdapter.add(SongsFragment.class, songArgs);
        mPagerAdapter.add(ArtistFragment.class, artistArgs);
        mPagerAdapter.add(AlbumFragment.class, albumArgs);

        mPagerAdapter.setPageTitles(getResources().getStringArray(R.array.online_search_titles));
    }

    private Bundle createParamsForType(Activity context, String qType) {
        Bundle bundle = XMusicListFragment.createParams(context,
                "Search_" + qType + "_" + mKeyword,
                "Search " + qType,
                String.format(Config.SEARCH_URL_PATH, qType, Base64.encodeToString(mKeyword.getBytes(), Base64.NO_WRAP)));
        bundle.putBoolean(Config.LAZY_LOAD, !mQueryType.equalsIgnoreCase(qType));
        return bundle;
    }

    @Override
    protected boolean listFragmentOnPosition(int position) {
        return true;
    }

    @Override
    public void onPageSelected(int position) {
        mQueryType = queryTypes[position];
        final XMusicListFragment fragment = (XMusicListFragment) mPagerAdapter.getFragment(position);
        fragment.startLoader();
    }

    public void loadNewQuery(String newQuery) {
        if (mKeyword.equals(newQuery)) {
            return;
        }

        mKeyword = newQuery;
        int currentPage = getPageForQueryType(mQueryType);

        Activity context = getActivity();
        Bundle args[] = new Bundle[]{
                createParamsForType(context, "song"),
                createParamsForType(context, "artist"),
                createParamsForType(context, "album")
        };

        for (int i = 0; i < 3; i++) {
            final XMusicListFragment fragment = (XMusicListFragment) mPagerAdapter.getFragment(i);
            fragment.setRoot(args[i]);
            fragment.setShouldRefresh(true);
            if (currentPage == i) {
                fragment.startLoader();
            }
        }
    }

    @Override
    protected boolean hasMenu() {
        return true;

    }
}
