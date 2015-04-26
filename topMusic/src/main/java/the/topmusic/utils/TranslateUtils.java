package the.topmusic.utils;

import android.text.Html;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;

import the.topmusic.HTTP.HTTPServiceException;

/**
 * Created by lucd on 9/21/14.
 */
public class TranslateUtils {
    static final public String translate(String s, String to_lang) throws IOException, JSONException {
        return translate(s, to_lang, null);
    }

    static final public String translate(String s, String to_lang, String force_from_lang) throws IOException, JSONException {
        JSONObject api_ret = null;
        String api_key = "AIzaSyCcvuJrAsLNOw3_uygykHFWn0iwhKF7ISQ";
        String api_url;
        String data = null;
        String from_lang;
        if (force_from_lang == null) {
            String detect_url_pattern = "https://www.googleapis.com/language/translate/v2/detect?key=%s&q=%s";
            api_url = String.format(detect_url_pattern, api_key, URLEncoder.encode(s, "utf-8"));
            try {
                data = URLUtils.getUrlAsString(api_url);
            } catch (Exception e) {
                e.printStackTrace();
            } catch (DiskCacheUtils.NoCacheAvailableException e) {
                e.printStackTrace();
            } catch (HTTPServiceException e) {
                e.printStackTrace();
            }
            if (data != null) {
                api_ret = new JSONObject(data);
            }
            if (api_ret != null) {
                from_lang = api_ret.getJSONObject("data").getJSONArray("detections").getJSONArray(0).getJSONObject(0).getString("language");
            } else {
                return null;
            }
        } else {
            from_lang = force_from_lang;
        }

        String translate_url_pattern = "https://www.googleapis.com/language/translate/v2?key=%s&source=%s&target=%s&q=%s";
        api_url = String.format(translate_url_pattern, api_key, from_lang, to_lang, URLEncoder.encode(s, "utf-8"));
        try {
            data = URLUtils.getUrlAsString(api_url);
        } catch (Exception e) {
            e.printStackTrace();
        } catch (DiskCacheUtils.NoCacheAvailableException e) {
            e.printStackTrace();
        } catch (HTTPServiceException e) {
            e.printStackTrace();
        }
        if (data != null) {
            api_ret = new JSONObject(data);
        }
        if (api_ret == null) {
            return null;
        }

        final String trans = api_ret.getJSONObject("data").getJSONArray("translations").getJSONObject(0).getString("translatedText");

        return String.valueOf(Html.fromHtml(trans));
    }
}
