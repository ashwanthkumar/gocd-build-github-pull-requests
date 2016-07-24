package in.ashwanthkumar.gocd.github.provider;

import com.thoughtworks.go.plugin.api.GoPluginIdentifier;
import com.tw.go.plugin.model.GitConfig;
import in.ashwanthkumar.gocd.github.settings.general.GeneralPluginConfigurationView;
import in.ashwanthkumar.gocd.github.settings.scm.ScmPluginConfigurationView;

import java.util.Map;

public interface Provider {
    public GoPluginIdentifier getPluginId();

    public String getName();

    public void addConfigData(GitConfig gitConfig);

    public boolean isValidURL(String url);

    public void checkConnection(GitConfig gitConfig);

    public String getRefSpec();

    public String getRefPattern();

    public void populateRevisionData(GitConfig gitConfig, String prId, String prSHA, Map<String, String> data);

    public ScmPluginConfigurationView getScmConfigurationView();

    public GeneralPluginConfigurationView getGeneralConfigurationView();
}
