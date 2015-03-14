package in.ashwanthkumar.gocd.github;

import org.apache.commons.io.IOUtils;
import org.eclipse.jgit.lib.Ref;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import static in.ashwanthkumar.gocd.github.GitConstants.PUBLIC_GITHUB_ENDPOINT;

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

    /**
     * Simple check for SSH form of Git urls
     * - Should have a @ in the url
     * - It should have 2 parts when split by /
     *
     * FIXME - Find a better way to do this?
     */
    public static boolean isValidSSHUrl(String url) {
        return url.contains("@") && url.replaceAll("//", "/").split("/").length == 2;
    }

    public static int prIdFrom(String diffUrl) {
        return Integer.parseInt(diffUrl.substring(diffUrl.indexOf("/pull/") + 6, diffUrl.length() - 5));
    }

    public static int pullRequestIdFromRef(Ref prRef) {
        // generally of the form refs/gh-merge/remotes/origin/3
        String idInString = prRef.getName().split("/")[4];
        return Integer.valueOf(idInString);
    }

    public static GitHub buildGithubFromPropertyFile() throws IOException {
        Properties props = readPropertyFile();
        GitHubBuilder self = new GitHubBuilder();
        self.withOAuthToken(props.getProperty("oauth"), props.getProperty("login"));
        self.withPassword(props.getProperty("login"), props.getProperty("password"));
        // For github enterprise you need to suffix /api/v3
        self.withEndpoint(props.getProperty("endpoint", PUBLIC_GITHUB_ENDPOINT));
        return self.build();
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
