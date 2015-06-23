package in.ashwanthkumar.gocd.github.provider.github;

import org.apache.commons.io.IOUtils;
import org.eclipse.jgit.lib.Ref;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class GHUtils {
    /**
     * General convention is
     *  === Public Github URL ===
     * https://github.com/ashwanthkumar/gocd-build-github-pull-requests.git
     * git@github.com:ashwanthkumar/gocd-build-github-pull-requests.git
     *
     *  === Github Enterprise URL ===
     * http://code.corp.yourcompany.com/username/repo
     * git@code.corp.yourcompany.com/username/repo
     */
    public static String parseGithubUrl(String url) {
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

    public static int prIdFrom(String diffUrl) {
        return Integer.parseInt(diffUrl.substring(diffUrl.indexOf("/pull/") + 6, diffUrl.length() - 5));
    }

    public static Properties readPropertyFile() throws IOException {
        File propertyFile = new File(System.getProperty("user.home"), ".github");
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
}
