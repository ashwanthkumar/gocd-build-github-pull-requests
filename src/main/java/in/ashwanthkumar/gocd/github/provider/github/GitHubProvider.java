package in.ashwanthkumar.gocd.github.provider.github;

import com.tw.go.plugin.model.GitConfig;
import com.tw.go.plugin.util.StringUtil;
import in.ashwanthkumar.gocd.github.provider.Provider;
import in.ashwanthkumar.gocd.github.provider.github.model.PullRequestStatus;
import in.ashwanthkumar.gocd.github.util.URLUtils;
import in.ashwanthkumar.utils.func.Function;
import in.ashwanthkumar.utils.lang.StringUtils;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.GitHub;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

public class GitHubProvider implements Provider {
    // public static final String PR_FETCH_REFSPEC = "+refs/pull/*/merge:refs/gh-merge/remotes/origin/*";
    // public static final String PR_MERGE_PREFIX = "refs/gh-merge/remotes/origin/";
    public static final String REF_SPEC = "+refs/pull/*/head:refs/remotes/origin/pull-request/*";
    public static final String REF_PATTERN = "refs/remotes/origin/pull-request/";
    public static final String PUBLIC_GITHUB_ENDPOINT = "https://api.github.com";

    @Override
    public String getName() {
        return "Github";
    }

    @Override
    public void addConfigData(GitConfig gitConfig) {
        try {
            Properties props = GHUtils.readPropertyFile();
            if (StringUtil.isEmpty(gitConfig.getUsername())) {
                gitConfig.setUsername(props.getProperty("login"));
            }
            if (StringUtil.isEmpty(gitConfig.getPassword())) {
                gitConfig.setPassword(props.getProperty("password"));
            }
        } catch (IOException e) {
            // ignore
        }
    }

    @Override
    public boolean isValidURL(String url) {
        if (StringUtil.isEmpty(url))
            return false;
        return new URLUtils().isValidURL(url) || GHUtils.isValidSSHUrl(url);
    }

    @Override
    public void checkConnection(GitConfig gitConfig) {
        try {
            loginWith(gitConfig).getRepository(GHUtils.parseGithubUrl(gitConfig.getEffectiveUrl()));
        } catch (Exception e) {
            throw new RuntimeException(String.format("check connection failed. %s", e.getMessage()), e);
        }
    }

    @Override
    public String getRefSpec() {
        return REF_SPEC;
    }

    @Override
    public String getRefPattern() {
        return REF_PATTERN;
    }

    @Override
    public void populateRevisionData(GitConfig gitConfig, String prId, String prSHA, Map<String, String> data) {
        data.put("PR_ID", prId);

        PullRequestStatus prStatus = null;
        if (!System.getProperty("go.plugin.github.pr.populate-details", "Y").equals("N")) {
            prStatus = getPullRequestStatus(gitConfig, prId, prSHA);
        }

        if (prStatus != null) {
            data.put("PR_BRANCH", String.valueOf(prStatus.getPrBranch()));
            data.put("TARGET_BRANCH", String.valueOf(prStatus.getToBranch()));
            data.put("PR_URL", String.valueOf(prStatus.getUrl()));
            data.put("PR_AUTHOR", prStatus.getAuthor());
            data.put("PR_AUTHOR_EMAIL", prStatus.getAuthorEmail());
            data.put("PR_DESCRIPTION", prStatus.getDescription());
            data.put("PR_TITLE", prStatus.getTitle());
        }
    }

    private PullRequestStatus getPullRequestStatus(GitConfig gitConfig, String prId, String prSHA) {
        try {
            GHPullRequest currentPR = pullRequestFrom(gitConfig, Integer.parseInt(prId));
            return transformGHPullRequestToPullRequestStatus(prSHA).apply(currentPR);
        } catch (Exception e) {
            // ignore
        }
        return null;
    }

    private GHPullRequest pullRequestFrom(GitConfig gitConfig, int currentPullRequestID) throws IOException {
        return loginWith(gitConfig)
                .getRepository(GHUtils.parseGithubUrl(gitConfig.getEffectiveUrl()))
                .getPullRequest(currentPullRequestID);
    }

    private Function<GHPullRequest, PullRequestStatus> transformGHPullRequestToPullRequestStatus(final String mergedSHA) {
        return new Function<GHPullRequest, PullRequestStatus>() {
            @Override
            public PullRequestStatus apply(GHPullRequest input) {
                int prID = GHUtils.prIdFrom(input.getDiffUrl().toString());
                try {
                    GHUser user = input.getUser();
                    return new PullRequestStatus(prID, input.getHead().getSha(), mergedSHA, input.getHead().getLabel(),
                            input.getBase().getLabel(), input.getHtmlUrl().toString(), user.getName(),
                            user.getEmail(), input.getBody(), input.getTitle());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    private GitHub loginWith(GitConfig gitConfig) throws IOException {
        if(hasCredentials(gitConfig)) return GitHub.connectUsingPassword(gitConfig.getUsername(), gitConfig.getPassword());
        else return GitHub.connect();
    }

    private boolean hasCredentials(GitConfig gitConfig) {
        return StringUtils.isNotEmpty(gitConfig.getUsername()) && StringUtils.isNotEmpty(gitConfig.getPassword());
    }
}
