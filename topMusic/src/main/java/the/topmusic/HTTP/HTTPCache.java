package the.topmusic.HTTP;

import android.content.Context;

import com.google.gson.Gson;

import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import the.topmusic.cache.DiskLruCache;
import the.topmusic.utils.DiskCacheUtils;

import static the.topmusic.utils.DiskCacheUtils.AddToCacheBlock;
import static the.topmusic.utils.DiskCacheUtils.DiskCacheUser;
import static the.topmusic.utils.DiskCacheUtils.GetFromCacheBlock;
import static the.topmusic.utils.DiskCacheUtils.NoCacheAvailableException;

/**
 * Created by lucd on 9/21/14.
 */
public class HTTPCache implements DiskCacheUser {
    private static final String TAG = HTTPCache.class.getSimpleName();
    // The default cache policy. When you set a request to use this, it will use the cache's defaultCachePolicy
    // public static int HTTPDownloadCache's default cache policy is 'public static int HTTPAskServerIfModifiedWhenStaleCachePolicy'
    public static int HTTPUseDefaultCachePolicy = 0;
    // Tell the request not to read from the cache
    public static int HTTPDoNotReadFromCacheCachePolicy = 1;
    // The the request not to write to the cache
    public static int HTTPDoNotWriteToCacheCachePolicy = 2;
    // Ask the server if there is an updated version of this resource (using a conditional GET) ONLY when the cached data is stale
    public static int HTTPAskServerIfModifiedWhenStaleCachePolicy = 4;
    // Always ask the server if there is an updated version of this resource (using a conditional GET)
    public static int HTTPAskServerIfModifiedCachePolicy = 8;
    // If cached data exists, use it even if it is stale. This means requests will not talk to the server unless the resource they are requesting is not in the cache
    public static int HTTPOnlyLoadIfNotCachedCachePolicy = 16;
    // If cached data exists, use it even if it is stale. If cached data does not exist, stop (will not set an error on the request)
    public static int HTTPDontLoadCachePolicy = 32;
    // Specifies that cached data may be used if the request fails. If cached data is used, the request will succeed without error. Usually used in combination with other options above.
    public static int HTTPFallbackToCacheIfLoadFailsCachePolicy = 64;
    /**
     * Default disk cache size 100MB
     */
    private static int DISK_CACHE_SIZE = 1024 * 1024 * 100;
    private static HTTPCache instance = null;
    private DiskLruCache mDiskCache;
    private DiskCacheUtils mCacheUtil;
    private DateFormat rfc1123DateFormatter;

    public HTTPCache() {
        mCacheUtil = DiskCacheUtils.getDefaultCache();
        mCacheUtil.initDiskCacheFor(this, DISK_CACHE_SIZE, false);
        rfc1123DateFormatter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US);
        rfc1123DateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public static HTTPCache getDefaultCache() throws NoCacheAvailableException {
        if (instance == null) {
            throw new NoCacheAvailableException("HTTP Cache Instance has not been set yet.");
        }
        return instance;
    }

    public static void setDefaultCacheSize(final Context context, long size) {
        DiskCacheUtils.setupDefaultCache(context);
        instance = new HTTPCache();
    }

    public static String digestForUrl(String url) {
        if ((url != null) && (url.length() > 0)) {
            return Integer.toHexString(url.hashCode());
        }

        return null;
    }

    public HTTPCacheResponse get(URI uri, int requestCachePolicy) {
        final HTTPCacheResponse response = new HTTPCacheResponse();

        if ((requestCachePolicy & HTTPDoNotReadFromCacheCachePolicy) != 0) {
            return null;
        } else if ((requestCachePolicy & HTTPDontLoadCachePolicy) != 0) {
            return response;
        }

        String headerPath = null;
        String bodyPath = null;
        try {
            headerPath = headerPathForURI(uri);
            bodyPath = bodyPathForURI(uri);
        } catch (NoCacheAvailableException e) {
            e.printStackTrace();
            return null;
        }

        synchronized (this) {
            try {
                final String jsHeader = mCacheUtil.getStringFromCache(mDiskCache, headerPath);

                if (jsHeader == null) {
                    return null;
                }

                LinkedHashMap<String, String> headerMap = new Gson().fromJson(jsHeader, new LinkedHashMap<String, String>().getClass());
                response.setHeaderMap(headerMap);

                FileInputStream bodyStream = null;
                mCacheUtil.getFileStreamFromCache(mDiskCache, bodyPath, new GetFromCacheBlock() {
                    @Override
                    public void readFrom(InputStream in) {
                        response.setBodyStream(in);
                    }

                    @Override
                    public boolean takeOwnership() {
                        return true;
                    }
                });

            } catch (Exception e) {
                return null;
            }
        }
        if ((requestCachePolicy & HTTPOnlyLoadIfNotCachedCachePolicy) != 0) {
            return response;
        } else if ((requestCachePolicy & HTTPFallbackToCacheIfLoadFailsCachePolicy) != 0) {
            return response;
        } else if ((requestCachePolicy & HTTPAskServerIfModifiedWhenStaleCachePolicy) != 0) {
            if (!isCachedDataCurrentForRequest(response)) {
                response.setStale(true);
            }
            return response;
        } else if ((requestCachePolicy & HTTPAskServerIfModifiedCachePolicy) != 0) {
            if (isCachedDataCurrentForRequest(response)) {
                return response;
            }
        }

        return null;
    }

    public boolean isCachedDataCurrentForRequest(HTTPCacheResponse response) {
        Date now = new Date();

        Map<String, String> cachedHeaders = response.getHeaders();
        String cacheControl = cachedHeaders.get("Cache-Control");
        if (cacheControl != null) {
            cacheControl = cacheControl.toLowerCase();
            int ageIndex = cacheControl.indexOf("max-age");
            if (ageIndex != -1) {
                String ageString = cacheControl.substring(ageIndex);
                String[] ageNVP = ageString.split("=");
                long maxAge = Long.parseLong(ageNVP[1]);

                String fetchDateStr = cachedHeaders.get("X-GYHTTPCache-Fetch-date");
                Date fetchDate = null;
                try {
                    fetchDate = rfc1123DateFormatter.parse(fetchDateStr);
                } catch (ParseException e) {
                    e.printStackTrace();
                    return false;
                }
                Date expiryDate = new Date(fetchDate.getTime() + maxAge * 1000);

                if (expiryDate.after(now)) {
                    return true;
                }
                // RFC 2612 says max-age must override any Expires header
                return false;
            }
        }

        // Look for an Expires header to see if the content is out of date
        String expires = cachedHeaders.get("Expires");
        if (expires != null) {
            Date expiryDate = null;
            try {
                expiryDate = rfc1123DateFormatter.parse(expires);
            } catch (ParseException e) {
                e.printStackTrace();
                return false;
            }
            if (expiryDate.after(now)) {
                return true;
            }
        }
        return false;
    }

    public void remove(URI uri) throws NoCacheAvailableException {
        String headerPath = headerPathForURI(uri);
        String bodyPath = bodyPathForURI(uri);

        mCacheUtil.removeKeyFromCache(mDiskCache, headerPath);
        mCacheUtil.removeKeyFromCache(mDiskCache, bodyPath);
    }

    public void put(URI uri, Map<String, String> headers, InputStream body) throws IOException, NoCacheAvailableException {
        this.put(uri, headers, body, 0);
    }

    public void updateHeader(URI uri, HTTPCacheResponse cacheResponse) throws NoCacheAvailableException, IOException {
        String headerPath = headerPathForURI(uri);
        String bodyPath = bodyPathForURI(uri);

        Map<String, String> headerMap = cacheResponse.getHeaders();
        // We use this special key to help expire the request when we get a max-age header
        Date now = new Date();
        headerMap.put("X-GYHTTPCache-Fetch-date", rfc1123DateFormatter.format(now));

        String jsHeader = new Gson().toJson(headerMap);
        synchronized (this) {
            mCacheUtil.removeKeyFromCache(mDiskCache, headerPath);
            mCacheUtil.addStringToCache(mDiskCache, headerPath, jsHeader);
        }
    }

    public void put(URI uri, Map<String, String> headerMap, final InputStream body, int maxAge) throws NoCacheAvailableException, IOException {
        String headerPath = headerPathForURI(uri);
        String bodyPath = bodyPathForURI(uri);

        stampTimeOnHeader(headerMap, maxAge);

        String jsHeader = new Gson().toJson(headerMap);
        synchronized (this) {
            mCacheUtil.addStringToCache(mDiskCache, headerPath, jsHeader);

            mCacheUtil.addToStreamedCache(mDiskCache, bodyPath, new AddToCacheBlock() {
                @Override
                public void writeTo(OutputStream out) {
                    byte[] buffer = new byte[4096];
                    int readBytes;
                    try {
                        while ((readBytes = body.read(buffer)) != -1) {
                            out.write(buffer, 0, readBytes);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    public String bodyPathForURL(String url) {
        if (url == null || url.trim().equals("")) {
            return null;
        }
        String ret = null;

        try {
            ret = bodyPathForURI(new URI(url));
        } catch (Exception e) {
            ret = null;
        }

        return ret;
    }

    public String bodyPathForURI(URI uri) {
        String[] urlStrs = uri.getPath().split("\\.");
        String ext = "";
        if (urlStrs.length > 1 && urlStrs[1].length() <= 4) {
            ext = "." + urlStrs[1];
        }
        if (ext.equalsIgnoreCase(".php")) {
            ext = ".html";
        }
        return pathForURI(uri) + ext;
    }

    private String headerPathForURI(URI uri) throws NoCacheAvailableException {
        return pathForURI(uri) + ".cachedHeader";
    }

    private String pathForURI(URI uri) {
        String urlPath = uri.toString();

        return urlPath;
    }

    public void put(URI uri, Map<String, String> headerMap, final byte[] responseData, int maxAge) throws NoCacheAvailableException, IOException {
        String headerPath = headerPathForURI(uri);
        String bodyPath = bodyPathForURI(uri);

        stampTimeOnHeader(headerMap, maxAge);

        String jsHeader = new Gson().toJson(headerMap);
        synchronized (this) {
            mCacheUtil.addStringToCache(mDiskCache, headerPath, jsHeader);

            mCacheUtil.addToStreamedCache(mDiskCache, bodyPath, new AddToCacheBlock() {
                @Override
                public void writeTo(OutputStream out) {
                    try {
                        out.write(responseData);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    private void stampTimeOnHeader(Map<String, String> headerMap, int maxAge) {
        if (maxAge != 0) {
            headerMap.remove("Expires");
            headerMap.put("Cache-Control", String.format("max-age=%d", maxAge));
        }
        // We use this special key to help expire the request when we get a max-age header
        Date now = new Date();
        headerMap.put("X-GYHTTPCache-Fetch-date", rfc1123DateFormatter.format(now));
    }

    public void put(URI uri, Map<String, String> headerMap, StringBuffer responseString, int maxAge) throws NoCacheAvailableException, IOException {
        String headerPath = headerPathForURI(uri);
        String bodyPath = bodyPathForURI(uri);

        stampTimeOnHeader(headerMap, maxAge);

        JSONObject jsHeader = new JSONObject(headerMap);
        synchronized (this) {
            File file = new File(headerPath);
            BufferedWriter fwr = new BufferedWriter(new FileWriter(file), 8192);
            fwr.write(jsHeader.toString());
            fwr.close();

            file = new File(bodyPath);
            fwr = new BufferedWriter(new FileWriter(file), 8192);
            fwr.write(responseString.toString());
            fwr.close();
        }
    }

    public boolean urlCached(String url) {
        try {
            return this.uriCached(new URI(url));
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean uriCached(URI uri) {
        String bodyPath = null;
        boolean cached = false;
        bodyPath = bodyPathForURI(uri);
        if (new File(bodyPath).exists()) {
            cached = true;
        }
        return cached;
    }

    @Override
    public void setCache(DiskLruCache cache) {
        mDiskCache = cache;
    }

    @Override
    public String getUniqueIdentifier() {
        return TAG;
    }

}