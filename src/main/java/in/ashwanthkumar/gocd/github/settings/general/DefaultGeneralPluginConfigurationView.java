package in.ashwanthkumar.gocd.github.settings.general;

import java.util.HashMap;
import java.util.Map;

public class DefaultGeneralPluginConfigurationView implements GeneralPluginConfigurationView {


    @Override
    public String templateName() {
        return "";
    }

    @Override
    public Map<String, Object> fields() {
        Map<String, Object> response = new HashMap<String, Object>();
        return response;
    }

    @Override
    public boolean hasConfigurationView() {
        return false;
    }
}
