package in.ashwanthkumar.gocd.github.json;

import com.google.gson.*;
import in.ashwanthkumar.gocd.github.model.PullRequestStatus;
import in.ashwanthkumar.gocd.github.model.PullRequests;

import java.lang.reflect.Type;
import java.util.ArrayList;

public class PullRequestsSerDe implements JsonSerializer<PullRequests>, JsonDeserializer<PullRequests> {
    @Override
    public JsonElement serialize(PullRequests src, Type typeOfSrc, JsonSerializationContext context) {
        return context.serialize(src.getPullRequestStatuses());
    }

    @Override
    public PullRequests deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonArray prs = json.getAsJsonArray();
        ArrayList<PullRequestStatus> prStatuses = new ArrayList<PullRequestStatus>();
        for (JsonElement pr : prs) {
            PullRequestStatus prStatus = context.deserialize(pr, PullRequestStatus.class);
            prStatuses.add(prStatus);
        }
        return new PullRequests().setPullRequestStatuses(prStatuses);
    }
}
