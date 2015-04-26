package the.topmusic.provider;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;
import android.util.Log;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import the.topmusic.model.Album;
import the.topmusic.model.Artist;
import the.topmusic.model.Song;
import the.topmusic.model.XMusicEntry;

/**
 * Created by lucd on 10/25/14.
 */
public class MusicStore extends SQLiteOpenHelper {

    private static String DB_NAME = "topmusic.db";
    private static MusicStore sInstance = null;
    private final Context myContext;

    /**
     * Constructor
     * Takes and keeps a reference of the passed context in order to access to the application assets and resources.
     *
     * @param context
     */
    public MusicStore(Context context) {

        super(context, DB_NAME, null, 1);
        this.myContext = context;

        try {
            createDataBase(false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param context The {@link Context} to use
     * @return A new instance of this class
     */
    public static synchronized MusicStore getInstance(final Context context) {
        if (sInstance == null) {
            sInstance = new MusicStore(context.getApplicationContext());
        }
        return sInstance;
    }


    /**
     * Creates a empty database on the system and rewrites it with your own database.
     */
    public void createDataBase(boolean force) throws IOException {

        if (!force && isDataBaseExist()) {
            //do nothing - database already exist
        } else {

            //By calling this method and empty database will be created into the default system path
            //of your application so we are gonna be able to overwrite that database with our database.
            this.getReadableDatabase();

            try {

                copyDataBase();

            } catch (IOException e) {

                throw new Error("Error copying database");

            }
        }

    }

    /**
     * Check if the database already exist to avoid re-copying the file each time you open the application.
     *
     * @return true if it exists, false if it doesn't
     */
    private boolean isDataBaseExist() {

        SQLiteDatabase checkDB = null;

        try {
            String myPath = myContext.getDatabasePath(DB_NAME).getAbsolutePath();
            checkDB = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READONLY);

        } catch (Exception e) {

            //database does't exist yet.

        }

        if (checkDB != null) {

            checkDB.close();

        }

        return checkDB != null;
    }

    /**
     * Copies your database from your local assets-folder to the just created empty database in the
     * system folder, from where it can be accessed and handled.
     * This is done by transfering bytestream.
     */
    private void copyDataBase() throws IOException {

        //Open your local db as the input stream
        InputStream myInput = myContext.getAssets().open(DB_NAME);

        // Path to the just created empty db
        String outFileName = myContext.getDatabasePath(DB_NAME).getAbsolutePath();

        //Open the empty db as the output stream
        OutputStream myOutput = new FileOutputStream(outFileName);

        //transfer bytes from the inputfile to the outputfile
        byte[] buffer = new byte[1024];
        int length;
        while ((length = myInput.read(buffer)) > 0) {
            myOutput.write(buffer, 0, length);
        }

        //Close the streams
        myOutput.flush();
        myOutput.close();
        myInput.close();

    }


    @Override
    public void onCreate(SQLiteDatabase db) {

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public void addMusicEntries(List<XMusicEntry> entryList) {
        if (entryList == null || entryList.size() == 0) {
            return;
        }

        final SQLiteDatabase database = getWritableDatabase();
        database.beginTransaction();

        for (XMusicEntry entry : entryList) {
            if (entry.mMusicEntryType.equalsIgnoreCase("song")) {
                insertSong(database, new Song(entry));
            } else if (entry.mMusicEntryType.equalsIgnoreCase("album")) {
                insertAlbum(database, new Album(entry));
            } else if (entry.mMusicEntryType.equalsIgnoreCase("artist")) {
                insertArtist(database, new Artist(entry));
            }
        }
        database.setTransactionSuccessful();
        database.endTransaction();
    }

    public String addPlaylist(String name) {
        if (TextUtils.isEmpty(name)) {
            return null;
        }

        final SQLiteDatabase database = getWritableDatabase();

        database.beginTransaction();

        final ContentValues values = new ContentValues(1);
        values.put(BasicColumns.NAME, name);
        long id = database.insertWithOnConflict(TableNames.PLAYLIST, null, values, SQLiteDatabase.CONFLICT_REPLACE);

        database.setTransactionSuccessful();
        database.endTransaction();

        String playlistId = null;
        if (id != -1) {
            playlistId = String.valueOf(id);
        }

        return playlistId;
    }

    public void deletePlaylist(String playlistId) {
        if (TextUtils.isEmpty(playlistId)) {
            return;
        }

        final SQLiteDatabase database = getWritableDatabase();

        database.beginTransaction();

        final String where = BasicColumns.ID + " = ?";
        final String[] values = new String[]{playlistId};
        long count = database.delete(TableNames.PLAYLIST, where, values);
        if (count <= 0) {
            Log.d(TableNames.PLAYLIST, "delete 0 row");
        }

        database.setTransactionSuccessful();
        database.endTransaction();

        return;
    }

    public String getPlaylistId(String name) {
        if (TextUtils.isEmpty(name)) {
            return null;
        }

        final SQLiteDatabase database = getReadableDatabase();
        final String[] projection = new String[]{BasicColumns.ID, BasicColumns.NAME};
        final String selection = BasicColumns.NAME + "=?";
        final String[] having = new String[]{
                name
        };

        String id = null;
        Cursor cursor = database.query(TableNames.PLAYLIST, projection, selection, having, null,
                null, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                id = cursor.getString(cursor.getColumnIndexOrThrow(BasicColumns.ID));
            }
            cursor.close();
            cursor = null;
        }
        return id;
    }

    public int addSongsToPlaylist(Song[] songs, String playlistid) {
        final int size = songs.length;
        final SQLiteDatabase database = getWritableDatabase();
        final String[] projection = new String[]{
                "count(*)"
        };
        final String where = "playlist = ?";
        final String table = TableNames.SONGLISTASSOC;
        Cursor cursor = database.query(table, projection, where, new String[]{playlistid}, null, null, null);
        cursor.moveToFirst();
        final int base = cursor.getInt(0);
        cursor.close();
        cursor = null;
        int numinserted = 0;
        database.beginTransaction();
        for (int offset = 0; offset < size; offset++) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(SongListColumns.PLAYLIST, Long.parseLong(playlistid));
            contentValues.put(SongListColumns.PLAYORDER, base + numinserted);
            String songId = songs[offset].mSongId;
            if (getSongById(songId) == null) {
                insertSong(database, songs[offset]);
            }
            contentValues.put(SongListColumns.SONG, Long.parseLong(songId));
            if (database.insert(table, null, contentValues) != -1) {
                numinserted++;
            } else {
                Log.d("addSongsToPlaylist", "Fail to insert song " + songs[offset].mSongName);
            }
        }
        database.setTransactionSuccessful();
        database.endTransaction();
        return numinserted;
    }

    public void addSong(Song song) {
        if (song == null) {
            return;
        }

        final SQLiteDatabase database = getWritableDatabase();

        database.beginTransaction();

        insertSong(database, song);

        database.setTransactionSuccessful();
        database.endTransaction();
    }


    public Song getSongById(String songId) {
        if (TextUtils.isEmpty(songId)) {
            return null;
        }

        final SQLiteDatabase database = getReadableDatabase();
        final String[] projection = new String[]{
                BasicColumns.ID,
                BasicColumns.NAME,
                SongColumns.ARTISTNAME,
                SongColumns.ALBUMNAME,
                SongColumns.ARTISTID,
                SongColumns.ALBUMID,
                SongColumns.DURATION,
                SongColumns.URL,
                BasicColumns.IMAGE,
                BasicColumns.THUMBNAIL
        };
        final String selection = BasicColumns.ID + "=?";
        final String[] having = new String[]{
                songId
        };

        Song song = null;
        Cursor cursor = database.query(TableNames.SONG, projection, selection, having, null,
                null, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                Song[] songs = getSongsFromCursor(cursor);
                song = songs[0];
            }
            cursor.close();
            cursor = null;
        }
        return song;
    }

    protected void insertSong(SQLiteDatabase database, Song song) {
        final ContentValues values = new ContentValues(9);
        values.put(BasicColumns.ID, song.mSongId);
        values.put(BasicColumns.NAME, song.mSongName);
        values.put(SongColumns.ALBUMNAME, song.mAlbumName);
        values.put(SongColumns.ARTISTNAME, song.mArtistName);
        values.put(SongColumns.ALBUMID, song.mAlbumId);
        values.put(SongColumns.ARTISTID, song.mArtistId);
        values.put(SongColumns.URL, song.mURL);
        values.put(BasicColumns.IMAGE, song.mImage);
        values.put(BasicColumns.THUMBNAIL, song.mThumbnail);
        values.put(SongColumns.DURATION, song.mDuration);

        database.insertWithOnConflict(TableNames.SONG, null, values, SQLiteDatabase.CONFLICT_IGNORE);
    }

    public void addAlbum(Album album) {
        if (album == null) {
            return;
        }

        final SQLiteDatabase database = getWritableDatabase();

        database.beginTransaction();

        insertAlbum(database, album);

        database.setTransactionSuccessful();
        database.endTransaction();
    }

    protected void insertAlbum(SQLiteDatabase database, Album album) {
        final ContentValues values = new ContentValues(6);

        values.put(BasicColumns.ID, album.mAlbumId);
        values.put(BasicColumns.NAME, album.mAlbumName);
        values.put(AlbumColumns.ARTISTNAME, album.mArtistName);
        values.put(AlbumColumns.PUBLISHYEAR, album.mYear);
        values.put(AlbumColumns.ARTISTID, album.mArtistId);
        values.put(BasicColumns.IMAGE, album.mImage);
        values.put(BasicColumns.THUMBNAIL, album.mThumbnail);

        database.insertWithOnConflict(TableNames.ALBUM, null, values, SQLiteDatabase.CONFLICT_IGNORE);
    }

    public void addArtist(Artist artist) {
        if (artist == null) {
            return;
        }
        final SQLiteDatabase database = getWritableDatabase();

        database.beginTransaction();

        insertArtist(database, artist);

        database.setTransactionSuccessful();
        database.endTransaction();
    }

    protected void insertArtist(SQLiteDatabase database, Artist artist) {
        final ContentValues values = new ContentValues(3);
        values.put(BasicColumns.ID, artist.mArtistId);
        values.put(BasicColumns.NAME, artist.mArtistName);
        values.put(BasicColumns.IMAGE, artist.mImage);
        values.put(BasicColumns.THUMBNAIL, artist.mThumbnail);

        database.insertWithOnConflict(TableNames.ARTIST, null, values, SQLiteDatabase.CONFLICT_IGNORE);
    }

    public Song[] getSongListForPlaylist(String playlistId) {
        if (TextUtils.isEmpty(playlistId)) {
            return null;
        }

        Cursor cursor = makePlaylistSongCursor(playlistId);

        if (cursor != null && cursor.moveToFirst()) {
            final Song[] list = getSongsFromCursor(cursor);
            cursor.close();
            cursor = null;
            return list;
        }

        return null;
    }

    public Song[] getSongsFromCursor(Cursor cursor) {
        final int len = cursor.getCount();
        final Song[] list = new Song[len];

        int idColumnIndex = cursor.getColumnIndex(BasicColumns.ID);
        int titleIndex = cursor.getColumnIndex(BasicColumns.NAME);
        int artistIndex = cursor.getColumnIndex(SongColumns.ARTISTNAME);
        int albumIndex = cursor.getColumnIndex(SongColumns.ALBUMNAME);
        int artistIDIndex = cursor.getColumnIndex(SongColumns.ARTISTID);
        int albumIDIndex = cursor.getColumnIndex(SongColumns.ALBUMID);
        int durationIndex = cursor.getColumnIndex(SongColumns.DURATION);
        int urlIndex = cursor.getColumnIndex(SongColumns.URL);
        int imageIndex = cursor.getColumnIndex(BasicColumns.IMAGE);
        int thumbnailIndex = cursor.getColumnIndex(BasicColumns.THUMBNAIL);

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
            String url = cursor.getString(urlIndex);
            String image = cursor.getString(imageIndex);
            String thumbnail = cursor.getString(thumbnailIndex);

            Song song = new Song(id, title, artist, artistID, album, albumID, duration);
            song.mURL = url;
            song.mImage = image;
            song.mThumbnail = thumbnail;

            list[i] = song;

            cursor.moveToNext();
        }
        return list;
    }

    public Cursor makePlaylistSongCursor(String playlistId) {
        final SQLiteDatabase database = getReadableDatabase();

        final String query = "SELECT * FROM " + TableNames.SONG + " song INNER JOIN " +
                TableNames.SONGLISTASSOC + " list ON song." + BasicColumns.ID + " = list."
                + SongListColumns.SONG + " WHERE list." + SongListColumns.PLAYLIST + " = ? ORDER BY "
                + SongListColumns.PLAYORDER;

        final String[] having = new String[]{
                playlistId
        };

        return database.rawQuery(query, having);
    }

    public void deleteSongFromPlaylist(String playlistId, String songId) {
        if (TextUtils.isEmpty(playlistId) || TextUtils.isEmpty(songId)) {
            return;
        }

        final SQLiteDatabase database = getWritableDatabase();

        database.beginTransaction();

        final String where = SongListColumns.PLAYLIST + " = ? AND " + SongListColumns.SONG + " = ?";
        final String[] values = new String[]{playlistId, songId};
        long count = database.delete(TableNames.SONGLISTASSOC, where, values);
        if (count <= 0) {
            Log.d(TableNames.SONGLISTASSOC, "delete 0 row");
        }

        database.setTransactionSuccessful();
        database.endTransaction();

        return;
    }

    public void moveItemInPlaylist(int from, int to) {

        if (from == to) {
            return;
        }

        final SQLiteDatabase database = getWritableDatabase();

        Cursor cursor;

        database.beginTransaction();

        final String queryUpdate = "UPDATE " + TableNames.SONGLISTASSOC + " SET play_order = -1 where play_order = ?";
        final String[] updateValue = new String[]{String.valueOf(from)};
        cursor = database.rawQuery(queryUpdate, updateValue);
        cursor.moveToFirst();
        cursor.close();

        String queryMove;
        final String[] moveValues = new String[]{String.valueOf(from), String.valueOf(to)};

        if (from < to) {
            queryMove = "UPDATE " + TableNames.SONGLISTASSOC + " SET play_order = play_order - 1 where play_order > ? AND play_order <= ?";
        } else {
            queryMove = "UPDATE " + TableNames.SONGLISTASSOC + " SET play_order = play_order + 1 where play_order < ? AND play_order >= ?";
        }
        cursor = database.rawQuery(queryMove, moveValues);
        cursor.moveToFirst();
        cursor.close();

        final String queryUpdate2 = "UPDATE " + TableNames.SONGLISTASSOC + " SET play_order = " + to + " where play_order = ?";
        final String[] updateValue2 = new String[]{"-1"};
        cursor = database.rawQuery(queryUpdate2, updateValue2);
        cursor.moveToFirst();
        cursor.close();

        database.setTransactionSuccessful();
        database.endTransaction();
    }


    public interface TableNames {
        public static final String SONG = "Song";
        public static final String ALBUM = "Album";
        public static final String ARTIST = "Artist";
        public static final String GENRE = "Genre";
        public static final String PLAYLIST = "Playlist";
        public static final String SONGLISTASSOC = "SongListAssoc";
    }

    public interface BasicColumns {
        public static final String ID = "_id";
        public static final String NAME = "name";
        public static final String IMAGE = "image";
        public static final String THUMBNAIL = "thumbnail";
    }

    public interface SongColumns {
        public static final String ALBUMNAME = "albumName";
        public static final String ARTISTNAME = "artistName";
        public static final String ALBUMID = "albumID";
        public static final String ARTISTID = "artistID";
        public static final String URL = "url";
        public static final String DURATION = "runLength";
    }

    public interface AlbumColumns {
        public static final String ARTISTNAME = "artistName";
        public static final String ARTISTID = "artistID";
        public static final String PUBLISHYEAR = "publishYear";
    }

    public interface SongListColumns {
        public static final String PLAYLIST = "playlist";
        public static final String SONG = "song";
        public static final String PLAYORDER = "play_order";
    }
}
