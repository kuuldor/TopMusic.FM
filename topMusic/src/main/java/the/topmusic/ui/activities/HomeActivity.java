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

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;

import com.actionbarsherlock.app.SherlockFragment;

import the.topmusic.R;
import the.topmusic.ui.fragments.online.MusicBrowserOnlineFragment;
import the.topmusic.ui.fragments.phone.MusicBrowserPhoneFragment;
import the.topmusic.utils.PreferenceUtils;


/**
 * This class is used to display the {@link ViewPager} used to swipe between the
 * main {@link Fragment}s used to browse the user's music.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class HomeActivity extends BaseActivity {

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load the music browser fragment
        if (savedInstanceState == null) {
            String homeBrowser = PreferenceUtils.getInstace(this).homeBrowserType(this);
            if (homeBrowser.equalsIgnoreCase("online")) {
                showOnlineBrowser();
            } else {
                showPhoneBrowser();
            }
        }

    }

    public void showOnlineBrowser() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.activity_base_content, new MusicBrowserOnlineFragment())
                .commit();
    }

    public void showPhoneBrowser() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.activity_base_content, new MusicBrowserPhoneFragment()).commit();
    }

    @Override
    protected void searchText(String query) {
        SherlockFragment fragment = (SherlockFragment) getSupportFragmentManager().findFragmentById(R.id.activity_base_content);
        if (fragment instanceof SearchProvider) {
            SearchProvider searchProvider = (SearchProvider) fragment;
            searchProvider.searchText(this, query);
        } else {
            super.searchText(query);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int setContentView() {
        return R.layout.activity_base;
    }

}
