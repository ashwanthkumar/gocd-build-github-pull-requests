package in.ashwanthkumar.gocd.github.provider.gitlab;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class GitLabUtils {
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

    public static String parseGitlabUrl(String url) {
        String[] urlParts = url.split("/");
        String repo = urlParts[urlParts.length - 1];
        String usernameWithSSHPrefix = urlParts[urlParts.length - 2];
        int positionOfColon = usernameWithSSHPrefix.lastIndexOf(":");
        if (positionOfColon > 0) {
            usernameWithSSHPrefix = usernameWithSSHPrefix.substring(positionOfColon + 1);
        }

        String urlWithoutPrefix = String.format("%s/%s", usernameWithSSHPrefix, repo);
        if (urlWithoutPrefix.endsWith(".git")) return urlWithoutPrefix.substring(0, urlWithoutPrefix.length() - 4);
        else return urlWithoutPrefix;
    }
}
