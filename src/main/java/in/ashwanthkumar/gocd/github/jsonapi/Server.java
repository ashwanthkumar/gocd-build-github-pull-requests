package in.ashwanthkumar.gocd.github.jsonapi;

import com.thoughtworks.go.plugin.api.logging.Logger;
import in.ashwanthkumar.gocd.github.settings.general.GeneralPluginSettings;
import in.ashwanthkumar.gocd.github.settings.general.GoApiSettings;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import static in.ashwanthkumar.utils.lang.StringUtils.isEmpty;

public class Server {
    private Logger LOG = Logger.getLoggerFor(Server.class);

    private GeneralPluginSettings settings;
    private HttpConnectionUtil httpConnectionUtil;

    /**
     * Construct a new server object, using credentials from PluginSettings.
     */
    public Server(GeneralPluginSettings settings) {
        this.settings = settings;
        httpConnectionUtil = new HttpConnectionUtil();
    }

    Server(GeneralPluginSettings settings, HttpConnectionUtil httpConnectionUtil) {
        this.settings = settings;
        this.httpConnectionUtil = httpConnectionUtil;
    }

    public <T> T getResourceAs(URL url, Class<T> type)
            throws IOException {
        URL normalizedUrl;
        try {
            normalizedUrl = url.toURI().normalize().toURL();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        LOG.info("Fetching " + normalizedUrl.toString());

        HttpURLConnection request = httpConnectionUtil.getConnection(normalizedUrl);

        GoApiSettings goApiSettings = (GoApiSettings) settings;
        final String login = goApiSettings.getGoApiUsername();
        final String password = goApiSettings.getGoApiPassword();
        // Add in our HTTP authorization credentials if we have them.
        if (!isEmpty(login) && !isEmpty(password)) {
            String userpass = login + ":" + password;
            String basicAuth = "Basic "
                    + DatatypeConverter.printBase64Binary(userpass.getBytes());
            request.setRequestProperty("Authorization", basicAuth);
        }

        request.connect();

        return httpConnectionUtil.responseToType(request.getContent(), type);
    }

    public PipelineStatus getPipelineStatus(String pipelineName)
            throws MalformedURLException, IOException {
        GoApiSettings goApiSettings = (GoApiSettings) settings;

        final String apiHost = goApiSettings.getGoApiHost();
        URL url = new URL(String.format("%s/go/api/pipelines/%s/status",
                apiHost, pipelineName));

        LOG.info(String.format("Fetch pipeline %s status from %s", pipelineName, url));
        return getResourceAs(url, PipelineStatus.class);
    }

    public PipelineHistory getPipelineHistory(String pipelineName)
            throws MalformedURLException, IOException {
        GoApiSettings goApiSettings = (GoApiSettings) settings;

        final String apiHost = goApiSettings.getGoApiHost();
        URL url = new URL(String.format("%s/go/api/pipelines/%s/history",
                apiHost, pipelineName));

        LOG.info(String.format("Fetch pipeline %s history from %s", pipelineName, url));
        return getResourceAs(url, PipelineHistory.class);
    }
}
