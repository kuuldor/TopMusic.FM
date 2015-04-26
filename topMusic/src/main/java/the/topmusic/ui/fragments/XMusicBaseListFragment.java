package the.topmusic.ui.fragments;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockFragment;

import java.util.List;

import the.topmusic.Config;
import the.topmusic.MusicStateListener;
import the.topmusic.R;
import the.topmusic.loaders.XMusicEntryLoader;
import the.topmusic.model.XMusicEntry;
import the.topmusic.recycler.RecycleHolder;
import the.topmusic.ui.activities.BaseActivity;
import the.topmusic.ui.activities.SearchOnlineActivity;

/**
 * Created by lucd on 9/30/14.
 */
public abstract class XMusicBaseListFragment extends SherlockFragment implements
        LoaderManager.LoaderCallbacks<List<XMusicEntry>>,
        AdapterView.OnItemClickListener,
        MusicStateListener,
        BaseActivity.SearchProvider {
    /**
     * LoaderCallbacks identifier
     */
    protected static final int LOADER = 0;
    /**
     * Fragment UI
     */
    protected ViewGroup mRootView;
    protected String mRootURL = null;
    protected String mRootTitle = null;
    protected String mRootID = null;
    protected String mRootType = null;
    protected String mParentType = null;
    protected Bundle mRootBundle = null;

    /**
     * Represents an entry
     */
    protected XMusicEntry mEntry;
    /**
     * The list view
     */
    private ListView mListView;
    /**
     * True if the list should execute {@code #restartLoader()}.
     */
    private boolean mShouldRefresh = false;
    private ArrayAdapter<XMusicEntry> mAdapter;
    private boolean mLazyLoad = false;
    private boolean mLoadStarted = false;
    private ProgressDialog mAcitivityIndicator;

    public ArrayAdapter<XMusicEntry> getAdapter() {
        return mAdapter;
    }

    public void setRoot(String rootID, String rootTitle, String rootURL) {
        this.mRootURL = rootURL;
        this.mRootID = rootID;
        this.mRootTitle = rootTitle;

        mEntry = new XMusicEntry(mRootID, mRootTitle, mRootType);
        mEntry.mUrl = mRootURL;

        this.refresh();
    }

    public void setRoot(XMusicEntry entry) {
        this.mRootURL = mEntry.mUrl;
        this.mRootID = mEntry.mMusicEntryId;
        this.mRootTitle = mEntry.mMusicEntryName;
        this.mRootType = mEntry.mMusicEntryType;

        mEntry = entry;

        this.refresh();
    }

    public void setRoot(String rootID, String rootTitle, String rootURL, String rootType) {
        this.mRootURL = rootURL;
        this.mRootID = rootID;
        this.mRootTitle = rootTitle;
        this.mRootType = rootType;

        mEntry = new XMusicEntry(mRootID, mRootTitle, mRootType);
        mEntry.mUrl = mRootURL;

        this.refresh();
    }

    abstract public ArrayAdapter<XMusicEntry> createAdapter(final Context context, final int layoutId);

    protected Bundle createRootArgs(Context context) {
        return null;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (args == null) {
            args = createRootArgs(getSherlockActivity());
        }

        setRoot(args);

        int layout = getListItemLayout();

        mAdapter = createAdapter(getSherlockActivity(), layout);

    }

    public void setRoot(Bundle args) {
        mRootURL = args.getString(Config.ENTRY_URL);
        mRootID = args.getString(Config.ID);
        mRootTitle = args.getString(Config.NAME);
        mRootType = args.getString(Config.ENTRY_TYPE);

        mLazyLoad = args.getBoolean(Config.LAZY_LOAD);
        mParentType = args.getString(Config.MIME_TYPE);

        mEntry = new XMusicEntry(mRootID, mRootTitle, mRootType);
        mEntry.mUrl = mRootURL;

        mRootBundle = new Bundle(args);
    }

    protected int getListItemLayout() {
        return R.layout.list_item_simple;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        // The View for the fragment's UI
        mRootView = (ViewGroup) inflater.inflate(R.layout.list_base, null);
        initListView();
        return mRootView;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Enable the options menu
        setHasOptionsMenu(false);


        if (!mLazyLoad) {
            startLoader();
        }
    }

    public void startLoader() {
        if (getActivity() == null) {
            return;
        }
        // Start the loader if hasn't
        if (!mLoadStarted) {
            getLoaderManager().initLoader(LOADER, mRootBundle, this);
            mLoadStarted = true;
            showActivityIndicator();
        } else if (mShouldRefresh) {
            refresh();
        }
    }

    protected void showActivityIndicator() {
        if (mAcitivityIndicator == null) {
            Activity activity = getActivity();
            mAcitivityIndicator = new ProgressDialog(activity, R.style.ProgressDialogTheme);
            mAcitivityIndicator.setCancelable(false);
            mAcitivityIndicator.setProgressStyle(android.R.style.Widget_ProgressBar_Large);
            mAcitivityIndicator.show();
        }
    }

    protected void hideActivityIndicator() {
        if (mAcitivityIndicator != null) {
            mAcitivityIndicator.dismiss();
            mAcitivityIndicator = null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putAll(mRootBundle != null ? mRootBundle : (getArguments() != null ? getArguments() : new Bundle()));
    }

    /**
     * Sets up the list view
     */
    private void initListView() {
        // Initialize the grid
        mListView = (ListView) mRootView.findViewById(R.id.list_base);
        // Set the data behind the list
        mListView.setAdapter(mAdapter);
        // Release any references to the recycled Views
        mListView.setRecyclerListener(new RecycleHolder());
        // Listen for ContextMenus to be created
        mListView.setOnCreateContextMenuListener(this);
        // Show the albums and songs from the selected artist
        mListView.setOnItemClickListener(this);

        // Set up the helpers
        initListViewExtra(mListView);
    }

    /**
     * Sets up various helpers for both the list and grid
     *
     * @param list The list or grid
     */
    protected void initListViewExtra(final AbsListView list) {
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void onItemClick(final AdapterView<?> parent, final View view, final int position,
                            final long id) {
        mEntry = mAdapter.getItem(position);
    }

    @Override
    public Loader<List<XMusicEntry>> onCreateLoader(int id, Bundle args) {
        return new XMusicEntryLoader(getSherlockActivity(), mEntry);
    }

    public ListView getListView() {
        return mListView;
    }

    @Override
    public void onLoadFinished(Loader<List<XMusicEntry>> loader, List<XMusicEntry> data) {
        hideActivityIndicator();

        loadDataIntoAdapter(data);
    }


    protected void loadDataIntoAdapter(List<XMusicEntry> data) {
        mAdapter.clear();
    }

    @Override
    public void onLoaderReset(Loader<List<XMusicEntry>> loader) {
        // Clear the data in the adapter
        mAdapter.clear();
    }

    @Override
    public void restartLoader() {
        // Update the list when the user deletes any items
        if (mShouldRefresh) {
            getLoaderManager().restartLoader(LOADER, null, this);
        }
        mShouldRefresh = false;
    }

    @Override
    public void onMetaChanged() {

    }

    /**
     * Restarts the loader.
     */
    public void refresh() {
        showActivityIndicator();
        // Scroll to the stop of the list before restarting the loader.
        // Otherwise, if the user has scrolled enough to move the header, it
        // becomes misplaced and needs to be reset.
//        mListView.setSelection(0);
        mAdapter.clear();
        // Wait a moment for the preference to change.
        SystemClock.sleep(10);
        mAdapter.notifyDataSetChanged();
        getLoaderManager().restartLoader(LOADER, mRootBundle, this);
    }

    public void setShouldRefresh(boolean yesOrNo) {
        mShouldRefresh = yesOrNo;
    }

    protected String getQueryType() {
        return "song";
    }

    public void searchText(Activity activity, String query) {
        final Intent intent = new Intent(activity, SearchOnlineActivity.class);
        intent.putExtra("keyword", query);

        String queryType = getQueryType();
        intent.putExtra("type", queryType);

        activity.startActivity(intent);
    }
}
