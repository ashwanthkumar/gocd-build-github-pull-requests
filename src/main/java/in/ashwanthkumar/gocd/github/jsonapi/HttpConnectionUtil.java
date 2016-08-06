package in.ashwanthkumar.gocd.github.jsonapi;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpConnectionUtil {

    public HttpURLConnection getConnection(URL url) throws IOException {
        return (HttpURLConnection) url.openConnection();
    }

    JsonElement responseToJson(Object content) {
        JsonParser parser = new JsonParser();
        return parser.parse(new InputStreamReader((InputStream) content));
    }

    <T> T convertResponse(JsonElement json, Class<T> type) {
        return new GsonBuilder().create().fromJson(json, type);
    }

    public <T> T responseToType(Object content, Class<T> type) {
        JsonElement element = responseToJson(content);
        return convertResponse(element, type);
    }

}
