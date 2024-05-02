package in.ashwanthkumar.gocd.github.util;

import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

public class JSONUtils {
    public static <T> T fromJSON(String json) {
        return new GsonBuilder().create().fromJson(json, new TypeToken<T>() {}.getType());
    }

    public static <T> T fromJSON(String json, Class<T> type) {
        return new GsonBuilder().create().fromJson(json, type);
    }

    public static String toJSON(Object object) {
        return new GsonBuilder().create().toJson(object);
    }
}
