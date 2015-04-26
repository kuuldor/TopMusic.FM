package the.topmusic.HTTP;

import java.io.InputStream;
import java.util.Map;

import static the.topmusic.utils.DiskCacheUtils.NoCacheAvailableException;

/**
 * Created by lucd on 9/21/14.
 */
public class HTTPCacheResponse {
    boolean stale;
    private InputStream bodyStream;
    private Map<String, String> headerMap;

    public HTTPCacheResponse() {
        stale = false;
        bodyStream = null;
        headerMap = null;
    }

    public boolean isStale() throws NoCacheAvailableException {
        if (!stale) {
            stale = !HTTPCache.getDefaultCache().isCachedDataCurrentForRequest(this);
        }
        return stale;
    }

    public void setStale(boolean stale) {
        this.stale = stale;
    }

    public void setBodyStream(InputStream bodyStream) {
        this.bodyStream = bodyStream;
    }

    public void setHeaderMap(Map<String, String> headerMap) {
        this.headerMap = headerMap;
    }

    public InputStream getBody() {
        return bodyStream;
    }

    public Map<String, String> getHeaders() {
        return headerMap;
    }
}
