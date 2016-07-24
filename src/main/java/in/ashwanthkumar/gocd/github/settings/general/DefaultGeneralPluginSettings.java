package in.ashwanthkumar.gocd.github.settings.general;

public class DefaultGeneralPluginSettings implements GeneralPluginSettings {

    private String goApiHost;
    private String goApiUsername;
    private String goApiPassword;

    public DefaultGeneralPluginSettings() {
    }

    public DefaultGeneralPluginSettings(String goApiHost, String goApiUsername, String goApiPassword) {
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
