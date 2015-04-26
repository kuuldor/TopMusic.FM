package the.topmusic.utils;

import android.content.Context;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import the.topmusic.HTTP.HTTPCache;
import the.topmusic.HTTP.HTTPServiceException;
import the.topmusic.HTTP.HttpConnect;

/**
 * Created by lucd on 9/13/14.
 */
public class URLUtils {
    /**
     * @param url The {@link java.net.URL} to fecth the lyrics from
     * @return The {@link java.net.URL} used to fetch the lyrics as a {@link String}
     * @throws java.io.IOException
     */

    public static String getUrlAsString(final String url) throws Exception, DiskCacheUtils.NoCacheAvailableException, HTTPServiceException {
        return getUrlAsString(url, 60*1000, false);
    }

    public static String getUrlAsString(final String url, int timeout) throws Exception, DiskCacheUtils.NoCacheAvailableException, HTTPServiceException {
        return getUrlAsString(url, timeout, false);
    }

    public static void enableHttpResponseCache(Context context) {
//        try {
//            long httpCacheSize = 100 * 1024 * 1024; // 100 MiB
//            File httpCacheDir = new File(context.getCacheDir(), "http");
//            Class.forName("android.net.http.HttpResponseCache")
//                    .getMethod("install", File.class, long.class)
//                    .invoke(null, httpCacheDir, httpCacheSize);
//        } catch (Exception httpResponseCacheNotAvailable) {
//        }
        HTTPCache.setDefaultCacheSize(context, 100 * 1024 * 1024); // 100 MiB
    }

    public static String getUrlAsString(final String url, int timeout, boolean useCache) throws Exception, DiskCacheUtils.NoCacheAvailableException, HTTPServiceException {

        // Perform a GET request for the lyrics
//        final HttpURLConnection httpURLConnection = (HttpURLConnection)url.openConnection();
//        httpURLConnection.setRequestMethod("GET");
//        httpURLConnection.setReadTimeout(timeout);
//        httpURLConnection.setUseCaches(useCache);
//        httpURLConnection.connect();
        HttpConnect httpURLConnection = new HttpConnect(url);
        if (httpURLConnection.execute(false)) {
            final InputStreamReader input = new InputStreamReader(httpURLConnection.getResponseStream());
            // Read the server output
            final BufferedReader reader = new BufferedReader(input);
            // Build the URL
            final StringBuilder builder = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append("\n");
            }
            return builder.toString();
        }

        return null;
    }
}
