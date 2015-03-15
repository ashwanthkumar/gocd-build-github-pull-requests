package in.ashwanthkumar.gocd.github.provider.github;

import com.tw.go.plugin.model.GitConfig;
import com.tw.go.plugin.util.StringUtil;
import in.ashwanthkumar.gocd.github.provider.Provider;
import in.ashwanthkumar.gocd.github.provider.github.model.PullRequestStatus;
import in.ashwanthkumar.utils.func.Function;
import org.apache.commons.validator.routines.UrlValidator;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.GitHub;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

public class GitHubProvider implements Provider {
    public static final String PR_FETCH_REFSPEC = "+refs/pull/*/merge:refs/gh-merge/remotes/origin/*";
    public static final String PR_MERGE_PREFIX = "refs/gh-merge/remotes/origin/";
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
        return new UrlValidator(UrlValidator.ALLOW_LOCAL_URLS).isValid(url) || GHUtils.isValidSSHUrl(url);
    }

    @Override
    public void checkConnection(GitConfig gitConfig) {
        try {
            GitHub.connect().getRepository(GHUtils.parseGithubUrl(gitConfig.getEffectiveUrl()));
        } catch (Exception e) {
            throw new RuntimeException(String.format("check connection failed. %s", e.getMessage()), e);
        }
    }

    @Override
    public String getRefSpec() {
        return PR_FETCH_REFSPEC;
    }

    @Override
    public String getRefPattern() {
        return PR_MERGE_PREFIX;
    }

    @Override
    public void populateRevisionData(GitConfig gitConfig, String prId, String prSHA, Map<String, String> data) {
        PullRequestStatus prStatus = null;
        try {
            GHPullRequest currentPR = pullRequestFrom(gitConfig, Integer.parseInt(prId));
            prStatus = transformGHPullRequestToPullRequestStatus(prSHA).apply(currentPR);
        } catch (Exception e) {
            // ignore
        }

        if (prStatus != null) {
            data.put("PR_ID", String.valueOf(prStatus.getId()));
            data.put("PR_BRANCH", String.valueOf(prStatus.getPrBranch()));
            data.put("TARGET_BRANCH", String.valueOf(prStatus.getToBranch()));
            data.put("PR_URL", String.valueOf(prStatus.getUrl()));
            data.put("PR_AUTHOR", prStatus.getAuthor());
            data.put("PR_AUTHOR_EMAIL", prStatus.getAuthorEmail());
            data.put("PR_DESCRIPTION", prStatus.getDescription());
            data.put("PR_TITLE", prStatus.getTitle());
        }
    }

    private GHPullRequest pullRequestFrom(GitConfig gitConfig, int currentPullRequestID) throws IOException {
        return GHUtils.buildGithubFromPropertyFile()
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
}
