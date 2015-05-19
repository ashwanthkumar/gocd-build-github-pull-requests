package in.ashwanthkumar.gocd.github.util;

import java.net.URL;
import java.net.URLConnection;

public class URLUtils {
    public boolean isValidURL(String url) {
        boolean isValid = true;
        try {
            URL urlObj = new URL(url);
            URLConnection conn = urlObj.openConnection();
            conn.connect();
        } catch (Exception e) {
            isValid = false;
        }
        return isValid;
    }
}
