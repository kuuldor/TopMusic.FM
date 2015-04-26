package the.topmusic.model;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by lucd on 9/13/14.
 */
public class XMusicEntry {
    /**
     * The unique Id of the MusicEntry
     */
    public String mMusicEntryId;

    /**
     * The MusicEntry name
     */
    public String mMusicEntryName;

    public String mMusicEntryType;

    public String mUrl;
    public boolean mExpandable;
    public JSONObject mJSONItem;

    /**
     * Constructor of <code>MusicEntry</code>
     *
     * @param entryId   The Id of the MusicEntry
     * @param entryName The MusicEntry name
     * @param entryType The MusicEntry name
     */
    public XMusicEntry(final String entryId, final String entryName, final String entryType) {
        super();
        mMusicEntryId = entryId;
        mMusicEntryName = entryName;
        mMusicEntryType = entryType;
        mExpandable = false;
    }

    public XMusicEntry(JSONObject jsonObject) {
        try {
            mUrl = jsonObject.getString("url");
            mExpandable = jsonObject.getBoolean("expandable");
            mJSONItem = jsonObject.getJSONObject("listitem");
            mMusicEntryType = mJSONItem.optString("type", "list");
            String name = mJSONItem.optString("name", null);
            if (name == null) {
                name = mJSONItem.getString("label");
            }
            mMusicEntryName = name;
            mMusicEntryId = mJSONItem.optString("id", name);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    private String getItem(final String itemName) {
        String item = null;
        if (mJSONItem != null) {
            item = mJSONItem.optString(itemName, null);
            if (item != null && item.equalsIgnoreCase("null")) {
                item = null;
            }
        }
        return item;
    }

    public String title() {
        return getItem("label");
    }

    public String subtitle() {
        return getItem("label2");
    }

    public String artistName() {
        String artist = getItem("artist");
        if (artist == null) {
            artist = subtitle();
        }
        return artist;
    }

    public String albumName() {
        return getItem("album");
    }

    public String imageURL() {
        return getItem("iconImage");
    }

    public String thumbnailURL() {
        return getItem("thumbnailImage");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (mMusicEntryId == null ? 0 : mMusicEntryId.hashCode());
        result = prime * result + (mMusicEntryName == null ? 0 : mMusicEntryName.hashCode());
        result = prime * result + (mMusicEntryType == null ? 0 : mMusicEntryType.hashCode());
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final XMusicEntry other = (XMusicEntry) obj;
        if (mMusicEntryId == null) {
            if (other.mMusicEntryId != null) {
                return false;
            }
        } else if (!mMusicEntryId.equals(other.mMusicEntryId)) {
            return false;
        }
        if (mMusicEntryName == null) {
            if (other.mMusicEntryName != null) {
                return false;
            }
        } else if (!mMusicEntryName.equals(other.mMusicEntryName)) {
            return false;
        }

        return mMusicEntryType.equalsIgnoreCase(other.mMusicEntryType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return mMusicEntryName;
    }
}
