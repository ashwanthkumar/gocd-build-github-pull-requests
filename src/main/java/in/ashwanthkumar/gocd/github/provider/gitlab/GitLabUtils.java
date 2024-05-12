package in.ashwanthkumar.gocd.github.provider.gitlab;

import in.ashwanthkumar.utils.lang.StringUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;

public class GitLabUtils {
    public static final String HTTP_PROTOCOL = "http";
    public static final String HTTPS_PROTOCOL = "https";
    public static final String URL_SEPARATOR = "/";
    public static final String GIT_URL_EXTENSION = ".git";
    public static final int GIT_URL_EXTENSION_LENGTH = 4;
    /**
     * After http url split with url separator a simple project will have no more than 5 splits
     */
    public static final int SIMPLE_PROJECT_HTTP_URL_PART_SIZE = 5;
    /**
     * After http url split index value where we find the first part of repository path
     */
    public static final int URL_WITH_PROTOCOL_START_INDEX = 3;
    public static final String AT_CHARACTER = "@";
    public static final int REPOSITORY_URL_INDEX = 1;
    public static final String COLUMN = ":";
    public static final int REPOSITORY_PATH_INDEX = 1;
    public static final int URL_WITHOUT_PROTOCOL_START_INDEX = 1;

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

    private static boolean isAuthorityPresentInURI(String url) {
        return url.contains(AT_CHARACTER) ;
    }
    /**
     * Will extract the project path from the repository url.
     * @param url
     * @return
     */
    public static String getProjectPathFromUrl(String url) {
        String normalizedUrl = getNormalizedUrl(url);
        String projectPath = "";
        if (isAuthorityPresentInURI(url)) {
            projectPath = getProjectPathFromUriWithAuthorithy(normalizedUrl);
        } else {
            projectPath = getProjectPathFromUriWithoutAuthorithy(normalizedUrl,URL_WITH_PROTOCOL_START_INDEX);
        }
        return removeGitExtension(projectPath);
    }

    private static String getNormalizedUrl(String url) {
        url = StringUtils.trim(url);
        if(url.endsWith(URL_SEPARATOR)) {
            url = url.substring(0, url.length() - 1);
        }
        return url.toLowerCase();
    }

    private static String getProjectPathFromUriWithAuthorithy(String normalizedUrl) {
        String projectPath = "";
        String repositoryUrlWithoutAuthority = normalizedUrl.split(AT_CHARACTER)[REPOSITORY_URL_INDEX];
        if(repositoryUrlWithoutAuthority.contains(COLUMN)) {
            projectPath = repositoryUrlWithoutAuthority.split(COLUMN)[REPOSITORY_PATH_INDEX];
        } else {
            projectPath = getProjectPathFromUriWithoutAuthorithy(repositoryUrlWithoutAuthority, URL_WITHOUT_PROTOCOL_START_INDEX);
        }
        return projectPath;
    }

    private static String removeGitExtension(String projectPath) {
        if (projectPath.endsWith(GIT_URL_EXTENSION)) return projectPath.substring(0, projectPath.length() - GIT_URL_EXTENSION_LENGTH);
        else return projectPath;
    }

    private static String getProjectPathFromUriWithoutAuthorithy(String normalizedUrl, int startIndex) {
        String[] urlParts = normalizedUrl.split(URL_SEPARATOR);
        return String.join(URL_SEPARATOR, Arrays.copyOfRange(urlParts, startIndex,urlParts.length));
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
