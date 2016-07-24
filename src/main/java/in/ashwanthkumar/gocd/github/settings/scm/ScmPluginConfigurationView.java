package in.ashwanthkumar.gocd.github.settings.scm;

import in.ashwanthkumar.gocd.github.util.BranchFilter;

import java.util.Map;

public interface ScmPluginConfigurationView extends PluginConfigurationView {

    BranchFilter getBranchFilter(ScmPluginSettings scmSettings);

    ScmPluginSettings getSettings(Map<String, String> rawSettings);
}
