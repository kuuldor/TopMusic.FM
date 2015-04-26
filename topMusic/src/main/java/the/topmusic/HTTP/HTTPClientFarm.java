package the.topmusic.HTTP;

import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

/**
 * Created by IntelliJ IDEA.
 * User: lucd
 * Date: 11-7-5
 * Time: PM1:57
 * To change this template use File | Settings | File Templates.
 */
public class HTTPClientFarm {
    private static final int SOCKET_TIMEOUT = 60000;
    private static final int CONNECTION_TIMEOUT = 20000;

    private static HTTPClientFarm instance = null;
    private ClientConnectionManager connManager;
    private DefaultHttpClient client;

    public HTTPClientFarm() {
        HttpParams httpParams = createDefaultHttpParams();

        SchemeRegistry registry = new SchemeRegistry();
        registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        registry.register(new Scheme("https", PlainSocketFactory.getSocketFactory(), 443));
        connManager = new ThreadSafeClientConnManager(httpParams, registry);
        client = new DefaultHttpClient(connManager, createDefaultHttpParams());
        client.setKeepAliveStrategy(new DefaultConnectionKeepAliveStrategy());
    }

    private static HttpParams createDefaultHttpParams() {
        BasicHttpParams httpParams = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, CONNECTION_TIMEOUT);
        HttpConnectionParams.setSoTimeout(httpParams, SOCKET_TIMEOUT);
        httpParams.setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
        return httpParams;
    }

    public static HttpClient getThreadSafeClient() {
        if (instance == null) {
            instance = new HTTPClientFarm();
        }

        return instance.client;
    }
}
