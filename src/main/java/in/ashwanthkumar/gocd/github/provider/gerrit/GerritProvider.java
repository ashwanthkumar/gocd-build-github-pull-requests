package in.ashwanthkumar.gocd.github.provider.gerrit;

import com.tw.go.plugin.HelperFactory;
import com.tw.go.plugin.model.GitConfig;
import in.ashwanthkumar.gocd.github.provider.Provider;
import in.ashwanthkumar.gocd.github.util.URLUtils;

import java.util.Map;

public class GerritProvider implements Provider {
    public static final String REF_SPEC = "refs/changes/*:refs/changes/*";
    public static final String REF_PATTERN = "refs/changes/";

    @Override
    public String getName() {
        return "Gerrit";
    }

    @Override
    public void addConfigData(GitConfig gitConfig) {
    }

    @Override
    public boolean isValidURL(String url) {
        return new URLUtils().isValidURL(url);
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
    public void populateRevisionData(GitConfig gitConfig, String changeId, String latestSHA, Map<String, String> data) {
        data.put("CHANGE_SET_ID", changeId);
    }
}
