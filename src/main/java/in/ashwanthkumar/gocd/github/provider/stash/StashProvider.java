package in.ashwanthkumar.gocd.github.provider.stash;

import com.tw.go.plugin.HelperFactory;
import com.tw.go.plugin.model.GitConfig;
import in.ashwanthkumar.gocd.github.provider.Provider;
import org.apache.commons.validator.routines.UrlValidator;

import java.util.Map;

public class StashProvider implements Provider {
    public static final String REF_SPEC = "+refs/pull-requests/*/from:refs/remotes/origin/pull-request/*";
    public static final String REF_PATTERN = "refs/remotes/origin/pull-request/";

    @Override
    public String getName() {
        return "Stash";
    }

    @Override
    public void addConfigData(GitConfig gitConfig) {
    }

    @Override
    public boolean isValidURL(String url) {
        return new UrlValidator(UrlValidator.ALLOW_LOCAL_URLS).isValid(url);
    }

    @Override
    public void checkConnection(GitConfig gitConfig) {
        HelperFactory.git(gitConfig, null).checkConnection();
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
    }
}
