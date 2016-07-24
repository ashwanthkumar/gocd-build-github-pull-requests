package in.ashwanthkumar.gocd.github.jsonapi;

import in.ashwanthkumar.gocd.github.settings.general.GeneralPluginSettings;

public class ServerFactory {

    public Server getServer(GeneralPluginSettings settings) {
        return new Server(settings);
    }
}
