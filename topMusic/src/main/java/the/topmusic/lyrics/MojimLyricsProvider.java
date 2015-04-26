package the.topmusic.lyrics;

import android.text.Html;
import android.util.Log;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import the.topmusic.HTTP.HTTPServiceException;
import the.topmusic.utils.DiskCacheUtils;
import the.topmusic.utils.URLUtils;

/**
 * Created by lucd on 9/21/14.
 */
public class MojimLyricsProvider implements LyricsProvider {
    private static final String PROVIDER_NAME = "Mojim";
    private static final String BASE_URL = "http://mojim.com";

    @Override
    public String getLyrics(String artist, String song) {
        if (artist == null || song == null) {
            return null;
        }
        String ret = null;

        try {
            String songURL = getLyricsURL(artist, song);
            if (songURL == null) {
                return null;
            }

            // And now get the full lyrics
            String html = URLUtils.getUrlAsString(songURL);

            ret = stripLyrics(html);

        } catch (MalformedURLException e) {
            Log.e("TopMusic", "Lyrics not found in " + getProviderName(), e);
        } catch (IOException e) {
            Log.e("TopMusic", "Lyrics not found in " + getProviderName(), e);
        } catch (HTTPServiceException e) {
            e.printStackTrace();
        } catch (DiskCacheUtils.NoCacheAvailableException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return ret;
    }

    private String stripLyrics(String html) {
        Pattern p = Pattern.compile("(?ms)</dt><dd>(.*?)</dl>");
        Matcher m = p.matcher(html);

        if (!m.find()) {
            return null;
        }
        html = m.group(1);

        html = html.replaceAll("(?ms)<script.*?</script>", "");
        html = String.valueOf(Html.fromHtml(html));

        html = html.replaceAll("更多更详尽.*?歌词网", "");

        return html;
    }

    private String getLyricsURL(String artist, String song) throws IOException {
        String searchURL = BASE_URL + "/" + URLEncoder.encode(song, "UTF-8") + ".html?g3";
        String html = null;
        try {
            html = URLUtils.getUrlAsString(searchURL);
        } catch (Exception e) {
            e.printStackTrace();
        } catch (DiskCacheUtils.NoCacheAvailableException e) {
            e.printStackTrace();
        } catch (HTTPServiceException e) {
            e.printStackTrace();
        }
        if (html == null) {
            return null;
        }

        Pattern p = Pattern.compile("(?ms)(<table class=\"iB\".*?.*?</table>)");
        Matcher m = p.matcher(html);

        if (!m.find()) {
            return null;
        }
        html = m.group(1);

        p = Pattern.compile("(?ms)<tr.*?>(\\s*<td.*?>.*?</td>\\s*)(\\s*<td.*?>.*?</td>\\s*)(\\s*<td.*?>.*?</td>\\s*)(\\s*<td.*?>.*?</td>\\s*)(\\s*<td.*?>.*?</td>\\s*)</tr>");
        m = p.matcher(html);
        Pattern[] tdParser = new Pattern[5];
        tdParser[0] = Pattern.compile("<td.*?>\\s*(\\d+)\\s*</td>");
        tdParser[1] = Pattern.compile("<td.*?<a.*?>\\s*(.*?)\\s*</a>.*?</td>");
        tdParser[2] = Pattern.compile("<td.*?<a.*?>\\s*(.*?)\\s*</a>.*?</td>");
        tdParser[3] = Pattern.compile("<td.*?>\\s*?(<a.*?>.*?</a>)\\s*?</td>");
        tdParser[4] = Pattern.compile("<td.*?>\\s*([0-9-]+)\\s*</td>");
        Matcher tdMather;
        Pattern nameParser = Pattern.compile("(?i)<a\\s*?href=\"(.+?)\".*?>.*?<font.*?>(.*)</a>");
        Matcher nameMather;
        while (m.find()) {
            boolean allMatch = true;
            String idx = null, singer = null, album = null, title = null, release = null, songurl = null;
            for (int i = 1; i < m.groupCount(); i++) {
                String td = m.group(i);
                tdMather = tdParser[i - 1].matcher(td);
                if (!tdMather.find()) {
                    allMatch = false;
                    break;
                }
                switch (i) {
                    case 1:
                        idx = tdMather.group(1);
                        break;
                    case 2:
                        singer = tdMather.group(1);
                        break;
                    case 3:
                        album = tdMather.group(1);
                        break;
                    case 4:
                        title = tdMather.group(1);
                        nameMather = nameParser.matcher(title);
                        if (nameMather.find()) {
                            songurl = nameMather.group(1);
                            title = nameMather.group(2);
                            title = title.replaceAll("(<font.*?>|</font>)", "");
                        }
                        break;
                    case 5:
                        release = tdMather.group(1);
                        break;
                    default:
                        break;
                }

            }
            if (allMatch) {
                //Log.e("Matcher", String.format("%s %s %s %s %s %s", idx, singer, album, title, release, songurl));
                if ((singer != null && (singer.equalsIgnoreCase(artist) || artist.contains(singer)))
                        && (title != null && title.equalsIgnoreCase(song))) {
                    return BASE_URL + songurl;
                }
            }
        }


        return null;
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }
}
