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

package the.topmusic.cache;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.ComponentCallbacks2;
import android.content.ContentUris;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.text.TextUtils;
import android.util.Log;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import the.topmusic.utils.DiskCacheUtils;
import the.topmusic.utils.TopMusicUtils;

import static the.topmusic.utils.DiskCacheUtils.GetFromCacheBlock;

/**
 * This class holds the memory and disk bitmap caches.
 */
public final class ImageCache implements DiskCacheUtils.DiskCacheUser {

    private static final String TAG = ImageCache.class.getSimpleName();

    /**
     * The {@link Uri} used to retrieve album art
     */
    private static final Uri mArtworkUri;

    /**
     * Default memory cache size as a percent of device memory class
     */
    private static final float MEM_CACHE_DIVIDER = 0.25f;

    /**
     * Default disk cache size 250MB
     */
    private static final int DISK_CACHE_SIZE = 1024 * 1024 * 250;

    /**
     * Compression settings when writing images to disk cache
     */
    private static final CompressFormat COMPRESS_FORMAT = CompressFormat.JPEG;

    /**
     * Image compression quality
     */
    private static final int COMPRESS_QUALITY = 95;
    private static ImageCache sInstance;
    /**
     * Used to temporarily pause the disk cache while scrolling
     */
    public boolean mPauseDiskAccess = false;

    static {
        mArtworkUri = Uri.parse("content://media/external/audio/albumart");
    }

    /**
     * LRU cache
     */
    private MemoryCache mLruCache;
    /**
     * Disk LRU cache
     */
    private DiskLruCache mDiskCache;
    private DiskCacheUtils mCacheUtil = null;

    /**
     * Constructor of <code>ImageCache</code>
     *
     * @param context The {@link android.content.Context} to use
     * @param diskCacheReadonly
     */
    public ImageCache(final Context context, boolean diskCacheReadonly) {
        init(context, diskCacheReadonly);
    }

    /**
     * Used to create a singleton of {@link ImageCache}
     *
     * @param context The {@link Context} to use
     * @return A new instance of this class.
     */
    public static ImageCache getInstance(final Context context, boolean diskCacheReadonly) {
        if (sInstance == null) {
            sInstance = new ImageCache(context.getApplicationContext(), diskCacheReadonly);
        }
        return sInstance;
    }

    public static ImageCache getInstance(final Context context) {
        return getInstance(context, true);
    }

    /**
     * Find and return an existing ImageCache stored in a {@link RetainFragment}
     * , if not found a new one is created using the supplied params and saved
     * to a {@link RetainFragment}
     *
     * @param activity The calling {@link android.support.v4.app.FragmentActivity}
     * @param diskCacheReadonly
     * @return An existing retained ImageCache object or a new one if one did
     * not exist
     */
    public static ImageCache findOrCreateCache(final SherlockFragmentActivity activity, boolean diskCacheReadonly) {

        // Search for, or create an instance of the non-UI RetainFragment
        final RetainFragment retainFragment = findOrCreateRetainFragment(activity
                .getSupportFragmentManager());

        // See if we already have an ImageCache stored in RetainFragment
        ImageCache cache = (ImageCache) retainFragment.getObject();

        // No existing ImageCache, create one and store it in RetainFragment
        if (cache == null) {
            cache = getInstance(activity, diskCacheReadonly);
            retainFragment.setObject(cache);
        }
        return cache;
    }

    /**
     * Locate an existing instance of this {@link Fragment} or if not found,
     * create and add it using {@link FragmentManager}
     *
     * @param fm The {@link FragmentManager} to use
     * @return The existing instance of the {@link Fragment} or the new instance
     * if just created
     */
    public static RetainFragment findOrCreateRetainFragment(final FragmentManager fm) {
        // Check to see if we have retained the worker fragment
        RetainFragment retainFragment = (RetainFragment) fm.findFragmentByTag(TAG);

        // If not retained, we need to create and add it
        if (retainFragment == null) {
            retainFragment = new RetainFragment();
            fm.beginTransaction().add(retainFragment, TAG).commit();
        }
        return retainFragment;
    }

    /**
     * Initialize the cache, providing all parameters.
     *
     * @param context The {@link android.content.Context} to use
     * @param diskCacheReadonly
     */
    private void init(final Context context, boolean diskCacheReadonly) {
        mCacheUtil = DiskCacheUtils.getDefaultCache();
        // Set up the disk cache
        if (mCacheUtil != null) {
            mCacheUtil.initDiskCacheFor(ImageCache.this, DISK_CACHE_SIZE, diskCacheReadonly);
        }
        
        // Set up the memory cache
        initLruCache(context);
    }

    /**
     * Sets up the Lru cache
     *
     * @param context The {@link Context} to use
     */
    @SuppressLint("NewApi")
    public void initLruCache(final Context context) {
        final ActivityManager activityManager = (ActivityManager) context
                .getSystemService(Context.ACTIVITY_SERVICE);
        final int lruCacheSize = Math.round(MEM_CACHE_DIVIDER * activityManager.getMemoryClass()
                * 1024 * 1024);
        mLruCache = new MemoryCache(lruCacheSize);

        // Release some memory as needed
        if (TopMusicUtils.hasICS()) {
            context.registerComponentCallbacks(new ComponentCallbacks2() {

                /**
                 * {@inheritDoc}
                 */
                @Override
                public void onTrimMemory(final int level) {
                    if (level >= TRIM_MEMORY_MODERATE) {
                        evictAll();
                    } else if (level >= TRIM_MEMORY_BACKGROUND) {
                        mLruCache.trimToSize(mLruCache.size() / 2);
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                @Override
                public void onLowMemory() {
                    // Nothing to do
                }

                /**
                 * {@inheritDoc}
                 */
                @Override
                public void onConfigurationChanged(final Configuration newConfig) {
                    // Nothing to do
                }
            });
        }
    }

    /**
     * Adds a new image to the memory and disk caches
     *
     * @param data   The key used to store the image
     * @param bitmap The {@link Bitmap} to cache
     */
    public void addBitmapToCache(final String data, final Bitmap bitmap) {
        if (data == null || bitmap == null) {
            return;
        }

        // Add to memory cache
        addBitmapToMemCache(data, bitmap);

        // Add to disk cache
        if (mDiskCache != null) {
            mCacheUtil.addToStreamedCache(mDiskCache, data, new DiskCacheUtils.AddToCacheBlock() {
                @Override
                public void writeTo(OutputStream out) {
                    bitmap.compress(COMPRESS_FORMAT, COMPRESS_QUALITY, out);

                }
            });
        }
    }

    /**
     * Called to add a new image to the memory cache
     *
     * @param data   The key identifier
     * @param bitmap The {@link Bitmap} to cache
     */
    public void addBitmapToMemCache(final String data, final Bitmap bitmap) {
        if (data == null || bitmap == null) {
            return;
        }
        // Add to memory cache
        if (getBitmapFromMemCache(data) == null) {
            mLruCache.put(data, bitmap);
        }
    }

    /**
     * Fetches a cached image from the memory cache
     *
     * @param data Unique identifier for which item to get
     * @return The {@link Bitmap} if found in cache, null otherwise
     */
    public final Bitmap getBitmapFromMemCache(final String data) {
        if (data == null) {
            return null;
        }
        if (mLruCache != null) {
            final Bitmap lruBitmap = mLruCache.get(data);
            if (lruBitmap != null) {
                return lruBitmap;
            }
        }
        return null;
    }

    /**
     * Fetches a cached image from the disk cache
     *
     * @param data Unique identifier for which item to get
     * @return The {@link Bitmap} if found in cache, null otherwise
     */
    public final Bitmap getBitmapFromDiskCache(final String data) {
        if (data == null) {
            return null;
        }

        // Check in the memory cache here to avoid going to the disk cache less
        // often
        if (getBitmapFromMemCache(data) != null) {
            return getBitmapFromMemCache(data);
        }

        while (mPauseDiskAccess) {
            // Pause for moment
        }

        if (mDiskCache != null) {
            final Bitmap[] bitmap = {null};

            mCacheUtil.getFileStreamFromCache(mDiskCache, data, new GetFromCacheBlock() {
                @Override
                public void readFrom(InputStream inputStream) {
                    bitmap[0] = BitmapFactory.decodeStream(inputStream);
                }

                @Override
                public boolean takeOwnership() {
                    return false;
                }
            });

            if (bitmap[0] != null) {
                return bitmap[0];
            }
        } else {
            Log.e(TAG, "Disk Cache is NULL");
        }
        return null;
    }

    /**
     * Tries to return a cached image from memory cache before fetching from the
     * disk cache
     *
     * @param data Unique identifier for which item to get
     * @return The {@link Bitmap} if found in cache, null otherwise
     */
    public final Bitmap getCachedBitmap(final String data) {
        if (data == null) {
            return null;
        }
        Bitmap cachedImage = getBitmapFromMemCache(data);
        if (cachedImage == null) {
            cachedImage = getBitmapFromDiskCache(data);
        }
        if (cachedImage != null) {
            addBitmapToMemCache(data, cachedImage);
            return cachedImage;
        }
        return null;
    }

    /**
     * Tries to return the album art from memory cache and disk cache, before
     * calling {@code #getArtworkFromFile(Context, String)} again
     *
     * @param context The {@link Context} to use
     * @param data    The name of the album art
     * @param id      The ID of the album to find artwork for
     * @return The artwork for an album
     */
    public final Bitmap getCachedArtwork(final Context context, final String data, final String id) {
        if (context == null || data == null) {
            return null;
        }
        Bitmap cachedImage = getCachedBitmap(data);
        if (cachedImage == null && id != null) {
            cachedImage = getArtworkFromFile(context, id);
        }
        if (cachedImage != null) {
            addBitmapToMemCache(data, cachedImage);
            return cachedImage;
        }
        return null;
    }

    /**
     * Used to fetch the artwork for an album locally from the user's device
     *
     * @param context The {@link Context} to use
     * @param albumId The ID of the album to find artwork for
     * @return The artwork for an album
     */
    public final Bitmap getArtworkFromFile(final Context context, final String albumId) {
        if (TextUtils.isEmpty(albumId)) {
            return null;
        }
        Bitmap artwork = null;
        while (mPauseDiskAccess) {
            // Pause for a moment
        }
        try {
            final Uri uri = ContentUris.withAppendedId(mArtworkUri, Long.valueOf(albumId));
            final ParcelFileDescriptor parcelFileDescriptor = context.getContentResolver()
                    .openFileDescriptor(uri, "r");
            if (parcelFileDescriptor != null) {
                final FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                artwork = BitmapFactory.decodeFileDescriptor(fileDescriptor);
            }
        } catch (final IllegalStateException e) {
            // Log.e(TAG, "IllegalStateExcetpion - getArtworkFromFile - ", e);
        } catch (final FileNotFoundException e) {
            // Log.e(TAG, "FileNotFoundException - getArtworkFromFile - ", e);
        } catch (final OutOfMemoryError evict) {
            // Log.e(TAG, "OutOfMemoryError - getArtworkFromFile - ", evict);
            evictAll();
        }
        return artwork;
    }


    /**
     * Clears the disk and memory caches
     */
    public void clearCaches() {
        TopMusicUtils.execute(false, new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(final Void... unused) {
                // Clear the disk cache
                try {
                    if (mDiskCache != null) {
                        mDiskCache.delete();
                        mDiskCache = null;
                    }
                } catch (final IOException e) {
                    Log.e(TAG, "clearCaches - " + e);
                }
                // Clear the memory cache
                evictAll();
                return null;
            }
        }, (Void[]) null);
    }

    /**
     * Closes the disk cache associated with this ImageCache object. Note that
     * this includes disk access so this should not be executed on the main/UI
     * thread.
     */
    public void close() {
        TopMusicUtils.execute(false, new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(final Void... unused) {
                if (mDiskCache != null) {
                    try {
                        if (!mDiskCache.isClosed()) {
                            mDiskCache.close();
                            mDiskCache = null;
                        }
                    } catch (final IOException e) {
                        Log.e(TAG, "close - " + e);
                    }
                }
                return null;
            }
        }, (Void[]) null);
    }

    /**
     * Evicts all of the items from the memory cache and lets the system know
     * now would be a good time to garbage collect
     */
    public void evictAll() {
        if (mLruCache != null) {
            mLruCache.evictAll();
        }
        System.gc();
    }

    /**
     * @param key The key used to identify which cache entries to delete.
     */
    public void removeFromCache(final String key) {
        if (key == null) {
            return;
        }
        // Remove the Lru entry
        if (mLruCache != null) {
            mLruCache.remove(key);
        }

        mCacheUtil.removeKeyFromCache(mDiskCache, key);
    }

    /**
     * flush() is called to synchronize up other methods that are accessing the
     * cache first
     */
    public void flush() {
        TopMusicUtils.execute(false, new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(final Void... unused) {
                mCacheUtil.flush(mDiskCache);
                return null;
            }
        }, (Void[]) null);
    }

    /**
     * Used to temporarily pause the disk cache while the user is scrolling to
     * improve scrolling.
     *
     * @param pause True to temporarily pause the disk cache, false otherwise.
     */
    public void setPauseDiskCache(final boolean pause) {
        mPauseDiskAccess = pause;
    }

    /**
     * @return True if the user is scrolling, false otherwise.
     */
    public boolean isScrolling() {
        return mPauseDiskAccess;
    }

    @Override
    public void setCache(DiskLruCache cache) {
        mDiskCache = cache;
    }

    @Override
    public String getUniqueIdentifier() {
        return TAG;
    }

    /**
     * A simple non-UI Fragment that stores a single Object and is retained over
     * configuration changes. In this sample it will be used to retain an
     * {@link ImageCache} object.
     */
    public static final class RetainFragment extends SherlockFragment {

        /**
         * The object to be stored
         */
        private Object mObject;

        /**
         * Empty constructor as per the {@link Fragment} documentation
         */
        public RetainFragment() {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            // Make sure this Fragment is retained over a configuration change
            setRetainInstance(true);
        }

        /**
         * Get the stored object
         *
         * @return The stored object
         */
        public Object getObject() {
            return mObject;
        }

        /**
         * Store a single object in this {@link Fragment}
         *
         * @param object The object to store
         */
        public void setObject(final Object object) {
            mObject = object;
        }
    }

    /**
     * Used to cache images via {@link LruCache}.
     */
    public static final class MemoryCache extends LruCache<String, Bitmap> {

        /**
         * Constructor of <code>MemoryCache</code>
         *
         * @param maxSize The allowed size of the {@link LruCache}
         */
        public MemoryCache(final int maxSize) {
            super(maxSize);
        }

        /**
         * Get the size in bytes of a bitmap.
         */
        @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
        public static int getBitmapSize(final Bitmap bitmap) {
            if (TopMusicUtils.hasHoneycombMR1()) {
                return bitmap.getByteCount();
            }
            /* Pre HC-MR1 */
            return bitmap.getRowBytes() * bitmap.getHeight();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected int sizeOf(final String paramString, final Bitmap paramBitmap) {
            return getBitmapSize(paramBitmap);
        }

    }

}
