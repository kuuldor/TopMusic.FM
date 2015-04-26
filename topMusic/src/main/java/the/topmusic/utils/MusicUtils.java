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

package the.topmusic.utils;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio.AlbumColumns;
import android.provider.MediaStore.Audio.ArtistColumns;
import android.provider.MediaStore.Audio.AudioColumns;
import android.provider.MediaStore.MediaColumns;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.SubMenu;
import android.widget.Toast;

import java.io.File;
import java.util.Arrays;
import java.util.Formatter;
import java.util.Locale;
import java.util.WeakHashMap;

import the.topmusic.ITopMusicService;
import the.topmusic.MusicPlaybackService;
import the.topmusic.R;
import the.topmusic.loaders.FavoritesLoader;
import the.topmusic.loaders.LastAddedLoader;
import the.topmusic.loaders.PlaylistLoader;
import the.topmusic.loaders.SongLoader;
import the.topmusic.menu.FragmentMenuItems;
import the.topmusic.model.Song;
import the.topmusic.provider.FavoritesStore;
import the.topmusic.provider.FavoritesStore.FavoriteColumns;
import the.topmusic.provider.MusicStore;
import the.topmusic.provider.RecentStore;

/**
 * A collection of helpers directly related to music or TopMusic's service.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public final class MusicUtils {

    private static final WeakHashMap<Context, ServiceBinder> mConnectionMap;
    private static final Song[] sEmptyList;
    public static ITopMusicService mService = null;
    private static ContentValues[] mContentValuesCache = null;

    static {
        mConnectionMap = new WeakHashMap<Context, ServiceBinder>();
        sEmptyList = new Song[0];
    }

    /* This class is never initiated */
    public MusicUtils() {
    }

    /**
     * @param context  The {@link Context} to use
     * @param callback The {@link ServiceConnection} to use
     * @return The new instance of {@link ServiceToken}
     */
    public static ServiceToken bindToService(final Context context,
                                             final ServiceConnection callback) {
        Activity realActivity = ((Activity) context).getParent();
        if (realActivity == null) {
            realActivity = (Activity) context;
        }
        final ContextWrapper contextWrapper = new ContextWrapper(realActivity);
        contextWrapper.startService(new Intent(contextWrapper, MusicPlaybackService.class));
        final ServiceBinder binder = new ServiceBinder(callback);
        if (contextWrapper.bindService(
                new Intent().setClass(contextWrapper, MusicPlaybackService.class), binder, 0)) {
            mConnectionMap.put(contextWrapper, binder);
            return new ServiceToken(contextWrapper);
        }
        return null;
    }

    /**
     * @param token The {@link ServiceToken} to unbind from
     */
    public static void unbindFromService(final ServiceToken token) {
        if (token == null) {
            return;
        }
        final ContextWrapper mContextWrapper = token.mWrappedContext;
        final ServiceBinder mBinder = mConnectionMap.remove(mContextWrapper);
        if (mBinder == null) {
            return;
        }
        mContextWrapper.unbindService(mBinder);
        if (mConnectionMap.isEmpty()) {
            mService = null;
        }
    }

    public static String[] songListToIDList(Song[] songList) {
        int len = songList.length;
        String[] list = new String[len];
        for (int i = 0; i < len; i++) {
            list[i] = songList[i].mSongId;
        }
        return list;
    }

    /**
     * Used to make number of labels for the number of artists, albums, songs,
     * genres, and playlists.
     *
     * @param context   The {@link Context} to use.
     * @param pluralInt The ID of the plural string to use.
     * @param number    The number of artists, albums, songs, genres, or playlists.
     * @return A {@link String} used as a label for the number of artists,
     * albums, songs, genres, and playlists.
     */
    public static String makeLabel(final Context context, final int pluralInt,
                                   final String number) {
        try {
            final StringBuilder formatBuilder = new StringBuilder();
            final Formatter formatter = new Formatter(formatBuilder, Locale.getDefault());
            final StringBuilder builder = new StringBuilder();
            final String quantity = context.getResources()
                    .getQuantityText(pluralInt, Integer.valueOf(number)).toString();
            formatBuilder.setLength(0);
            formatter.format(quantity, Integer.valueOf(number));
            builder.append(formatBuilder);
            final String label = builder.toString();
            return label != null ? label : null;
        } catch (final IndexOutOfBoundsException fixme) {
            return null;
        }
    }

    /**
     * * Used to create a formatted time string for the duration of tracks.
     *
     * @param context The {@link Context} to use.
     * @param secs    The track in seconds.
     * @return Duration of a track that's properly formatted.
     */
    public static String makeTimeString(final Context context, final long secs) {
        final StringBuilder formatBuilder = new StringBuilder();
        final Formatter formatter = new Formatter(formatBuilder, Locale.getDefault());
        final String durationFormat = context.getResources().getString(
                secs < 3600 ? R.string.durationformatshort : R.string.durationformatlong);
        final Object[] mTimeArgs = new Object[5];
        formatBuilder.setLength(0);
        mTimeArgs[0] = secs / 3600;
        mTimeArgs[1] = secs / 60;
        mTimeArgs[2] = secs / 60 % 60;
        mTimeArgs[3] = secs;
        mTimeArgs[4] = secs % 60;
        final String mTime = formatter.format(durationFormat, mTimeArgs).toString();
        return mTime;
    }

    /**
     * Changes to the next track
     */
    public static void next() {
        if (mService != null) {
            TopMusicUtils.execute(true, new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    try {
                        mService.next();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    return null;
                }
            }, (Void[]) null);
        }
    }

    /**
     * Changes to the previous track.
     *
     * @NOTE The AIDL isn't used here in order to properly use the previous
     * action. When the user is shuffling, because {@link
     * MusicPlaybackService.#openCurrentAndNext()} is used, the user won't
     * be able to travel to the previously skipped track. To remedy this,
     * {@link MusicPlaybackService.#openCurrent()} is called in {@link
     * MusicPlaybackService.#prev()}. {@code #startService(Intent intent)}
     * is called here to specifically invoke the onStartCommand used by
     * {@link MusicPlaybackService}, which states if the current position
     * less than 2000 ms, start the track over, otherwise move to the
     * previously listened track.
     */
    public static void previous(final Context context) {
        final Intent previous = new Intent(context, MusicPlaybackService.class);
        previous.setAction(MusicPlaybackService.PREVIOUS_ACTION);
        context.startService(previous);
    }

    /**
     * Plays or pauses the music.
     */
    public static void playOrPause() {
        try {
            if (mService != null) {
                if (mService.isPlaying()) {
                    mService.pause();
                } else {
                    mService.play();
                }
            }
        } catch (final Exception ignored) {
        }
    }

    /**
     * Cycles through the repeat options.
     */
    public static void cycleRepeat() {
        try {
            if (mService != null) {
                switch (mService.getRepeatMode()) {
                    case MusicPlaybackService.REPEAT_NONE:
                        mService.setRepeatMode(MusicPlaybackService.REPEAT_ALL);
                        break;
                    case MusicPlaybackService.REPEAT_ALL:
                        mService.setRepeatMode(MusicPlaybackService.REPEAT_CURRENT);
                        if (mService.getShuffleMode() != MusicPlaybackService.SHUFFLE_NONE) {
                            mService.setShuffleMode(MusicPlaybackService.SHUFFLE_NONE);
                        }
                        break;
                    default:
                        mService.setRepeatMode(MusicPlaybackService.REPEAT_NONE);
                        break;
                }
            }
        } catch (final RemoteException ignored) {
        }
    }

    /**
     * Cycles through the shuffle options.
     */
    public static void cycleShuffle() {
        try {
            if (mService != null) {
                switch (mService.getShuffleMode()) {
                    case MusicPlaybackService.SHUFFLE_NONE:
                        mService.setShuffleMode(MusicPlaybackService.SHUFFLE_NORMAL);
                        if (mService.getRepeatMode() == MusicPlaybackService.REPEAT_CURRENT) {
                            mService.setRepeatMode(MusicPlaybackService.REPEAT_ALL);
                        }
                        break;
                    case MusicPlaybackService.SHUFFLE_NORMAL:
                        mService.setShuffleMode(MusicPlaybackService.SHUFFLE_NONE);
                        break;
                    case MusicPlaybackService.SHUFFLE_AUTO:
                        mService.setShuffleMode(MusicPlaybackService.SHUFFLE_NONE);
                        break;
                    default:
                        break;
                }
            }
        } catch (final RemoteException ignored) {
        }
    }

    /**
     * @return True if we're playing music, false otherwise.
     */
    public static boolean isPlaying() {
        if (mService != null) {
            try {
                return mService.isPlaying();
            } catch (final RemoteException ignored) {
            }
        }
        return false;
    }

    /**
     * @return The current shuffle mode.
     */
    public static int getShuffleMode() {
        if (mService != null) {
            try {
                return mService.getShuffleMode();
            } catch (final RemoteException ignored) {
            }
        }
        return 0;
    }

    /**
     * @return The current repeat mode.
     */
    public static int getRepeatMode() {
        if (mService != null) {
            try {
                return mService.getRepeatMode();
            } catch (final RemoteException ignored) {
            }
        }
        return 0;
    }

    /**
     * @return The current track name.
     */
    public static String getTrackName() {
        if (mService != null) {
            try {
                return mService.getTrackName();
            } catch (final RemoteException ignored) {
            }
        }
        return null;
    }

    /**
     * @return The current artist name.
     */
    public static String getArtistName() {
        if (mService != null) {
            try {
                return mService.getArtistName();
            } catch (final RemoteException ignored) {
            }
        }
        return null;
    }

    public static String getArtworkURL() {
        if (mService != null) {
            try {
                return mService.getArtworkURL();
            } catch (final RemoteException ignored) {
            }
        }
        return null;
    }

    public static String getArtworkThumbnailURL() {
        if (mService != null) {
            try {
                return mService.getArtworkThumbnailURL();
            } catch (final RemoteException ignored) {
            }
        }
        return null;
    }


    /**
     * @return The current album name.
     */
    public static String getAlbumName() {
        if (mService != null) {
            try {
                return mService.getAlbumName();
            } catch (final RemoteException ignored) {
            }
        }
        return null;
    }

    /**
     * @return The current album Id.
     */
    public static String getCurrentAlbumId() {
        if (mService != null) {
            try {
                return mService.getAlbumId();
            } catch (final RemoteException ignored) {
            }
        }
        return null;
    }

    /**
     * @return The current song Id.
     */
    public static String getCurrentAudioId() {
        if (mService != null) {
            try {
                return mService.getAudioId();
            } catch (final RemoteException ignored) {
            }
        }
        return null;
    }

    /**
     * @return The current artist Id.
     */
    public static String getCurrentArtistId() {
        if (mService != null) {
            try {
                return mService.getArtistId();
            } catch (final RemoteException ignored) {
            }
        }
        return null;
    }

    /**
     * @return The queue.
     */
    public static Song[] getQueue() {
        try {
            if (mService != null) {
                return mService.getQueue();
            }
        } catch (final RemoteException ignored) {
        }
        return sEmptyList;
    }

    /**
     * @param id The ID of the track to remove.
     * @return removes track from a playlist or the queue.
     */
    public static int removeTrack(final String id) {
        try {
            if (mService != null) {
                return mService.removeTrack(id);
            }
        } catch (final RemoteException ingored) {
        }
        return 0;
    }

    /**
     * @return The position of the current track in the queue.
     */
    public static int getQueuePosition() {
        try {
            if (mService != null) {
                return mService.getQueuePosition();
            }
        } catch (final RemoteException ignored) {
        }
        return 0;
    }

    /**
     * @param position The position to move the queue to
     */
    public static void setQueuePosition(final int position) {
        if (mService != null) {
            TopMusicUtils.execute(true, new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    try {
                        mService.setQueuePosition(position);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    return null;
                }
            }, (Void[]) null);
        }
    }

    /**
     * @param cursor The {@link Cursor} used to perform our query.
     * @return The song list for a MIME type.
     */
    public static Song[] getSongListForCursor(Cursor cursor) {
        if (cursor == null) {
            return sEmptyList;
        }
        final int len = cursor.getCount();
        final Song[] list = new Song[len];
        cursor.moveToFirst();
        int idColumnIndex = -1;
        int titleIndex = cursor.getColumnIndex(AudioColumns.TITLE);
        int artistIndex = cursor.getColumnIndex(AudioColumns.ARTIST);
        int albumIndex = cursor.getColumnIndex(AudioColumns.ALBUM);
        int artistIDIndex = cursor.getColumnIndex(AudioColumns.ARTIST_ID);
        int albumIDIndex = cursor.getColumnIndex(AudioColumns.ALBUM_ID);
        int durationIndex = cursor.getColumnIndex(AudioColumns.DURATION);

        try {
            idColumnIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Playlists.Members.AUDIO_ID);
        } catch (final IllegalArgumentException notaplaylist) {
            idColumnIndex = cursor.getColumnIndexOrThrow(BaseColumns._ID);
        }
        for (int i = 0; i < len; i++) {
            String id = cursor.getString(idColumnIndex);
            String title = cursor.getString(titleIndex);
            String artist = cursor.getString(artistIndex);
            String album = cursor.getString(albumIndex);
            String artistID = cursor.getString(artistIDIndex);
            String albumID = cursor.getString(albumIDIndex);
            String duration = "";
            if (durationIndex >= 0) {
                duration = cursor.getString(durationIndex);
            }

            list[i] = new Song(id, title, artist, artistID, album, albumID, duration);

            cursor.moveToNext();
        }
        cursor.close();
        cursor = null;
        return list;
    }

    /**
     * @param context The {@link Context} to use.
     * @param id      The ID of the artist.
     * @return The song list for an artist.
     */
    public static Song[] getSongListForArtist(final Context context, final String id) {
        final String[] projection = new String[]{
                BaseColumns._ID,
                AudioColumns.TITLE,
                AudioColumns.ALBUM_ID,
                AudioColumns.ALBUM,
                AudioColumns.ARTIST_ID,
                AudioColumns.ARTIST,
                AudioColumns.DURATION
        };
        final String selection = AudioColumns.ARTIST_ID + "=" + id + " AND "
                + AudioColumns.IS_MUSIC + "=1";
        Cursor cursor = context.getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection, null,
                AudioColumns.ALBUM_KEY + "," + AudioColumns.TRACK);
        if (cursor != null) {
            final Song[] mList = getSongListForCursor(cursor);
            cursor.close();
            cursor = null;
            return mList;
        }
        return sEmptyList;
    }

    /**
     * @param context The {@link Context} to use.
     * @param id      The ID of the album.
     * @return The song list for an album.
     */
    public static Song[] getSongListForAlbum(final Context context, final String id) {
        final String[] projection = new String[]{
                BaseColumns._ID,
                AudioColumns.TITLE,
                AudioColumns.ALBUM_ID,
                AudioColumns.ALBUM,
                AudioColumns.ARTIST_ID,
                AudioColumns.ARTIST,
                AudioColumns.DURATION
        };
        final String selection = AudioColumns.ALBUM_ID + "=" + id + " AND " + AudioColumns.IS_MUSIC
                + "=1";
        Cursor cursor = context.getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection, null,
                AudioColumns.TRACK + ", " + MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
        if (cursor != null) {
            final Song[] mList = getSongListForCursor(cursor);
            cursor.close();
            cursor = null;
            return mList;
        }
        return sEmptyList;
    }

    /**
     * @param context The {@link Context} to use.
     * @param id      The ID of the genre.
     * @return The song list for an genre.
     */
    public static Song[] getSongListForGenre(final Context context, final String id) {
        final String[] projection = new String[]{
                BaseColumns._ID,
                AudioColumns.TITLE,
                AudioColumns.ALBUM_ID,
                AudioColumns.ALBUM,
                AudioColumns.ARTIST_ID,
                AudioColumns.ARTIST,
                AudioColumns.DURATION
        };
        final StringBuilder selection = new StringBuilder();
        selection.append(AudioColumns.IS_MUSIC + "=1");
        selection.append(" AND " + MediaColumns.TITLE + "!=''");
        final Uri uri = MediaStore.Audio.Genres.Members.getContentUri("external", Long.valueOf(id));
        Cursor cursor = context.getContentResolver().query(uri, projection, selection.toString(),
                null, null);
        if (cursor != null) {
            final Song[] mList = getSongListForCursor(cursor);
            cursor.close();
            cursor = null;
            return mList;
        }
        return sEmptyList;
    }

    /**
     * @param context      The {@link Context} to use.
     * @param list         The list of songs to play.
     * @param position     Specify where to start.
     * @param forceShuffle True to force a shuffle, false otherwise.
     */
    public static void playAll(final Context context, final Song[] list, int position,
                               final boolean forceShuffle) {
        if (list.length == 0 || mService == null) {
            return;
        }
        try {
            if (forceShuffle) {
                mService.setShuffleMode(MusicPlaybackService.SHUFFLE_NORMAL);
            } else {
                mService.setShuffleMode(MusicPlaybackService.SHUFFLE_NONE);
            }
            final String currentId = mService.getAudioId();
            final int currentQueuePosition = getQueuePosition();
            if (position != -1 && currentQueuePosition == position && currentId != null && currentId.equals(list[position].mSongId)) {
                final Song[] playlist = getQueue();
                if (Arrays.equals(list, playlist)) {
                    mService.play();
                    return;
                }
            }
            if (position < 0) {
                position = 0;
            }

            final int finalPosition = position;
            TopMusicUtils.execute(true, new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    try {
                        mService.open(list, forceShuffle ? -1 : finalPosition);
                        mService.play();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    return null;
                }
            }, (Void[]) null);
        } catch (final RemoteException ignored) {
        }
    }

    /**
     * @param list The list to enqueue.
     */
    public static void playNext(final Song[] list) {
        if (mService == null) {
            return;
        }
        try {
            mService.enqueue(list, MusicPlaybackService.NEXT);
        } catch (final RemoteException ignored) {
        }
    }

    /**
     * @param context The {@link Context} to use.
     */
    public static void shuffleAll(final Context context) {
        Cursor cursor = SongLoader.makeSongCursor(context);
        final Song[] mTrackList = getSongListForCursor(cursor);

        final int position = 0;
        if (mTrackList.length == 0 || mService == null) {
            return;
        }
        try {
            mService.setShuffleMode(MusicPlaybackService.SHUFFLE_NORMAL);
            final String mCurrentId = mService.getAudioId();
            final int mCurrentQueuePosition = getQueuePosition();
            if (position != -1 && mCurrentQueuePosition == position
                    && mCurrentId != null && mTrackList != null
                    && mCurrentId.equals(mTrackList[position])) {
                final Song[] mPlaylist = getQueue();
                if (Arrays.equals(mTrackList, mPlaylist)) {
                    mService.play();
                    return;
                }
            }
            TopMusicUtils.execute(true, new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    try {
                        mService.open(mTrackList, -1);
                        mService.play();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    return null;
                }
            }, (Void[]) null);
            cursor.close();
            cursor = null;
        } catch (final RemoteException ignored) {
        }
    }

    /**
     * Returns The ID for a playlist.
     *
     * @param context The {@link Context} to use.
     * @param name    The name of the playlist.
     * @return The ID for a playlist.
     */
    public static String getIdForPlaylist(final Context context, final String name) {

        if (name == null) return null;

        String id = MusicStore.getInstance(context).getPlaylistId(name);

        return id;
    }

    /**
     * Returns the Id for an artist.
     *
     * @param context The {@link Context} to use.
     * @param name    The name of the artist.
     * @return The ID for an artist.
     */
    public static String getIdForArtist(final Context context, final String name) {
        Cursor cursor = context.getContentResolver().query(
                MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI, new String[]{
                        BaseColumns._ID
                }, ArtistColumns.ARTIST + "=?", new String[]{
                        name
                }, ArtistColumns.ARTIST);
        String id = null;
        if (cursor != null) {
            cursor.moveToFirst();
            if (!cursor.isAfterLast()) {
                int anInt = cursor.getInt(0);
                id = String.valueOf(anInt);
            }
            cursor.close();
            cursor = null;
        }
        return id;
    }

    /**
     * Returns the ID for an album.
     *
     * @param context The {@link Context} to use.
     * @param name    The name of the album.
     * @return The ID for an album.
     */
    public static String getIdForAlbum(final Context context, final String name) {
        Cursor cursor = context.getContentResolver().query(
                MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, new String[]{
                        BaseColumns._ID
                }, AlbumColumns.ALBUM + "=?", new String[]{
                        name
                }, AlbumColumns.ALBUM);
        String id = null;
        if (cursor != null) {
            cursor.moveToFirst();
            if (!cursor.isAfterLast()) {
                int anInt = cursor.getInt(0);
                id = String.valueOf(anInt);
            }
            cursor.close();
            cursor = null;
        }
        return id;
    }

    /**
     * Returns the artist name for a album.
     *
     * @param context The {@link Context} to use.
     * @param name    The name of the album.
     * @return The artist for an album.
     */
    public static String getAlbumArtist(final Context context, final String name) {
        Cursor cursor = context.getContentResolver().query(
                MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, new String[]{
                        AlbumColumns.ARTIST
                }, AlbumColumns.ALBUM + "=?", new String[]{
                        name
                }, AlbumColumns.ALBUM);
        String artistName = null;
        if (cursor != null) {
            cursor.moveToFirst();
            if (!cursor.isAfterLast()) {
                artistName = cursor.getString(0);
            }
            cursor.close();
            cursor = null;
        }
        return artistName;
    }

    /**
     * @param context The {@link Context} to use.
     * @param name    The name of the new playlist.
     * @return A new playlist ID.
     */
    public static String createPlaylist(final Context context, final String name) {
        if (name != null && name.length() > 0) {
            return MusicStore.getInstance(context).addPlaylist(name);
        }
        return null;
    }

    /**
     * @param context    The {@link Context} to use.
     * @param playlistId The playlist ID.
     */
    public static void clearPlaylist(final Context context, final String playlistId) {
        MusicStore.getInstance(context).deletePlaylist(playlistId);
    }

    /**
     * @param context    The {@link android.content.Context} to use.
     * @param songs      The id of the song(s) to add.
     * @param playlistid The id of the playlist being added to.
     */
    public static void addToPlaylist(final Context context, final Song[] songs, final String playlistid) {
        int numinserted = MusicStore.getInstance(context).addSongsToPlaylist(songs, playlistid);
        final String message = context.getResources().getQuantityString(
                R.plurals.NNNtrackstoplaylist, numinserted, numinserted);
        Toast.makeText((Activity) context, message, Toast.LENGTH_LONG).show();
    }

    /**
     * @param context The {@link Context} to use.
     * @param list    The list to enqueue.
     */
    public static void addToQueue(final Context context, final Song[] list) {
        if (mService == null) {
            return;
        }
        try {
            mService.enqueue(list, MusicPlaybackService.LAST);
            final String message = context.getResources().getQuantityString(
                    R.plurals.NNNtrackstoqueue, list.length, Integer.valueOf(list.length));
            Toast.makeText((Activity) context, message, Toast.LENGTH_LONG).show();
        } catch (final RemoteException ignored) {
        }
    }

    /**
     * @param context The {@link android.content.Context} to use
     * @param id      The song ID.
     */
    public static void setRingtone(final Context context, final String id) {
        final ContentResolver resolver = context.getContentResolver();
        final Uri uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, Long.valueOf(id));
        try {
            final ContentValues values = new ContentValues(2);
            values.put(AudioColumns.IS_RINGTONE, "1");
            values.put(AudioColumns.IS_ALARM, "1");
            resolver.update(uri, values, null, null);
        } catch (final UnsupportedOperationException ingored) {
            return;
        }

        final String[] projection = new String[]{
                BaseColumns._ID, MediaColumns.DATA, MediaColumns.TITLE
        };

        final String selection = BaseColumns._ID + "=" + id;
        Cursor cursor = resolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection,
                selection, null, null);
        try {
            if (cursor != null && cursor.getCount() == 1) {
                cursor.moveToFirst();
                Settings.System.putString(resolver, Settings.System.RINGTONE, uri.toString());
                final String message = context.getString(R.string.set_as_ringtone,
                        cursor.getString(2));
                Toast.makeText((Activity) context, message, Toast.LENGTH_LONG).show();
            }
        } finally {
            if (cursor != null) {
                cursor.close();
                cursor = null;
            }
        }
    }

    /**
     * @param context The {@link Context} to use.
     * @param name    The name of the album.
     * @return The song count for an album.
     */
    public static String getSongCountForAlbum(final Context context, final String name) {
        if (name == null) {
            return null;
        }
        Cursor cursor = context.getContentResolver().query(
                MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, new String[]{
                        AlbumColumns.NUMBER_OF_SONGS
                }, AlbumColumns.ALBUM + "=?", new String[]{
                        name
                }, AlbumColumns.ALBUM);
        String songCount = null;
        if (cursor != null) {
            cursor.moveToFirst();
            if (!cursor.isAfterLast()) {
                songCount = cursor.getString(0);
            }
            cursor.close();
            cursor = null;
        }
        return songCount;
    }

    /**
     * @param context The {@link Context} to use.
     * @param name    The name of the album.
     * @return The release date for an album.
     */
    public static String getReleaseDateForAlbum(final Context context, final String name) {
        if (name == null) {
            return null;
        }
        Cursor cursor = context.getContentResolver().query(
                MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, new String[]{
                        AlbumColumns.FIRST_YEAR
                }, AlbumColumns.ALBUM + "=?", new String[]{
                        name
                }, AlbumColumns.ALBUM);
        String releaseDate = null;
        if (cursor != null) {
            cursor.moveToFirst();
            if (!cursor.isAfterLast()) {
                releaseDate = cursor.getString(0);
            }
            cursor.close();
            cursor = null;
        }
        return releaseDate;
    }

    /**
     * @return The path to the currently playing file as {@link String}
     */
    public static String getFilePath() {
        try {
            if (mService != null) {
                return mService.getPath();
            }
        } catch (final RemoteException ignored) {
        }
        return null;
    }

    /**
     * @param from The index the item is currently at.
     * @param to   The index the item is moving to.
     */
    public static void moveQueueItem(final int from, final int to) {
        try {
            if (mService != null) {
                mService.moveQueueItem(from, to);
            } else {
            }
        } catch (final RemoteException ignored) {
        }
    }

    /**
     * Toggles the current song as a favorite.
     */
    public static void toggleFavorite() {
        try {
            if (mService != null) {
                mService.toggleFavorite();
            }
        } catch (final RemoteException ignored) {
        }
    }

    /**
     * @return True if the current song is a favorite, false otherwise.
     */
    public static boolean isFavorite() {
        try {
            if (mService != null) {
                return mService.isFavorite();
            }
        } catch (final RemoteException ignored) {
        }
        return false;
    }

    /**
     * @param context    The {@link Context} to sue
     * @param playlistId The playlist Id
     * @return The track list for a playlist
     */
    public static Song[] getSongListForPlaylist(final Context context, final String playlistId) {

        if (playlistId != null) {

            return MusicStore.getInstance(context).getSongListForPlaylist(playlistId);
        }
        return sEmptyList;
    }

    /**
     * Plays a user created playlist.
     *
     * @param context    The {@link Context} to use.
     * @param playlistId The playlist Id.
     */
    public static void playPlaylist(final Context context, final String playlistId) {
        final Song[] playlistList = getSongListForPlaylist(context, playlistId);
        if (playlistList != null) {
            playAll(context, playlistList, -1, false);
        }
    }

    /**
     * @param cursor The {@link Cursor} used to gather the list in our favorites
     *               database
     * @return The song list for the favorite playlist
     */
    public static Song[] getSongListForFavoritesCursor(Cursor cursor) {
        if (cursor == null) {
            return sEmptyList;
        }
        final int len = cursor.getCount();
        final Song[] list = new Song[len];
        cursor.moveToFirst();
        int ididx = -1;
        int titleidx = cursor.getColumnIndexOrThrow(FavoriteColumns.SONGNAME);
        int artistidx = cursor.getColumnIndexOrThrow(FavoriteColumns.ARTISTNAME);
        int albumidx = cursor.getColumnIndexOrThrow(FavoriteColumns.ALBUMNAME);
        int artistididx = cursor.getColumnIndexOrThrow(FavoriteColumns.ARTISTID);
        int albumididx = cursor.getColumnIndexOrThrow(FavoriteColumns.ALBUMID);

        try {
            ididx = cursor.getColumnIndexOrThrow(FavoriteColumns.ID);
        } catch (final Exception ignored) {
        }
        for (int i = 0; i < len; i++) {
            final String SongId = cursor.getString(ididx);
            final String SongName = cursor.getString(titleidx);
            final String ArtistID = cursor.getString(artistididx);
            final String ArtistName = cursor.getString(artistidx);
            final String AlbumID = cursor.getString(albumididx);
            final String AlbumName = cursor.getString(albumidx);
            final String Duration = null;

            list[i] = new Song(SongId, SongName, ArtistName, ArtistID, AlbumName, AlbumID, Duration);
            cursor.moveToNext();
        }
        cursor.close();
        cursor = null;
        return list;
    }

    /**
     * @param context The {@link Context} to use
     * @return The song list from our favorites database
     */
    public static Song[] getSongListForFavorites(final Context context) {
        Cursor cursor = FavoritesLoader.makeFavoritesCursor(context);
        if (cursor != null) {
            final Song[] list = getSongListForFavoritesCursor(cursor);
            cursor.close();
            cursor = null;
            return list;
        }
        return sEmptyList;
    }

    /**
     * Play the songs that have been marked as favorites.
     *
     * @param context The {@link Context} to use
     */
    public static void playFavorites(final Context context) {
        playAll(context, getSongListForFavorites(context), 0, false);
    }

    /**
     * @param context The {@link Context} to use
     * @return The song list for the last added playlist
     */
    public static Song[] getSongListForLastAdded(final Context context) {
        final Cursor cursor = LastAddedLoader.makeLastAddedCursor(context);
        if (cursor != null) {
            final int count = cursor.getCount();
            final Song[] list = new Song[count];
            for (int i = 0; i < count; i++) {
                cursor.moveToNext();
                final String SongId = cursor.getString(0);
                final String SongName = cursor.getString(1);
                final String ArtistName = cursor.getString(2);
                final String AlbumName = cursor.getString(3);
                final String ArtistID = cursor.getString(4);
                final String AlbumID = cursor.getString(5);
                final String Duration = null;

                list[i] = new Song(SongId, SongName, ArtistName, ArtistID, AlbumName, AlbumID, Duration);
            }
            return list;
        }
        return sEmptyList;
    }

    /**
     * Plays the last added songs from the past two weeks.
     *
     * @param context The {@link Context} to use
     */
    public static void playLastAdded(final Context context) {
        playAll(context, getSongListForLastAdded(context), 0, false);
    }

    /**
     * Creates a sub menu used to add items to a new playlist or an existsing
     * one.
     *
     * @param context       The {@link Context} to use.
     * @param groupId       The group Id of the menu.
     * @param subMenu       The {@link SubMenu} to add to.
     * @param showFavorites True if we should show the option to add to the
     *                      Favorites cache.
     */
    public static void makePlaylistMenu(final Context context, final int groupId,
                                        final SubMenu subMenu, final boolean showFavorites) {
        subMenu.clear();
        if (showFavorites) {
            subMenu.add(groupId, FragmentMenuItems.ADD_TO_FAVORITES, Menu.NONE,
                    R.string.add_to_favorites);
        }
        subMenu.add(groupId, FragmentMenuItems.NEW_PLAYLIST, Menu.NONE, R.string.new_playlist);
        Cursor cursor = PlaylistLoader.makePlaylistCursor(context);
        if (cursor != null && cursor.getCount() > 0 && cursor.moveToFirst()) {
            while (!cursor.isAfterLast()) {
                final Intent intent = new Intent();
                intent.putExtra("playlist", getIdForPlaylist(context, cursor.getString(1)));
                subMenu.add(groupId, FragmentMenuItems.PLAYLIST_SELECTED, Menu.NONE,
                        cursor.getString(1)).setIntent(intent);
                cursor.moveToNext();
            }
        }
        if (cursor != null) {
            cursor.close();
            cursor = null;
        }
    }

    public static void makePlaylistMenu(final Context context, final int groupId,
                                        final com.actionbarsherlock.view.SubMenu subMenu, final boolean showFavorites) {
        subMenu.clear();
        if (showFavorites) {
            subMenu.add(groupId, FragmentMenuItems.ADD_TO_FAVORITES, Menu.NONE,
                    R.string.add_to_favorites);
        }
        subMenu.add(groupId, FragmentMenuItems.NEW_PLAYLIST, Menu.NONE, R.string.new_playlist);
        Cursor cursor = PlaylistLoader.makePlaylistCursor(context);
        if (cursor != null && cursor.getCount() > 0 && cursor.moveToFirst()) {
            while (!cursor.isAfterLast()) {
                final Intent intent = new Intent();
                intent.putExtra("playlist", getIdForPlaylist(context, cursor.getString(1)));
                subMenu.add(groupId, FragmentMenuItems.PLAYLIST_SELECTED, Menu.NONE,
                        cursor.getString(1)).setIntent(intent);
                cursor.moveToNext();
            }
        }
        if (cursor != null) {
            cursor.close();
            cursor = null;
        }
    }

    /**
     * Called when one of the lists should refresh or requery.
     */
    public static void refresh() {
        try {
            if (mService != null) {
                mService.refresh();
            }
        } catch (final RemoteException ignored) {
        }
    }

    /**
     * Queries {@link RecentStore} for the last album played by an artist
     *
     * @param context    The {@link Context} to use
     * @param artistName The artist name
     * @return The last album name played by an artist
     */
    public static String getLastAlbumForArtist(final Context context, final String artistName) {
        return RecentStore.getInstance(context).getAlbumName(artistName);
    }

    /**
     * Seeks the current track to a desired position
     *
     * @param position The position to seek to
     */
    public static void seek(final long position) {
        if (mService != null) {
            try {
                mService.seek(position);
            } catch (final RemoteException ignored) {
            }
        }
    }

    /**
     * @return The current position time of the track
     */
    public static long position() {
        if (mService != null) {
            try {
                return mService.position();
            } catch (final RemoteException ignored) {
            }
        }
        return 0;
    }

    /**
     * @return The total length of the current track
     */
    public static long duration() {
        if (mService != null) {
            try {
                return mService.duration();
            } catch (final RemoteException ignored) {
            }
        }
        return 0;
    }

    /**
     * Clears the qeueue
     */
    public static void clearQueue() {
        try {
            mService.removeTracks(0, Integer.MAX_VALUE);
        } catch (final RemoteException ignored) {
        }
    }

    /**
     * Used to build and show a notification when TopMusic is sent into the
     * background
     *
     * @param context The {@link Context} to use.
     */
    public static void startBackgroundService(final Context context) {
        final Intent startBackground = new Intent(context, MusicPlaybackService.class);
        startBackground.setAction(MusicPlaybackService.START_BACKGROUND);
        context.startService(startBackground);
    }

    /**
     * Used to kill the current foreground notification
     *
     * @param context The {@link Context} to use.
     */
    public static void killForegroundService(final Context context) {
        final Intent killForeground = new Intent(context, MusicPlaybackService.class);
        killForeground.setAction(MusicPlaybackService.KILL_FOREGROUND);
        context.startService(killForeground);
    }

    /**
     * @param context The {@link Context} to use.
     * @return True if the mediascanner is running, false otherwise.
     */
    public static boolean isMediaScannerScanning(final Context context) {
        boolean result = false;
        final Cursor cursor = context.getContentResolver().query(MediaStore.getMediaScannerUri(),
                new String[]{
                        MediaStore.MEDIA_SCANNER_VOLUME
                }, null, null, null);
        if (cursor != null) {
            if (cursor.getCount() == 1) {
                cursor.moveToFirst();
                result = "external".equals(cursor.getString(0));
            }
            cursor.close();
        }
        return result;
    }

    /**
     * Perminately deletes item(s) from the user's device
     *
     * @param context The {@link Context} to use.
     * @param list    The item(s) to delete.
     */
    public static void deleteTracks(final Context context, final String[] list) {
        final String[] projection = new String[]{
                BaseColumns._ID, MediaColumns.DATA, AudioColumns.ALBUM_ID
        };
        final StringBuilder selection = new StringBuilder();
        selection.append(BaseColumns._ID + " IN (");
        for (int i = 0; i < list.length; i++) {
            selection.append(list[i]);
            if (i < list.length - 1) {
                selection.append(",");
            }
        }
        selection.append(")");
        final Cursor c = context.getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection.toString(),
                null, null);
        if (c != null) {
            // Step 1: Remove selected tracks from the current playlist, as well
            // as from the album art cache
            c.moveToFirst();
            while (!c.isAfterLast()) {
                // Remove from current playlist
                long aLong = c.getLong(0);
                final String id = String.valueOf(aLong);
                removeTrack(id);
                // Remove from the favorites playlist
                FavoritesStore.getInstance(context).removeItem(id);
                // Remove any items in the recents database
                long aLong1 = c.getLong(2);
                RecentStore.getInstance(context).removeItem(String.valueOf(aLong1));
                c.moveToNext();
            }

            // Step 2: Remove selected tracks from the database
            context.getContentResolver().delete(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    selection.toString(), null);

            // Step 3: Remove files from card
            c.moveToFirst();
            while (!c.isAfterLast()) {
                final String name = c.getString(1);
                final File f = new File(name);
                try { // File.delete can throw a security exception
                    if (!f.delete()) {
                        // I'm not sure if we'd ever get here (deletion would
                        // have to fail, but no exception thrown)
                        Log.e("MusicUtils", "Failed to delete file " + name);
                    }
                    c.moveToNext();
                } catch (final SecurityException ex) {
                    c.moveToNext();
                }
            }
            c.close();
        }

        final String message = context.getResources().getQuantityString(R.plurals.NNNtracksdeleted,
                list.length, list.length);

        Toast.makeText((Activity) context, message, Toast.LENGTH_LONG).show();
        // We deleted a number of tracks, which could affect any number of
        // things
        // in the media content domain, so update everything.
        context.getContentResolver().notifyChange(Uri.parse("content://media"), null);
        // Notify the lists to update
        refresh();
    }

    public static final class ServiceBinder implements ServiceConnection {
        private final ServiceConnection mCallback;

        /**
         * Constructor of <code>ServiceBinder</code>
         *
         * @param callback The {@link ServiceConnection} to use
         */
        public ServiceBinder(final ServiceConnection callback) {
            mCallback = callback;
        }

        @Override
        public void onServiceConnected(final ComponentName className, final IBinder service) {
            mService = ITopMusicService.Stub.asInterface(service);
            if (mCallback != null) {
                mCallback.onServiceConnected(className, service);
            }
        }

        @Override
        public void onServiceDisconnected(final ComponentName className) {
            if (mCallback != null) {
                mCallback.onServiceDisconnected(className);
            }
            mService = null;
        }
    }

    public static final class ServiceToken {
        public ContextWrapper mWrappedContext;

        /**
         * Constructor of <code>ServiceToken</code>
         *
         * @param context The {@link ContextWrapper} to use
         */
        public ServiceToken(final ContextWrapper context) {
            mWrappedContext = context;
        }
    }
}
