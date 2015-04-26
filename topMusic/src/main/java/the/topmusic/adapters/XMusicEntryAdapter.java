package the.topmusic.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import com.actionbarsherlock.app.SherlockFragmentActivity;

import the.topmusic.Config;
import the.topmusic.R;
import the.topmusic.cache.ImageFetcher;
import the.topmusic.model.Album;
import the.topmusic.model.XMusicEntry;
import the.topmusic.ui.MusicHolder;
import the.topmusic.utils.TopMusicUtils;

/**
 * Created by lucd on 9/14/14.
 */
public class XMusicEntryAdapter extends ArrayAdapter<XMusicEntry> {

    /**
     * Number of views (ImageView and TextView)
     */
    private static final int VIEW_TYPE_COUNT = 2;

    /**
     * The resource Id of the layout to inflate
     */
    private final int mLayoutId;

    /**
     * Image cache and image fetcher
     */
    private final ImageFetcher mImageFetcher;

    /**
     * Semi-transparent overlay
     */
    private final int mOverlay;

    /**
     * Determines if the grid or list should be the default style
     */
    private boolean mLoadExtraData = false;

    /**
     * Sets the album art on click listener to start playing them album when
     * touched.
     */
    private boolean mTouchPlay = false;


    public XMusicEntryAdapter(final Context context, final int layoutId) {
        super(context, 0);
        // Get the layout Id
        mLayoutId = layoutId;
        // Initialize the cache & image fetcher
        mImageFetcher = TopMusicUtils.getImageFetcher((SherlockFragmentActivity) context);
        // Cache the transparent overlay
        mOverlay = context.getResources().getColor(R.color.list_item_background);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public View getView(final int position, View convertView, final ViewGroup parent) {
        // Recycle ViewHolder's items
        MusicHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(mLayoutId, parent, false);
            holder = new MusicHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (MusicHolder)convertView.getTag();
        }

        holder.mLineOne.get().setVisibility(View.VISIBLE);
        holder.mLineTwo.get().setVisibility(View.VISIBLE);
        holder.mLineThree.get().setVisibility(View.GONE);

        ImageView imageView = holder.mImage.get();

        if (imageView != null) {
            imageView.setVisibility(View.VISIBLE);
        }

        final XMusicEntry entry = getItem(position);
        holder.mLineOne.get().setText(entry.mMusicEntryName);


        final String subtitle = entry.subtitle();
        if (subtitle != null && !subtitle.trim().equals("")) {
            holder.mLineTwo.get().setVisibility(View.VISIBLE);
            holder.mLineTwo.get().setText(subtitle);
        } else {
            holder.mLineTwo.get().setVisibility(View.GONE);
        }

        if (imageView != null) {
            String imgURL = entry.thumbnailURL();
            if (imgURL == null || imgURL.isEmpty()) {
                imgURL = entry.imageURL();
            }
            if (imgURL == null || imgURL.isEmpty()) {
                imageView.setVisibility(View.GONE);
            } else {
                imageView.setVisibility(View.VISIBLE);
            }
            // Asynchronously load the image into the adapter
            mImageFetcher.loadImageFromURL(imgURL, imageView);
        }

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
    public int getViewTypeCount() {
        return VIEW_TYPE_COUNT;
    }



    /**
     * Method that unloads and clears the items in the adapter
     */
    public void unload() {
        clear();
    }

    /**
     * @param pause True to temporarily pause the disk cache, false otherwise.
     */
    public void setPauseDiskCache(final boolean pause) {
        if (mImageFetcher != null) {
            mImageFetcher.setPauseDiskCache(pause);
        }
    }

    /**
     * @param album The key used to find the cached album to remove
     */
    public void removeFromCache(final Album album) {
        if (mImageFetcher != null) {
            mImageFetcher.removeFromCache(album.mAlbumName + Config.ALBUM_ART_SUFFIX);
        }
    }

    /**
     * Flushes the disk cache.
     */
    public void flush() {
        mImageFetcher.flush();
    }

}
