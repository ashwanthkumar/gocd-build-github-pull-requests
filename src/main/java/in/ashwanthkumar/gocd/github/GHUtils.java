package in.ashwanthkumar.gocd.github;

public class GHUtils {
    public static String parseGithubUrl(String url) {
        String urlInLowerCase = url.toLowerCase();
        String urlWithoutPrefix = urlInLowerCase;
        if (urlInLowerCase.startsWith("git@github.com:")) {
            urlWithoutPrefix = urlInLowerCase.replaceAll("git@github.com:", "");
        } else if (urlInLowerCase.startsWith("https://github.com/")) {
            urlWithoutPrefix = urlInLowerCase.replaceAll("https://github.com/", "");
        }

        if (urlWithoutPrefix.endsWith(".git")) return urlWithoutPrefix.substring(0, urlWithoutPrefix.length() - 4);
        else return urlWithoutPrefix;
    }

    public static boolean isValidGHUrl(String url) {
        return url.toLowerCase().startsWith("https://github.com/") || url.toLowerCase().startsWith("git@github.com:");
    }

    public static int prIdFrom(String diffUrl) {
        return Integer.parseInt(diffUrl.substring(diffUrl.indexOf("/pull/") + 6, diffUrl.length() - 5));
    }
}
