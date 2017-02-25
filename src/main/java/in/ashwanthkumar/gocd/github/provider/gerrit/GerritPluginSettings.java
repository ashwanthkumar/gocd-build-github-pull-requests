package in.ashwanthkumar.gocd.github.provider.gerrit;

import in.ashwanthkumar.gocd.github.settings.general.GeneralPluginSettings;
import in.ashwanthkumar.gocd.github.settings.general.GoApiSettings;

public class GerritPluginSettings implements GoApiSettings, GeneralPluginSettings {

    private String goApiHost;
    private String goApiUsername;
    private String goApiPassword;

    public GerritPluginSettings(String goApiHost, String goApiUsername, String goApiPassword) {
        this.goApiHost = goApiHost;
        this.goApiUsername = goApiUsername;
        this.goApiPassword = goApiPassword;
    }

    @Override
    public String getGoApiHost() {
        return goApiHost;
    }

    @Override
    public String getGoApiUsername() {
        return goApiUsername;
    }

    @Override
    public String getGoApiPassword() {
        return goApiPassword;
    }
}
