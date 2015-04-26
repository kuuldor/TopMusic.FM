package the.topmusic.ui.fragments.profile;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import java.util.List;

import the.topmusic.Config;
import the.topmusic.R;
import the.topmusic.adapters.ProfileMusicEntryAdapter;
import the.topmusic.model.XMusicEntry;
import the.topmusic.ui.activities.ProfileActivity;
import the.topmusic.ui.fragments.FragmentIndex;
import the.topmusic.ui.fragments.XMusicBaseListFragment;
import the.topmusic.utils.NavUtils;
import the.topmusic.widgets.CarouselTab;
import the.topmusic.widgets.ProfileTabCarousel;
import the.topmusic.widgets.VerticalScrollListener;

import static the.topmusic.adapters.ProfileMusicEntryAdapter.ParentType.Album;
import static the.topmusic.adapters.ProfileMusicEntryAdapter.ParentType.Artist;
import static the.topmusic.adapters.ProfileMusicEntryAdapter.ParentType.List;

/**
 * Created by lucd on 9/14/14.
 */
public class XMusicEntryFragment extends XMusicBaseListFragment {

    /**
     * Used to keep context menu items from bleeding into other fragments
     */
    private static final int GROUP_ID = FragmentIndex.XMUSICENTRY_FRAGMENT_IDX;


    private static final int TAB_INDEX = 0;


    /**
     * Profile header
     */
    protected ProfileTabCarousel mProfileTabCarousel;

    /**
     * Empty constructor as per the {@link android.support.v4.app.Fragment} documentation
     */
    public XMusicEntryFragment() {
    }

    public ProfileMusicEntryAdapter getAdapter() {
        return (ProfileMusicEntryAdapter) super.getAdapter();
    }

    @Override
    public ArrayAdapter<XMusicEntry> createAdapter(Context context, int layoutId) {
        ProfileMusicEntryAdapter adapter = new ProfileMusicEntryAdapter(getSherlockActivity(), layoutId);

        if (mParentType.equalsIgnoreCase("album")) {
            adapter.setParentType(Album);
        } else if (mParentType.equalsIgnoreCase("artist")) {
            adapter.setParentType(Artist);
        } else {
            adapter.setParentType(List);
        }

        return adapter;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onAttach(final Activity activity) {
        super.onAttach(activity);
        mProfileTabCarousel = (ProfileTabCarousel) activity
                .findViewById(R.id.acivity_profile_base_tab_carousel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Enable the options menu
        setHasOptionsMenu(true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int getListItemLayout() {
        int layout;
        if (isSimpleLayout()) {
            layout = R.layout.list_item_simple;
        } else if (isDetailedLayout()) {
            layout = R.layout.list_item_normal;
        } else {
            layout = R.layout.list_item_normal;
        }
        return layout;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void initListViewExtra(final AbsListView list) {
        // To help make scrolling smooth
        list.setOnScrollListener(new VerticalScrollListener(null, mProfileTabCarousel, TAB_INDEX));
        // Remove the scrollbars and padding for the fast scroll
        list.setVerticalScrollBarEnabled(false);
        list.setFastScrollEnabled(false);
        list.setPadding(0, 0, 0, 0);
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

        mEntry = getAdapter().getItem(position - 1);

        if (mEntry.mExpandable) {
            NavUtils.openXMusicEntry(getSherlockActivity(), mEntry, mEntry.mMusicEntryType);
        }
    }


    @Override
    protected Bundle createRootArgs(Context context) {
        Bundle bundle = new Bundle();
        bundle.putString(Config.ID, "");
        bundle.putString(Config.MIME_TYPE, "MusicEntry");
        bundle.putString(Config.NAME, "");

        bundle.putString(Config.ENTRY_URL, "");
        bundle.putString(Config.ENTRY_TYPE, "list");
        return bundle;
    }

    @Override
    protected void loadDataIntoAdapter(List<XMusicEntry> data) {
        // Start fresh
        getAdapter().unload();
        ProfileActivity activity = (ProfileActivity) getActivity();

        // Add the data to the adpater
        for (final XMusicEntry entry : data) {
            if (entry.mMusicEntryType.equalsIgnoreCase("choice")) {
                if (mProfileTabCarousel != null) {
                    CarouselTab tab = mProfileTabCarousel.getTabAtIndex(0);
                    if (tab != null) {
                        tab.setLabel(entry.mMusicEntryName);
                    }
                    XChoiceEntryFragment choiceFragment = activity.getChartListFragment();
                    choiceFragment.loadChoicesIntoAdapter(entry);
                }
            } else if (entry.mMusicEntryType.equalsIgnoreCase("list")) {
                XMusicEntryFragment hotsongFragment = activity.getHotSongsFragment();
                hotsongFragment.setRoot(entry);
            } else {
                getAdapter().add(entry);
            }
        }
    }

    private boolean isSimpleLayout() {
        boolean simple = false;
        if (mRootType != null)
            simple = mRootType.equals("album");
        return simple;
    }


    private boolean isDetailedLayout() {
        boolean detailed = false;
        if (mRootType != null)
            detailed = mRootType.equals("list");
        return detailed;
    }
}
