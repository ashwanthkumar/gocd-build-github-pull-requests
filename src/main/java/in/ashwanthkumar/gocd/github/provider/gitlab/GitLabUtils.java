package in.ashwanthkumar.gocd.github.provider.gitlab;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class GitLabUtils {
    public static final String HTTP_PROTOCOL = "http";
    public static final String HTTPS_PROTOCOL = "https";
    public static final String URL_SEPARATOR = "/";

    public static Properties readPropertyFile() throws IOException {
        File propertyFile = new File(System.getProperty("user.home"), ".gitlab");
        Properties props = new Properties();
        FileInputStream in = null;
        try {
            in = new FileInputStream(propertyFile);
            props.load(in);
        } finally {
            IOUtils.closeQuietly(in);
        }
        return props;
    }

    public static Boolean isSSHUrl(String url) {
        return url.startsWith("git@");
    }

    public static String getProtocol(String url) {
        if(url.startsWith(HTTPS_PROTOCOL) || isSSHUrl(url)) {
            return HTTPS_PROTOCOL;
        } else {
            return HTTP_PROTOCOL;
        }
    }

    public static String getServerUrl(String url) {
        if(isSSHUrl(url)) {
            return String.format("%s://%s", getProtocol(url), url.split(":")[0].substring(4));
        } else {
            String[] urlParts = url.split(URL_SEPARATOR);
            return String.format("%s://%s", getProtocol(url), urlParts[2]);
        }
    }
}
