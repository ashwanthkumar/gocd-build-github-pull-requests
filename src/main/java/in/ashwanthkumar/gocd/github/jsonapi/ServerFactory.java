package in.ashwanthkumar.gocd.github.jsonapi;

import in.ashwanthkumar.gocd.github.util.PluginSettings;

public class ServerFactory {

    public Server getServer(PluginSettings settings) {
        return new Server(settings);
    }
}
