package in.ashwanthkumar.gocd.github.json;

import com.google.gson.*;
import in.ashwanthkumar.gocd.github.model.SCMConfig;
import in.ashwanthkumar.utils.collections.Maps;

import java.lang.reflect.Type;
import java.util.Map;

public class SCMConfigSerDe implements JsonSerializer<SCMConfig>, JsonDeserializer<SCMConfig> {
    @Override
    public JsonElement serialize(SCMConfig config, Type type, JsonSerializationContext serializer) {
        return serializer.serialize(Maps.of(config.getName(), config.props()));
    }

    @Override
    public SCMConfig deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        Map.Entry<String, JsonElement> config = json.getAsJsonObject().entrySet().iterator().next();
        JsonObject configValue = config.getValue().getAsJsonObject();

        SCMConfig scmConfig = new SCMConfig();
        scmConfig.name(config.getKey());
        if (configValue.has("default-value"))
            scmConfig.defaultValue(configValue.get("default-value").getAsString());
        if (configValue.has("display-name"))
            scmConfig.displayName(configValue.get("display-name").getAsString());
        if (configValue.has("display-order"))
            scmConfig.displayOrder(configValue.get("display-order").getAsString());
        if (configValue.has("required"))
            scmConfig.required(configValue.get("required").getAsBoolean());
        if (configValue.has("part-of-identity"))
            scmConfig.partOfIdentity(configValue.get("part-of-identity").getAsBoolean());
        if (configValue.has("secure"))
            scmConfig.partOfIdentity(configValue.get("secure").getAsBoolean());

        return scmConfig;
    }
}
