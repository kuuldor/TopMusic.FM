package the.topmusic.loaders;

import android.content.Context;
import android.text.Html;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import the.topmusic.HTTP.HTTPCache;
import the.topmusic.HTTP.HTTPServiceException;
import the.topmusic.model.XMusicEntry;
import the.topmusic.provider.MusicStore;
import the.topmusic.utils.DiskCacheUtils;
import the.topmusic.utils.Lists;
import the.topmusic.utils.URLUtils;

/**
 * Created by lucd on 9/13/14.
 */
public class XMusicEntryLoader  extends WrappedAsyncTaskLoader<List<XMusicEntry>> {

    /**
     * The result
     */
    private final ArrayList<XMusicEntry> mXMusicEntryList = Lists.newArrayList();

    private XMusicEntry mParent;

    private MusicStore musicStore;

    /**
     * Constructor of <code>XMusicEntryLoader</code>
     *
     * @param context The {@link android.content.Context} to use
     */
    public XMusicEntryLoader(final Context context, XMusicEntry parent) {
        super(context);

        musicStore = MusicStore.getInstance(context);

        mParent = parent;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<XMusicEntry> loadInBackground() {
        // Download from parent's URL
        String urlString = mParent.mUrl+"&method=json";
        final JSONArray jsonOutput;
        try {
            final String contentOfURL = URLUtils.getUrlAsString(urlString, 60 * 1000, true);
            final String jsonString = Html.fromHtml(contentOfURL).toString();
            jsonOutput = new JSONObject(jsonString).getJSONArray("content");

            for (int i = 0; i < jsonOutput.length(); i++) {
                JSONObject jsonObject = jsonOutput.getJSONObject(i);
                final XMusicEntry entry = new XMusicEntry(jsonObject);
                // Add everything up
                mXMusicEntryList.add(entry);
            }
            musicStore.addMusicEntries(mXMusicEntryList);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            removeURLCache(urlString);
            e.printStackTrace();
        } catch (HTTPServiceException e) {
            removeURLCache(urlString);
            e.printStackTrace();
        } catch (DiskCacheUtils.NoCacheAvailableException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return mXMusicEntryList;
    }

    private void removeURLCache(String urlString) {
        try {
            HTTPCache cache = HTTPCache.getDefaultCache();
            cache.remove(new URI(urlString));
        } catch (DiskCacheUtils.NoCacheAvailableException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

}
