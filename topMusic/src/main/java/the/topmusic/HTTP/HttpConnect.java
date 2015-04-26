package the.topmusic.HTTP;

import android.util.Log;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicHeader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.Map;

import static the.topmusic.utils.DiskCacheUtils.NoCacheAvailableException;

public class HttpConnect {
    private final Map<String, Header> headerMap;
    private HTTPCache cache;
    private URI uri;
    private InputStream responseStream;
    private byte[] responseData;
    private int cachePolicy;

    public HttpConnect(String url) throws Exception, NoCacheAvailableException {
        if (url == null || url.equals("")) {
            throw new URISyntaxException("Syntax Error", "Empty String");
        }
        this.uri = new URI(url);
        cache = HTTPCache.getDefaultCache();
        cachePolicy = HTTPCache.HTTPAskServerIfModifiedWhenStaleCachePolicy;
        headerMap = new LinkedHashMap<String, Header>();
    }

    public HttpConnect(URI url) throws NoCacheAvailableException {
        this.uri = url;
        cache = HTTPCache.getDefaultCache();
        headerMap = new LinkedHashMap<String, Header>();
    }

    public byte[] getResponseData() {
        return responseData;
    }

    public URI getUri() {
        return uri;
    }

    public Map<String, Header> getHeaderMap() {

        return headerMap;
    }

    public int getCachePolicy() {
        return cachePolicy;
    }

    public void setCachePolicy(int cachePolicy) {
        this.cachePolicy = cachePolicy;
    }

    public HTTPCache getCache() {
        return cache;
    }

    public void setCache(HTTPCache cache) {
        this.cache = cache;
    }

    public InputStream getResponseStream() {
        return responseStream;
    }

    public boolean execute(final boolean downloadToFile) throws NoCacheAvailableException, HTTPServiceException {
        resetContent();

        final HTTPCacheResponse cacheResponse = this.cache.get(uri, this.cachePolicy);
        if (cacheResponse != null) {
            Map<String, String> headers = cacheResponse.getHeaders();
            for (String key : headers.keySet()) {
                Header header = new BasicHeader(key, headers.get(key));
                headerMap.put(key, header);
            }
            responseStream = cacheResponse.getBody();
        }

        // If no cache, or Policy require load from network even cached
        if (cacheResponse == null
                || (cachePolicy & HTTPCache.HTTPAskServerIfModifiedCachePolicy) != 0
                || ((cachePolicy & HTTPCache.HTTPAskServerIfModifiedWhenStaleCachePolicy) != 0
                && cacheResponse.isStale())
                || (cachePolicy & HTTPCache.HTTPFallbackToCacheIfLoadFailsCachePolicy) != 0) {

            HttpClient httpClient = HTTPClientFarm.getThreadSafeClient();

            HttpGet httpGet = null;
            HttpPost httpPost = null;


            httpGet = new HttpGet(uri);

            if (cacheResponse != null) {
                // If cached, ask server if modified
                if ((cachePolicy & HTTPCache.HTTPAskServerIfModifiedCachePolicy) != 0
                        || (cachePolicy & HTTPCache.HTTPAskServerIfModifiedWhenStaleCachePolicy) != 0) {
                    Header etag = headerMap.get("Etag");
                    if (etag != null) {
                        httpGet.setHeader("If-None-Match", etag.getValue());
                    }

                    Header lastModified = headerMap.get("Last-Modified");
                    if (lastModified != null) {
                        httpGet.setHeader("If-Modified-Since", lastModified.getValue());
                    }
                }
            }


            ResponseHandler<InputStream> handler = new ResponseHandler<InputStream>() {

                public InputStream handleResponse(HttpResponse httpResponse) throws ClientProtocolException {
                    int status = httpResponse.getStatusLine().getStatusCode();
                    HttpEntity entity = httpResponse.getEntity();

                    InputStream responseStream = null;
                    if (status == HttpStatus.SC_NOT_MODIFIED) {
                        try {
                            cache.updateHeader(uri, cacheResponse);
                        } catch (NoCacheAvailableException e) {
                            e.printStackTrace();    //To change body of catch statement use File | Settings | File Templates.
                        } catch (IOException e) {
                            e.printStackTrace();    //To change body of catch statement use File | Settings | File Templates.
                        }
                    } else if (status == HttpStatus.SC_OK && null != entity) {
                        try {
                            Map<String, String> cachedHeaders = new LinkedHashMap<String, String>();
                            Header[] headers = httpResponse.getAllHeaders();
                            for (Header header : headers) {
                                headerMap.put(header.getName(), header);
                                cachedHeaders.put(header.getName(), header.getValue());
                            }

                            InputStream contentStream = entity.getContent();
                            if (downloadToFile) {
                                // TODO: the maxAge should come from configuration
                                try {
                                    cache.put(uri, cachedHeaders, contentStream, 3600);
                                    HTTPCacheResponse responseCached = cache.get(uri, HTTPCache.HTTPOnlyLoadIfNotCachedCachePolicy);
                                    if (responseCached != null) {
                                        responseStream = responseCached.getBody();
                                    }
                                } catch (NoCacheAvailableException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                responseData = readStreamContent(contentStream);
                                if ((cachePolicy & HTTPCache.HTTPDoNotWriteToCacheCachePolicy) == 0) {
                                    try {
                                        cache.put(uri, cachedHeaders, responseData, 3600);
                                    } catch (NoCacheAvailableException e) {
                                        e.printStackTrace();
                                    }
                                }
                                responseStream = new ByteArrayInputStream(responseData);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            throw new ClientProtocolException("服务器异常！");
                        }
                    } else {
                        Log.e(HttpConnect.this.getClass().toString(), "HTTP error: " + httpResponse.getStatusLine());
                        Log.e(HttpConnect.this.getClass().toString(), "Request URL: " + uri.toString());
                        throw new ClientProtocolException("服务器异常！");
                    }

                    return responseStream;
                }
            };

            try {
                if (httpGet != null) {
                    responseStream = httpClient.execute(httpGet, handler);
                } else {
                    responseStream = httpClient.execute(httpPost, handler);
                }
            } catch (ClientProtocolException exception) {
                resetContent();
                exception.printStackTrace();
                throw new HTTPServiceException(exception.getMessage());
            } catch (IOException ignored) {
                resetContent();
                ignored.printStackTrace();
                throw new HTTPServiceException("网络异常! " + ignored.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
                resetContent();
                throw new HTTPServiceException("网络异常! " + e.getMessage());
            }
        }

        return responseStream != null;
    }

    private void resetContent() {
        headerMap.clear();
        responseStream = null;
        responseData = null;
    }

    public String contentOfURL() throws HTTPServiceException {
        String ret = "";
        try {
            if (execute(false)) {
                ret = responseString();
            }
        } catch (NoCacheAvailableException e) {
            e.printStackTrace();
        }
        return ret;
    }

    public InputStream downloadURL() throws HTTPServiceException {
        InputStream ret = null;
        try {
            if (execute(true)) {
                ret = responseStream;
            }
        } catch (NoCacheAvailableException e) {
            e.printStackTrace();
        }
        return ret;
    }

    public String responseString() {
        if (responseStream == null) {
            return null;
        }

        byte[] responseData = null;
        try {
            responseData = readStreamContent(responseStream);
        } catch (IOException e) {
            return null;
        }
        String responseString = null;
        String encoding = "UTF-8";
        Header content_type = headerMap.get("Content-Type");
        HeaderElement[] headerElements = content_type.getElements();
        for (HeaderElement element : headerElements) {
            NameValuePair nvp = element.getParameterByName("charset");
            if (nvp != null) {
                encoding = nvp.getValue();
            }
        }
        try {
            responseString = new String(responseData, encoding);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return responseString;
    }

    private byte[] readStreamContent(InputStream inputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int readBytes = 0;
        while ((readBytes = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, readBytes);
        }

        return outputStream.toByteArray();
    }
}