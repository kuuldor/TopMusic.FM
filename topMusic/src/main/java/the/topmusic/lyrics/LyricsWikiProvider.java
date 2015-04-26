
package the.topmusic.lyrics;

import android.text.Html;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;

import the.topmusic.HTTP.HTTPServiceException;
import the.topmusic.utils.DiskCacheUtils;
import the.topmusic.utils.TranslateUtils;
import the.topmusic.utils.URLUtils;

import static java.net.URLEncoder.encode;

public class LyricsWikiProvider implements LyricsProvider {

    // Currently, the only lyrics provider
    public static final String PROVIDER_NAME = "LyricsWiki";
    // URL used to fetch the lyrics
    private static final String LYRICS_URL = "http://lyrics.wikia.com/api.php?action=lyrics&fmt=json&func=getSong&artist=%1s&song=%1s";
    private static final String ARTIST_URL = "http://lyrics.wikia.com/api.php?action=lyrics&fmt=json&func=getSong&artist=%1s";
    // Timeout duration
    private static final int DEFAULT_HTTP_TIME = 15 * 1000;

    private static final int MAX_RETRY_STEPS = 1;  // Make this up to 3 to allow try different way to get lyrics

    /**
     * {@inheritDoc}
     */
    @Override
    public String getLyrics(String artist, String song) {
        if (artist == null || song == null) {
            return null;
        }
        String ret = null;

        SongInfo info = new SongInfo(artist, song);
        try {
            String songURL = getLyricURL(info.artist, info.song);
            for (int step = 1; songURL == null && step < MAX_RETRY_STEPS; step++) {
                SongInfo newInfo = processName(step, info);
                if (newInfo.artist.equals(info.artist) && newInfo.song.equals(info.song)) {
                    continue;
                }
                info = newInfo;
                songURL = getLyricURL(info.artist, info.song);
            }

            if (songURL == null) {
                return null;
            }

            // And now get the full lyrics
            String html = URLUtils.getUrlAsString(songURL);
            if (html == null) {
                return null;
            }
            // TODO Clean this up
            html = html.substring(html.indexOf("<div class='lyricbox'>"));
            int scriptEnd = html.indexOf("</script>&#");
            if (scriptEnd == -1) {
                scriptEnd = html.indexOf("</script>");
                if (scriptEnd != -1) {
                    html = html.substring(scriptEnd + 9);
                }
            } else while (scriptEnd != -1) {
                html = html.substring(scriptEnd + 9);
                scriptEnd = html.indexOf("</script>&#");
            }

            html = html.substring(0, html.indexOf("<!--"));
            ret = String.valueOf(Html.fromHtml(html));
            if (ret.trim().isEmpty()) {
                ret = null;
            }
        } catch (final MalformedURLException e) {
            Log.e("TopMusic", "Lyrics not found in " + getProviderName(), e);
        } catch (final IOException e) {
            Log.e("TopMusic", "Lyrics not found in " + getProviderName(), e);
        } catch (final JSONException e) {
            Log.e("TopMusic", "Lyrics not found in " + getProviderName(), e);
        } catch (final NumberFormatException e) {
            Log.e("TopMusic", "Lyrics not found in " + getProviderName(), e);
        } catch (final Exception e) {
            Log.e("TopMusic", "Lyrics not found in " + getProviderName(), e);
        } catch (HTTPServiceException e) {
            e.printStackTrace();
        } catch (DiskCacheUtils.NoCacheAvailableException e) {
            e.printStackTrace();
        }
        return ret;
    }

    private SongInfo processName(int step, SongInfo orig) throws IOException, JSONException {
        SongInfo info;
        switch (step) {
            case 0:
                info = orig;
                break;
            case 2:
                final String artistFullName = getArtistFullName(orig.artist);
                info = new SongInfo(artistFullName, orig.song);
                break;
            case 1:
                final String splitter = "##@@##";
                final String[] trans = TranslateUtils.translate(orig.artist + splitter + orig.song, "zh_TW", "zh_CN").split(splitter);
                final String artistTCName = trans[0];
                final String songTCName = trans[1];
                info = new SongInfo(artistTCName, songTCName);
                break;
//            case 3:
//                final String artistTCFullName = getArtistFullName(orig.artist);
//                info = new SongInfo(artistTCFullName, orig.song);
//                break;
            default:
                info = orig;
                break;
        }
        return info;
    }

    private String getLyricURL(String artist, String song) throws IOException, JSONException {
        // Get the lyrics URL
        String url = String.format(LYRICS_URL, encode(artist, "UTF-8"), encode(song, "UTF-8"));
        String contentOfURL = null;
        try {
            contentOfURL = URLUtils.getUrlAsString(url, DEFAULT_HTTP_TIME);
        } catch (Exception e) {
            e.printStackTrace();
        } catch (DiskCacheUtils.NoCacheAvailableException e) {
            e.printStackTrace();
        } catch (HTTPServiceException e) {
            e.printStackTrace();
        }
        if (contentOfURL == null) {
            return null;
        }

        JSONObject apiRet = new JSONObject(contentOfURL.replace("song = ", ""));

        final String songURL = apiRet.getString("url");
        if (songURL.endsWith("action=edit")) {
            return null;
        }
        return songURL;
    }

    private String getArtistFullName(String artist) throws IOException, JSONException {
        // Get the lyrics URL
        String url = String.format(ARTIST_URL, encode(artist, "UTF-8"));
        String contentOfURL = null;
        try {
            contentOfURL = URLUtils.getUrlAsString(url, DEFAULT_HTTP_TIME);
        } catch (Exception e) {
            e.printStackTrace();
        } catch (DiskCacheUtils.NoCacheAvailableException e) {
            e.printStackTrace();
        } catch (HTTPServiceException e) {
            e.printStackTrace();
        }
        JSONObject apiRet = new JSONObject(contentOfURL);
        final String artistName = apiRet.getString("artist");

        if (artistName == null || artistName.isEmpty()) {
            return artist;
        }

        final JSONArray albums = apiRet.getJSONArray("albums");
        if (albums == null || albums.length() == 0) {
            return artist;
        }
        return artistName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    public class SongInfo {
        public final String artist;
        public final String song;

        public SongInfo(String t, String u) {
            this.artist = t;
            this.song = u;
        }
    }

}
