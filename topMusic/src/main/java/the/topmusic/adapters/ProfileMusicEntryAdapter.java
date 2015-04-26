package the.topmusic.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.actionbarsherlock.app.SherlockFragmentActivity;

import the.topmusic.R;
import the.topmusic.cache.ImageFetcher;
import the.topmusic.model.XMusicEntry;
import the.topmusic.ui.MusicHolder;
import the.topmusic.utils.TopMusicUtils;

/**
 * Created by lucd on 9/14/14.
 */
public class ProfileMusicEntryAdapter extends ArrayAdapter<XMusicEntry> {

    /**
     * The header view
     */
    private static final int ITEM_VIEW_TYPE_HEADER = 0;

    /**
     * * The data in the list.
     */
    private static final int ITEM_VIEW_TYPE_MUSIC = 1;

    /**
     * Number of views (ImageView, TextView, header)
     */
    private static final int VIEW_TYPE_COUNT = 3;

    /**
     * LayoutInflater
     */
    private final LayoutInflater mInflater;

    /**
     * Image cache and image fetcher
     */
    private final ImageFetcher mImageFetcher;

    /**
     * The resource Id of the layout to inflate
     */
    private final int mLayoutId;

    private final boolean mLoadExtra;
    private ParentType parentType;

    public ProfileMusicEntryAdapter(final Context context, final int layoutId) {
        super(context, 0);
        // Used to create the custom layout
        mInflater = LayoutInflater.from(context);

        // Get the layout Id
        mLayoutId = layoutId;

        mLoadExtra = (layoutId == R.layout.list_item_detailed);
        // Initialize the cache & image fetcher
        mImageFetcher = TopMusicUtils.getImageFetcher((SherlockFragmentActivity) context);
    }

    public void setParentType(ParentType parentType) {
        this.parentType = parentType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public View getView(final int position, View convertView, final ViewGroup parent) {

        // Return a faux header at position 0
        if (position == 0) {
            /**
             * Fake header
             */

            return mInflater.inflate(R.layout.faux_carousel, null);
        }

        // Recycle MusicHolder's items
        MusicHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(mLayoutId, parent, false);
            holder = new MusicHolder(convertView);
            if (!mLoadExtra) {
                holder.mLineThree.get().setVisibility(View.GONE);
            }
            convertView.setTag(holder);
        } else {
            holder = (MusicHolder) convertView.getTag();
        }

        // Retrieve the album
        final XMusicEntry entry = getItem(position - 1);

        // Set each track name (line one)
        holder.mLineOne.get().setText(entry.mMusicEntryName);

        final String type = entry.mMusicEntryType;
        String line2 = null;
        switch (parentType) {
            case Artist:
                if (type.equalsIgnoreCase("song")) {
                    line2 = entry.albumName();
                } else if (type.equalsIgnoreCase("album")) {
                    line2 = entry.mJSONItem.optString("publishYear", null);
                }
                break;
            case Album:
                line2 = entry.artistName();
                break;
            default:
                break;
        }

        if (line2 == null || line2.trim().equals("")) {
            line2 = entry.subtitle();
        }

        if (line2 != null && !line2.trim().equals("")) {
            holder.mLineTwo.get().setText(line2);
        } else {
            holder.mLineTwo.get().setVisibility(View.GONE);
        }

        if (mLoadExtra) {
            String lineThree = null;
            // Set the duration or album name (line three)

            if (type.equalsIgnoreCase("song")) {
                lineThree = entry.mJSONItem.optString("album", null);
            } else if (type.equalsIgnoreCase("album")) {
                lineThree = entry.mJSONItem.optString("publishYear", null);
            }

            if (lineThree != null) {
                holder.mLineThree.get().setText(lineThree);
            } else {
                holder.mLineThree.get().setVisibility(View.GONE);
            }
        }
        // Asynchronously load the image into the adapter
        mImageFetcher.loadImageFromURL(entry.thumbnailURL(), holder.mImage.get());

        return convertView;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasStableIds() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getCount() {
        final int size = super.getCount();
        return size == 0 ? 0 : size + 1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getItemId(final int position) {
        return position - 1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getViewTypeCount() {
        return VIEW_TYPE_COUNT;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getItemViewType(final int position) {
        if (position == 0) {
            return ITEM_VIEW_TYPE_HEADER;
        }
        return ITEM_VIEW_TYPE_MUSIC;
    }

    /**
     * Method that unloads and clears the items in the adapter
     */
    public void unload() {
        clear();
    }

    public enum ParentType {
        List, Artist, Album
    }

}
