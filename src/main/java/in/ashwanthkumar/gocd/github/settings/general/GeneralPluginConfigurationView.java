package in.ashwanthkumar.gocd.github.settings.general;

import in.ashwanthkumar.gocd.github.settings.scm.PluginConfigurationView;

import java.util.Map;

public interface GeneralPluginConfigurationView extends PluginConfigurationView {

    GeneralPluginSettings getSettings(Map<String, Object> rawSettings);

}
