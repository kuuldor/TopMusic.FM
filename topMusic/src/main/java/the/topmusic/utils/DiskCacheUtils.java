package the.topmusic.utils;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import the.topmusic.cache.DiskLruCache;

/**
 * Created by lucd on 9/21/14.
 */
public class DiskCacheUtils {
    private static final String TAG = DiskCacheUtils.class.getSimpleName();
    /**
     * Disk cache index to read from
     */
    private static final int DISK_CACHE_INDEX = 0;
    private static DiskCacheUtils instance = null;
    private static String mCachePath = null;

    private DiskCacheUtils(Context context) {
        init(context);
    }

    public static DiskCacheUtils getDefaultCache() {
        return instance;
    }

    public static void setupDefaultCache(final Context context) {
        if (instance == null) {
            instance = new DiskCacheUtils(context);
        }
    }

    /**
     * Get a usable cache directory (external if available, internal otherwise)
     *
     * @param uniqueName A unique directory name to append to the cache
     *                   directory
     * @return The cache directory
     */
    public static File getDiskCacheDir(final String uniqueName) {

        return new File(mCachePath + File.separator + uniqueName);
    }

    /**
     * http://stackoverflow.com/questions/332079
     *
     * @param bytes The bytes to convert.
     * @return A {@link String} converted from the bytes of a hashable key used
     * to store a filename on the disk, to hex digits.
     */
    private static String bytesToHexString(final byte[] bytes) {
        final StringBuilder builder = new StringBuilder();
        for (final byte b : bytes) {
            final String hex = Integer.toHexString(0xFF & b);
            if (hex.length() == 1) {
                builder.append('0');
            }
            builder.append(hex);
        }
        return builder.toString();
    }

    private void init(Context context) {
        mCachePath = Environment.MEDIA_MOUNTED.equals(Environment
                .getExternalStorageState()) || !isExternalStorageRemovable() ? getExternalCacheDir(
                context).getPath() : context.getCacheDir().getPath();
    }

    public void initDiskCacheFor(final DiskCacheUser user, final int cacheSize, boolean diskCacheReadonly) {
        // Initialize the disk cahe in a background thread
        try {
            initDiskCache(user, cacheSize, diskCacheReadonly);
        } catch (NoCacheAvailableException e) {
            e.printStackTrace();
        }
    }

    public void addToStreamedCache(final DiskLruCache cache, final String key, final AddToCacheBlock block) {
        if (cache == null || key == null || block == null) {
            return;
        }

        final String internal_key = hashKeyForDisk(key);
        OutputStream out = null;
        try {
            final DiskLruCache.Snapshot snapshot = cache.get(internal_key);
            if (snapshot == null) {
                final DiskLruCache.Editor editor = cache.edit(internal_key);
                if (editor != null) {
                    out = editor.newOutputStream(DISK_CACHE_INDEX);
                    block.writeTo(out);
                    editor.commit();
                    out.close();
                    flush(cache);
                }
            } else {
                snapshot.getInputStream(DISK_CACHE_INDEX).close();
            }
        } catch (final IOException e) {
            Log.e(TAG, "addToStreamedCache - " + e);
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (final IOException e) {
                Log.e(TAG, "addToStreamedCache - " + e);
            } catch (final IllegalStateException e) {
                Log.e(TAG, "addToStreamedCache - " + e);
            }
        }
    }

    public void addStringToCache(final DiskLruCache cache, final String key, final String value) {
        if (cache == null || key == null || value == null) {
            return;
        }

        final String internal_key = hashKeyForDisk(key);
        try {
            final DiskLruCache.Snapshot snapshot = cache.get(internal_key);
            if (snapshot == null) {
                final DiskLruCache.Editor editor = cache.edit(internal_key);
                if (editor != null) {
                    editor.set(DISK_CACHE_INDEX, value);
                    editor.commit();
                }
            } else {
                snapshot.getInputStream(DISK_CACHE_INDEX).close();
            }
        } catch (final IOException e) {
            Log.e(TAG, "addToStreamedCache - " + e);
        }
    }

    public String getStringFromCache(final DiskLruCache cache, final String index) {
        if (cache == null || index == null) {
            return null;
        }

        String retv = null;

        final String key = hashKeyForDisk(index);

        try {
            final DiskLruCache.Snapshot snapshot = cache.get(key);
            if (snapshot != null) {
                retv = snapshot.getString(DISK_CACHE_INDEX);
            }
        } catch (final IOException e) {
            Log.e(TAG, "getFromDiskCache - " + e);
        }
        return retv;
    }

    public void getFileStreamFromCache(final DiskLruCache cache, final String index, final GetFromCacheBlock block) {
        if (cache == null || index == null || block == null) {
            return;
        }
        final String key = hashKeyForDisk(index);

        InputStream inputStream = null;
        try {
            final DiskLruCache.Snapshot snapshot = cache.get(key);
            if (snapshot != null) {
                inputStream = snapshot.getInputStream(DISK_CACHE_INDEX);
                if (inputStream != null) {
                    block.readFrom(inputStream);
                }
            }
        } catch (final IOException e) {
            Log.e(TAG, "getFromDiskCache - " + e);
        } finally {
            try {
                if (inputStream != null && !block.takeOwnership()) {
                    inputStream.close();
                }
            } catch (final IOException e) {
            }
        }
    }

    /**
     * @param key The key used to identify which cache entries to delete.
     */
    public void removeKeyFromCache(final DiskLruCache cache, final String key) {
        if (key == null) {
            return;
        }

        try {
            // Remove the disk entry
            if (cache != null) {
                cache.remove(hashKeyForDisk(key));
            }
        } catch (final IOException e) {
            Log.e(TAG, "remove - " + e);
        }
        flush(cache);
    }

    /**
     * flush() is called to synchronize up other methods that are accessing the
     * cache first
     */
    public void flush(final DiskLruCache cache) {
        if (cache != null) {
            try {
                if (!cache.isClosed()) {
                    cache.flush();
                }
            } catch (final IOException e) {
                Log.e(TAG, "flush - " + e);
            }
        }
    }

    /**
     * Initializes the disk cache. Note that this includes disk access so this
     * should not be executed on the main/UI thread. By default an ImageCache
     * does not initialize the disk cache when it is created, instead you should
     * call initDiskCache() to initialize it on a background thread.
     *  @param user      The DiskUser
     * @param cacheSize Max size of cache in bytes
     * @param readOnly
     */
    private void initDiskCache(DiskCacheUser user, int cacheSize, boolean readOnly) throws NoCacheAvailableException {
        String TAG = user.getUniqueIdentifier();

        DiskLruCache diskCache = null;
        // Set up disk cache
        File diskCacheDir = getDiskCacheDir(TAG);
        if (diskCacheDir != null) {
            if (!diskCacheDir.exists()) {
                diskCacheDir.mkdirs();
            }
            if (getUsableSpace(diskCacheDir) > cacheSize) {
                try {
                    diskCache = DiskLruCache.open(diskCacheDir, 1, 1, cacheSize, readOnly);
                } catch (final IOException e) {
                    diskCacheDir = null;
                }
            }
        }
        if (diskCache == null) {
            throw new NoCacheAvailableException(TAG);
        }

        user.setCache(diskCache);
    }

    /**
     * Check if external storage is built-in or removable
     *
     * @return True if external storage is removable (like an SD card), false
     * otherwise
     */
    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    public boolean isExternalStorageRemovable() {
        return !TopMusicUtils.hasGingerbread() || Environment.isExternalStorageRemovable();
    }

    /**
     * Get the external app cache directory
     *
     * @param context The {@link android.content.Context} to use
     * @return The external cache directory
     */
    public File getExternalCacheDir(final Context context) {
        if (TopMusicUtils.hasFroyo()) {
            final File mCacheDir = context.getExternalCacheDir();
            if (mCacheDir != null) {
                return mCacheDir;
            }
        }

        /* Before Froyo we need to construct the external cache dir ourselves */
        final String mCacheDir = "/Android/data/" + context.getPackageName() + "/cache/";
        return new File(Environment.getExternalStorageDirectory().getPath() + mCacheDir);
    }

    /**
     * Check how much usable space is available at a given path.
     *
     * @param path The path to check
     * @return The space available in bytes
     */
    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    public long getUsableSpace(final File path) {
        if (TopMusicUtils.hasGingerbread()) {
            return path.getUsableSpace();
        }
        final StatFs stats = new StatFs(path.getPath());
        return (long) stats.getBlockSize() * (long) stats.getAvailableBlocks();
    }

    /**
     * A hashing method that changes a string (like a URL) into a hash suitable
     * for using as a disk filename.
     *
     * @param key The key used to store the file
     */
    public String hashKeyForDisk(final String key) {
        String cacheKey;
        try {
            final MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(key.getBytes());
            cacheKey = bytesToHexString(digest.digest());
        } catch (final NoSuchAlgorithmException e) {
            cacheKey = String.valueOf(key.hashCode());
        }
        return cacheKey;
    }

    public interface DiskCacheUser {
        void setCache(DiskLruCache cache);

        String getUniqueIdentifier();
    }

    public interface AddToCacheBlock {
        void writeTo(OutputStream out);
    }

    public interface GetFromCacheBlock {
        void readFrom(InputStream in);

        boolean takeOwnership();
    }

    public static class NoCacheAvailableException extends Throwable {
        public NoCacheAvailableException(final String desc) {
            super(desc);
        }
    }

}
