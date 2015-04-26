package the.topmusic.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.inputmethod.InputMethodManager;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.widget.SearchView;

import the.topmusic.R;
import the.topmusic.ui.fragments.online.SearchOnlineFragment;
import the.topmusic.utils.MusicUtils;

/**
 * Created by lucd on 11/6/14.
 */
public class SearchOnlineActivity extends BaseActivity {

    private SearchOnlineFragment searchFragment = null;
    private String mFilterString;

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set up the action bar
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        showFragment(getIntent());
    }

    private void showFragment(Intent intent) {
        // Get the keyword
        final String keyword = intent.getStringExtra("keyword");

        mFilterString = !TextUtils.isEmpty(keyword) ? keyword : null;
        // Get the default search type
        final String queryType = intent.getStringExtra("type");

        // Action bar subtitle
        mResources.setSubtitle("\"" + mFilterString + "\"");

        if (searchFragment == null) {
            searchFragment = new SearchOnlineFragment();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.activity_base_content, searchFragment)
                    .commit();
        }

        searchFragment.setQuery(keyword, queryType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onNewIntent(final Intent intent) {
        super.onNewIntent(intent);
        showFragment(intent);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        boolean createdOK = super.onCreateOptionsMenu(menu);
        if (createdOK) {
            // Filter the list the user is looking it via SearchView
            final SearchView searchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(final String query) {
                    if (TextUtils.isEmpty(query)) {
                        return false;
                    }
                    // When the search is "committed" by the user, then hide the keyboard so
                    // the user can
                    // more easily browse the list of results.
                    if (searchView != null) {
                        final InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        if (imm != null) {
                            imm.hideSoftInputFromWindow(searchView.getWindowToken(), 0);
                        }
                        searchView.clearFocus();
                    }
                    menu.findItem(R.id.menu_search).collapseActionView();

                    // Action bar subtitle
                    mResources.setSubtitle("\"" + query + "\"");

                    searchFragment.loadNewQuery(query);

                    return true;
                }

                @Override
                public boolean onQueryTextChange(final String newText) {
                    return false;
                }
            });
        }
        return createdOK;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onResume() {
        super.onResume();
        MusicUtils.killForegroundService(this);
    }


    @Override
    public int setContentView() {
        return R.layout.activity_base;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

}

