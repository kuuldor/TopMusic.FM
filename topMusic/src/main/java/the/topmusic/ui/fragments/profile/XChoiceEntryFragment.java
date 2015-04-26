package the.topmusic.ui.fragments.profile;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.view.ContextMenu;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import the.topmusic.R;
import the.topmusic.adapters.ProfileMusicEntryAdapter;
import the.topmusic.model.XMusicEntry;
import the.topmusic.ui.activities.ProfileActivity;
import the.topmusic.ui.fragments.FragmentIndex;
import the.topmusic.ui.fragments.XMusicBaseListFragment;
import the.topmusic.widgets.ProfileTabCarousel;
import the.topmusic.widgets.VerticalScrollListener;

import static the.topmusic.adapters.ProfileMusicEntryAdapter.ParentType.Album;
import static the.topmusic.adapters.ProfileMusicEntryAdapter.ParentType.Artist;

/**
 * Created by lucd on 9/15/14.
 */
public class XChoiceEntryFragment extends XMusicBaseListFragment {

    /**
     * Used to keep context menu items from bleeding into other fragments
     */
    private static final int GROUP_ID = FragmentIndex.XCHOICEENTRY_FRAGMENT_IDX;


    private static final int TAB_INDEX = 1;


    /**
     * Profile header
     */
    private ProfileTabCarousel mProfileTabCarousel;

    /**
     * Empty constructor as per the {@link android.support.v4.app.Fragment} documentation
     */
    public XChoiceEntryFragment() {
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
            adapter.setParentType(ProfileMusicEntryAdapter.ParentType.List);
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
    public void onCreateContextMenu(final ContextMenu menu, final View v,
                                    final ContextMenu.ContextMenuInfo menuInfo) {
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

        ProfileActivity activity = (ProfileActivity) getActivity();
        activity.getChartSongsFragment().setRoot(mEntry);
        mProfileTabCarousel.resetTabAtIndex(0).setLabel(mEntry.mMusicEntryName);
        mProfileTabCarousel.setCurrentTab(0);
    }

    @Override
    public void startLoader() {
        //Do not show progress indicator
    }

    @Override
    public Loader<List<XMusicEntry>> onCreateLoader(int id, Bundle args) {
        return null;
    }

    protected void loadChoicesIntoAdapter(XMusicEntry entry) {

        // Start fresh
        getAdapter().unload();

        JSONArray choices = null;

        if (entry.mMusicEntryType.equalsIgnoreCase("choice")) {

            try {
                choices = entry.mJSONItem.getJSONArray("choices");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        if (choices != null) for (int i = 0; i < choices.length(); i++) {
            final JSONObject choice;
            try {
                choice = choices.getJSONObject(i);
                getAdapter().add(new XMusicEntry(choice));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}