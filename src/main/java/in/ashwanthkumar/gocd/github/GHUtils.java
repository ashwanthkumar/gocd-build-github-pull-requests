package in.ashwanthkumar.gocd.github;

public class GHUtils {
    public static String parseGithubUrl(String url) {
        String urlWithoutPrefix = url;
        if (url.startsWith("git@github.com:")) {
            urlWithoutPrefix = url.replaceAll("git@github.com:", "");
        } else if (url.startsWith("https://github.com/")) {
            urlWithoutPrefix = url.replaceAll("https://github.com/", "");
        }

        if (urlWithoutPrefix.endsWith(".git")) return urlWithoutPrefix.substring(0, urlWithoutPrefix.length() - 4);
        else return urlWithoutPrefix;
    }

    public static boolean isValidGHUrl(String url) {
        return url.startsWith("https://github.com/") || url.startsWith("git@github.com:");
    }

    public static int prIdFrom(String diffUrl) {
        return Integer.parseInt(diffUrl.substring(diffUrl.indexOf("/pull/") + 6, diffUrl.length() - 5));
    }
}
