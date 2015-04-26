package the.topmusic.ui.fragments.online;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;

import the.topmusic.Config;
import the.topmusic.ui.fragments.FragmentIndex;
import the.topmusic.utils.NavUtils;

/**
 * Created by lucd on 9/13/14.
 */
public class ChartFragment extends the.topmusic.ui.fragments.online.XMusicListFragment {

    /**
     * Used to keep context menu items from bleeding into other fragments
     */
    private static final int GROUP_ID = FragmentIndex.CHART_FRAGMENT_IDX;

    /**
     * Grid view column count. ONE - list, TWO - normal grid, FOUR - landscape
     */
    private static final int ONE = 1, TWO = 2, FOUR = 4;

    @Override
    protected Bundle createRootArgs(Context context) {
        return createParams(context, "TopMusicChartsRoot", "TopMusic Charts", Config.CHART_URL_PATH);
    }

    @Override
    public void onItemClick(final AdapterView<?> parent, final View view, final int position,
                            final long id) {
        mEntry = getAdapter().getItem(position);
        NavUtils.openXMusicEntry(getSherlockActivity(), mEntry, "Chart");
    }
}
