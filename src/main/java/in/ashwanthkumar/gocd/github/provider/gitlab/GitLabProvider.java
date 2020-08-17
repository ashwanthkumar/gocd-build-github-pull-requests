package in.ashwanthkumar.gocd.github.provider.gitlab;

import com.thoughtworks.go.plugin.api.GoPluginIdentifier;
import com.tw.go.plugin.model.GitConfig;
import com.tw.go.plugin.util.StringUtil;
import in.ashwanthkumar.gocd.github.provider.Provider;
import in.ashwanthkumar.gocd.github.provider.github.model.PullRequestStatus;
import in.ashwanthkumar.gocd.github.settings.general.DefaultGeneralPluginConfigurationView;
import in.ashwanthkumar.gocd.github.settings.general.GeneralPluginConfigurationView;
import in.ashwanthkumar.gocd.github.settings.scm.DefaultScmPluginConfigurationView;
import in.ashwanthkumar.gocd.github.settings.scm.ScmPluginConfigurationView;
import in.ashwanthkumar.gocd.github.util.URLUtils;
import in.ashwanthkumar.utils.func.Function;
import in.ashwanthkumar.utils.lang.StringUtils;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.Author;
import org.gitlab4j.api.models.MergeRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

public class GitLabProvider implements Provider {
    private static final Logger LOG = LoggerFactory.getLogger(GitLabProvider.class);
    public static final String REF_SPEC = "+refs/merge-requests/*/head:refs/remotes/origin/merge-requests/*";
    public static final String REF_PATTERN = "refs/remotes/origin/merge-requests/";

    @Override
    public GoPluginIdentifier getPluginId() {
        return new GoPluginIdentifier("gitlab.pr", Arrays.asList("1.0"));
    }

    @Override
    public String getName() {
        return "Gitlab";
    }

    @Override
    public void addConfigData(GitConfig gitConfig) {
        try {
            Properties props = GitLabUtils.readPropertyFile();
            if (StringUtil.isEmpty(gitConfig.getUsername())) {
                gitConfig.setUsername(props.getProperty("login"));
            } if (StringUtil.isEmpty(gitConfig.getPassword())) {
                gitConfig.setPassword(props.getProperty("password"));
            }
        } catch (IOException e) {
            // ignore
        }
    }

    @Override
    public boolean isValidURL(String url) {
        return new URLUtils().isValidURL(url);
    }

    @Override
    public void checkConnection(GitConfig gitConfig) {
        try {
            Objects.requireNonNull(loginWith(gitConfig)).getProjectApi().getProjects();
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
        boolean isDisabled = System.getProperty("go.plugin.gitlab.pr.populate-details", "Y").equals("N");
        LOG.debug("Populating PR details is disabled");
        if (!isDisabled) {
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

    @Override
    public ScmPluginConfigurationView getScmConfigurationView() {
        return new DefaultScmPluginConfigurationView();
    }

    @Override
    public GeneralPluginConfigurationView getGeneralConfigurationView() {
        return new DefaultGeneralPluginConfigurationView();
    }

    private PullRequestStatus getPullRequestStatus(GitConfig gitConfig, String prId, String prSHA) {
        try {
            MergeRequest currentPR = pullRequestFrom(gitConfig, Integer.parseInt(prId));
            return transformMergeRequestToPullRequestStatus(prSHA).apply(currentPR);
        } catch (Exception e) {
            // ignore
            LOG.warn(e.getMessage(), e);
        } return null;
    }

    private MergeRequest pullRequestFrom(GitConfig gitConfig, int currentPullRequestID) throws GitLabApiException,
            NullPointerException {
        return Objects.requireNonNull(loginWith(gitConfig))
                .getMergeRequestApi()
                .getMergeRequest(GitLabUtils.parseGitlabUrl(gitConfig.getEffectiveUrl()), currentPullRequestID);
    }

    private Function<MergeRequest, PullRequestStatus> transformMergeRequestToPullRequestStatus(final String mergedSHA) {
        return new Function<MergeRequest, PullRequestStatus>() {
            @Override
            public PullRequestStatus apply(MergeRequest input) {
                int prID = input.getId();
                Author user = input.getAuthor();
                return new PullRequestStatus(prID, GitLabProvider.REF_PATTERN, input.getSha(), mergedSHA,
                        input.getSourceBranch(), input.getTargetBranch(), input.getWebUrl(), user.getName(),
                        user.getEmail(), input.getDescription(), input.getTitle());
            }
        };
    }

    private GitLabApi loginWith(GitConfig gitConfig) throws GitLabApiException {
        if (hasCredentials(gitConfig))
            return GitLabApi.oauth2Login(gitConfig.getUrl(), gitConfig.getUsername(), gitConfig.getPassword());
        else {
            LOG.warn("No gitlab credentials found!");
            return null;
        }
    }

    private boolean hasCredentials(GitConfig gitConfig) {
        return StringUtils.isNotEmpty(gitConfig.getUsername()) && StringUtils.isNotEmpty(gitConfig.getPassword());
    }
}
