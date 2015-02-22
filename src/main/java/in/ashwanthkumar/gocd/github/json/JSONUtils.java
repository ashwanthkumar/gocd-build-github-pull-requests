package in.ashwanthkumar.gocd.github.json;

import com.google.gson.GsonBuilder;
import in.ashwanthkumar.gocd.github.model.PullRequests;

import java.util.HashMap;

public class JSONUtils {
    private final static GsonBuilder gsonBuilder = new GsonBuilder()
            .setExclusionStrategies(new AnnotationExclusionStrategy())
            .registerTypeAdapter(PullRequests.class, new PullRequestsSerDe());

    public static String toJson(Object anything) {
        return gsonBuilder.setPrettyPrinting().create().toJson(anything);
    }

    public static HashMap fromJsonAsMap(String json) {
        return gsonBuilder.create().fromJson(json, HashMap.class);
    }

    public static <T> T fromJson(String json, Class<T> type) {
        return gsonBuilder.create().fromJson(json, type);
    }

}
