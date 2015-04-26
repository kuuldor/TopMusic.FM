package the.topmusic.ui.fragments.online;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;
import java.util.Stack;

import the.topmusic.Config;
import the.topmusic.R;
import the.topmusic.adapters.XMusicEntryAdapter;
import the.topmusic.model.XMusicEntry;
import the.topmusic.ui.activities.OnBackPressedListener;
import the.topmusic.ui.fragments.XMusicBaseListFragment;
import the.topmusic.utils.PreferenceUtils;
import the.topmusic.utils.TopMusicUtils;

/**
 * Created by lucd on 10/4/14.
 */
public class XMusicListFragment extends XMusicBaseListFragment implements AbsListView.OnScrollListener, OnBackPressedListener {

    protected final Stack<XMusicEntry> navigationStack = new Stack<XMusicEntry>();
    private boolean loadingMore;

    public static Bundle createParams(Context context, String id, String name, String url_path) {
        String baseURL = PreferenceUtils.getInstace(context).serverLocaion(context);
        Bundle bundle = new Bundle();
        bundle.putString(Config.ID, id);
        bundle.putString(Config.MIME_TYPE, "MusicEntry");
        bundle.putString(Config.NAME, name);

        String entityURL = baseURL + url_path + "&locale=" + TopMusicUtils.appLocale();
        bundle.putString(Config.ENTRY_URL, entityURL);
        bundle.putString(Config.ENTRY_TYPE, "list");

        return bundle;
    }

    public static Bundle createParams(String id, String name, String url) {
        Bundle bundle = new Bundle();
        bundle.putString(Config.ID, id);
        bundle.putString(Config.MIME_TYPE, "MusicEntry");
        bundle.putString(Config.NAME, name);

        String entityURL = url + "&locale=" + TopMusicUtils.appLocale();
        bundle.putString(Config.ENTRY_URL, entityURL);
        bundle.putString(Config.ENTRY_TYPE, "list");

        return bundle;
    }

    public XMusicEntryAdapter getAdapter() {
        return (XMusicEntryAdapter) super.getAdapter();
    }

    @Override
    public ArrayAdapter<XMusicEntry> createAdapter(Context context, int layoutId) {
        return new XMusicEntryAdapter(context, layoutId);
    }

    /**
     * Sets up various helpers for both the list and grid
     *
     * @param list The list or grid
     */
    @Override
    protected void initListViewExtra(final AbsListView list) {
        // To help make scrolling smooth
        list.setOnScrollListener(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onPause() {
        super.onPause();
        getAdapter().flush();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onItemClick(final AdapterView<?> parent, final View view, final int position,
                            final long id) {
        navigationStack.push(mEntry);
        mEntry = getAdapter().getItem(position);
        onXMEntryClick();
    }

    protected void onXMEntryClick() {
        setRoot(mEntry);
    }

    public void setRoot(XMusicEntry entry) {
        if (!this.mEntry.mMusicEntryType.equalsIgnoreCase("next")) {
            super.setRoot(entry);
        } else {
            mEntry = entry;
            this.loadMore();
        }
    }

    protected void loadMore() {
        this.loadingMore = true;
        getAdapter().notifyDataSetChanged();
        getLoaderManager().restartLoader(LOADER, getArguments(), this);
        showActivityIndicator();
    }

    protected void onEmptyData() {
        // Set the empty text
        final TextView empty = (TextView) mRootView.findViewById(R.id.empty);
        empty.setText(getString(R.string.empty_music));
        getListView().setEmptyView(empty);

        if (getAdapter().getCount() == 0) {
            super.setShouldRefresh(true);
        }
    }

    @Override
    protected void loadDataIntoAdapter(List<XMusicEntry> data) {
        if (!loadingMore) {
            // Start fresh
            getAdapter().unload();
        } else {
            getAdapter().remove(mEntry);
            //remove the entry from stack
            mEntry = navigationStack.pop();
        }

        loadingMore = false;
        // Check for any errors
        if (data.isEmpty()) {
            onEmptyData();
        } else for (final XMusicEntry entry : data) {
            // Add the data to the adpater
            getAdapter().add(entry);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onLoaderReset(final Loader<List<XMusicEntry>> loader) {
        // Clear the data in the adapter
        getAdapter().unload();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onScrollStateChanged(final AbsListView view, final int scrollState) {
        // Pause disk cache access to ensure smoother scrolling
        if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_FLING
                || scrollState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
            getAdapter().setPauseDiskCache(true);
        } else {
            getAdapter().setPauseDiskCache(false);
            getAdapter().notifyDataSetChanged();
        }
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

    }

    @Override
    public boolean doBack() {
        boolean handled = false;
        if (!navigationStack.isEmpty()) {
            handled = true;
            mEntry = navigationStack.pop();
            setRoot(mEntry);
        }
        return handled;
    }
}
