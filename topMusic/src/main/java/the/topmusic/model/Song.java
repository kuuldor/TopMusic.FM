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

package the.topmusic.model;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;

/**
 * A class that represents a song.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class Song implements Parcelable {

    public static SongCreator CREATOR = new SongCreator();
    /**
     * The unique Id of the song
     */
    public String mSongId;
    /**
     * The song name
     */
    public String mSongName;
    /**
     * The song artist
     */
    public String mArtistName;
    /**
     * The song artist
     */
    public String mArtistId;
    /**
     * The song album
     */
    public String mAlbumName;
    /**
     * The song album
     */
    public String mAlbumId;
    /**
     * The song duration
     */
    public String mDuration;
    public String mURL;
    public String mImage;
    public String mThumbnail;

    /**
     * Constructor of <code>Song</code>
     *
     * @param songId     The Id of the song
     * @param songName   The name of the song
     * @param artistName The song artist
     * @param albumName  The song album
     * @param duration   The duration of a song
     */
    public Song(final String songId, final String songName, final String artistName, final String artistID,
                final String albumName, final String albumID, final String duration) {
        mSongId = songId;
        mSongName = songName;
        mArtistName = artistName != null ? artistName : "";
        mArtistId = artistID != null ? artistID : "";
        mAlbumName = albumName != null ? albumName : "";
        mAlbumId = albumID != null ? albumID : "";
        mDuration = duration != null ? duration : "";
        mURL = "";
        mImage = "";
        mThumbnail = "";
    }

    /**
     * This will be used only by the MyCreator
     *
     * @param source
     */
    public Song(Parcel source) {
            /*
             * Reconstruct from the Parcel
             */
        mSongId = source.readString();
        mSongName = source.readString();
        mArtistId = source.readString();
        mArtistName = source.readString();
        mAlbumId = source.readString();
        mAlbumName = source.readString();
        mDuration = source.readString();
        mURL = source.readString();
        mImage = source.readString();
        mThumbnail = source.readString();
    }

    public Song(XMusicEntry entry) {
        if (!entry.mMusicEntryType.equalsIgnoreCase("song")) {
            return;
        }
        mSongId = entry.mMusicEntryId;
        mSongName = entry.mMusicEntryName;
        mAlbumName = entry.albumName();
        mArtistName = entry.artistName();
        mArtistId = entry.mJSONItem.optString("artistID", null);
        mAlbumId = entry.mJSONItem.optString("albumID", null);
        final String URL;
        try {
            mURL = entry.mJSONItem.getString("songURL");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        mImage = entry.imageURL();
        mThumbnail = entry.thumbnailURL();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (mAlbumName == null ? 0 : mAlbumName.hashCode());
        result = prime * result + (mArtistName == null ? 0 : mArtistName.hashCode());
        result = prime * result + (mDuration == null ? 0 : mDuration.hashCode());
        result = prime * result + (mSongId == null ? 0 : mSongId.hashCode());
        result = prime * result + (mSongName == null ? 0 : mSongName.hashCode());
        result = prime * result + (mArtistId == null ? 0 : mArtistId.hashCode());
        result = prime * result + (mAlbumId == null ? 0 : mAlbumId.hashCode());
        result = prime * result + (mURL == null ? 0 : mURL.hashCode());
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
        if (obj.getClass() != Song.class) {
            return false;
        }
        final Song other = (Song) obj;
        if (mAlbumName == null) {
            if (other.mAlbumName != null) {
                return false;
            }
        } else if (!mAlbumName.equals(other.mAlbumName)) {
            return false;
        }
        if (mArtistName == null) {
            if (other.mArtistName != null) {
                return false;
            }
        } else if (!mArtistName.equals(other.mArtistName)) {
            return false;
        }
        if (mDuration == null) {
            if (other.mDuration != null) {
                return false;
            }
        } else if (!mDuration.equals(other.mDuration)) {
            return false;
        }
        if (mSongId == null) {
            if (other.mSongId != null) {
                return false;
            }
        } else if (!mSongId.equals(other.mSongId)) {
            return false;
        }
        if (mSongName == null) {
            if (other.mSongName != null) {
                return false;
            }
        } else if (!mSongName.equals(other.mSongName)) {
            return false;
        }
        if (mArtistId == null) {
            if (other.mArtistId != null) {
                return false;
            }
        } else if (!mArtistId.equals(other.mArtistId)) {
            return false;
        }
        if (mAlbumId == null) {
            if (other.mAlbumId != null) {
                return false;
            }
        } else if (!mAlbumId.equals(other.mAlbumId)) {
            return false;
        }
        if (mURL == null) {
            if (other.mURL != null) {
                return false;
            }
        } else if (!mURL.equals(other.mURL)) {
            return false;
        }

        return true;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mSongId);
        dest.writeString(mSongName);
        dest.writeString(mArtistId);
        dest.writeString(mArtistName);
        dest.writeString(mAlbumId);
        dest.writeString(mAlbumName);
        dest.writeString(mDuration);
        dest.writeString(mURL);
        dest.writeString(mImage);
        dest.writeString(mThumbnail);
    }

    /**
     * It will be required during un-marshaling data stored in a Parcel
     *
     * @author prasanta
     */
    public static class SongCreator implements Parcelable.Creator<Song> {
        public Song createFromParcel(Parcel source) {
            return new Song(source);
        }

        public Song[] newArray(int size) {
            return new Song[size];
        }
    }
}
