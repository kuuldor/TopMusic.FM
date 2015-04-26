package the.topmusic.ui.fragments.online;

import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.actionbarsherlock.app.SherlockFragment;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.viewpagerindicator.TitlePageIndicator;

import the.topmusic.R;
import the.topmusic.adapters.PagerAdapter;
import the.topmusic.ui.activities.BaseActivity;
import the.topmusic.ui.activities.OnBackPressedListener;
import the.topmusic.utils.PreferenceUtils;
import the.topmusic.utils.ThemeUtils;

import static android.support.v4.view.ViewPager.OnPageChangeListener;
/**
 * Created by lucd on 11/6/14.
 */
public abstract class OnlineFragment extends SherlockFragment implements OnPageChangeListener, OnBackPressedListener {
    public AdView adView;
    /**
     * Pager
     */
    protected ViewPager mViewPager;
    /**
     * VP's adapter
     */
    protected PagerAdapter mPagerAdapter;
    /**
     * Theme resources
     */
    protected ThemeUtils mResources;
    protected PreferenceUtils mPreferences;

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        position = 0;
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        // The View for the fragment's UI
        final ViewGroup rootView = (ViewGroup) inflater.inflate(
                R.layout.fragment_music_browser_phone, container, false);

        // Initialize the adapter
        BaseActivity activity = (BaseActivity) getSherlockActivity();

        mPagerAdapter = new PagerAdapter(activity);

        setPages(savedInstanceState);

        // Initialize the ViewPager
        mViewPager = (ViewPager) rootView.findViewById(R.id.fragment_home_phone_pager);
        // Attch the adapter
        mViewPager.setAdapter(mPagerAdapter);

        // Offscreen pager loading limit
        mViewPager.setOffscreenPageLimit(mPagerAdapter.getCount() - 1);
        // Start on the last page the user was on
        mViewPager.setCurrentItem(getStartPage());

        // Initialze the TPI
        final TitlePageIndicator pageIndicator = (TitlePageIndicator) rootView
                .findViewById(R.id.fragment_home_phone_pager_titles);
        // Attach the ViewPager
        pageIndicator.setViewPager(mViewPager);
        pageIndicator.setOnPageChangeListener(this);

        activity.setOnBackPressedListener(this);

        adView = (AdView) rootView.findViewById(R.id.adview);
//        adView.setAdUnitId("ca-app-pub-8747871488975411/6050297886");
//        adView.setAdSize(AdSize.BANNER);

        AdRequest adRequest = new AdRequest.Builder()
//                .addTestDevice("C8F97588F23EA36083BF7993CD3B6714")
//                .addTestDevice("758110E21057192707091B929F1D6AC1")
                .build();
        adView.loadAd(adRequest);

        return rootView;
    }

    protected abstract int getStartPage();

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Get the preferences
        mPreferences = PreferenceUtils.getInstace(getSherlockActivity());
    }

    protected abstract void setPages(Bundle savedInstanceState);

    @Override
    public boolean doBack() {
        boolean handled = false;

        int position = mViewPager.getCurrentItem();

        if (listFragmentOnPosition(position)) {
            XMusicListFragment fragment = (XMusicListFragment) mPagerAdapter.getFragment(position);
            handled = fragment.doBack();
        }


        return handled;
    }

    protected abstract boolean listFragmentOnPosition(int position);

    /**
     * {@inheritDoc}
     */
    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Initialze the theme resources
        mResources = new ThemeUtils(getSherlockActivity());
        // Enable the options menu
        setHasOptionsMenu(hasMenu());
    }

    protected boolean hasMenu() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onPause() {
        adView.pause();
        super.onPause();
        // Save the last page the use was on
        mPreferences.setStartPage(mViewPager.getCurrentItem());
    }

    @Override
    public void onResume() {
        super.onResume();
        adView.resume();
    }

    @Override
    public void onDestroy() {
        adView.destroy();
        super.onDestroy();
    }


}
