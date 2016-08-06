package in.ashwanthkumar.gocd.github.util;

public class PluginSettings {

    private String goApiHost;
    private String goUsername;
    private String goPassword;

    public PluginSettings() {
    }

    public PluginSettings(String goApiHost, String goUsername, String goPassword) {
        this.goUsername = goUsername;
        this.goPassword = goPassword;
        this.goApiHost = goApiHost;
    }

    public String getGoUsername() {
        return goUsername;
    }

    public void setGoUsername(String goUsername) {
        this.goUsername = goUsername;
    }

    public String getGoPassword() {
        return goPassword;
    }

    public void setGoPassword(String goPassword) {
        this.goPassword = goPassword;
    }

    public String getGoApiHost() {
        return goApiHost;
    }

    public void setGoApiHost(String goApiHost) {
        this.goApiHost = goApiHost;
    }
}
