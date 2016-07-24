package in.ashwanthkumar.gocd.github.provider.gerrit;

import in.ashwanthkumar.gocd.github.settings.general.DefaultGeneralPluginSettings;
import in.ashwanthkumar.gocd.github.settings.general.GeneralPluginConfigurationView;
import in.ashwanthkumar.gocd.github.settings.general.GeneralPluginSettings;
import in.ashwanthkumar.gocd.github.util.FieldFactory;

import java.util.HashMap;
import java.util.Map;

public class GerritPluginConfigurationView implements GeneralPluginConfigurationView {


    @Override
    public String templateName() {
        return "/views/plugin.template.html";
    }

    @Override
    public Map<String, Object> fields() {
        Map<String, Object> response = new HashMap<String, Object>();
        response.put("go_api_host", FieldFactory.createForGeneral("Go API Host", null, false, false, "0"));
        response.put("go_api_username", FieldFactory.createForGeneral("Go Username", null, false, false, "1"));
        response.put("go_api_password", FieldFactory.createForGeneral("Go Password", null, false, true, "2"));
        return response;
    }

    @Override
    public boolean hasConfigurationView() {
        return true;
    }

    public GeneralPluginSettings getSettings(Map<String, Object> rawSettings) {
        GeneralPluginSettings settings = new DefaultGeneralPluginSettings(
                (String)rawSettings.get("go_api_host"),
                (String)rawSettings.get("go_api_username"),
                (String)rawSettings.get("go_api_password")
        );

        return settings;
    }
}
