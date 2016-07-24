package in.ashwanthkumar.gocd.github.provider.git;

import com.thoughtworks.go.plugin.api.GoPluginIdentifier;
import com.tw.go.plugin.HelperFactory;
import com.tw.go.plugin.model.GitConfig;
import in.ashwanthkumar.gocd.github.provider.Provider;
import in.ashwanthkumar.gocd.github.settings.general.DefaultGeneralPluginConfigurationView;
import in.ashwanthkumar.gocd.github.settings.general.GeneralPluginConfigurationView;
import in.ashwanthkumar.gocd.github.settings.scm.ScmPluginConfigurationView;
import in.ashwanthkumar.gocd.github.util.URLUtils;

import java.io.File;
import java.util.Arrays;
import java.util.Map;

public class GitProvider implements Provider {
    public static final String REF_SPEC = "+refs/heads/*:refs/remotes/origin/*";
    public static final String REF_PATTERN = "refs/remotes/origin/";

    @Override
    public GoPluginIdentifier getPluginId() {
        return new GoPluginIdentifier("git.fb", Arrays.asList("1.0"));
    }

    @Override
    public String getName() {
        return "Git Feature Branch";
    }

    @Override
    public void addConfigData(GitConfig gitConfig) {
    }

    @Override
    public boolean isValidURL(String url) {
        if (url.startsWith("/")) {
            return new File(url).exists();
        }
        return new URLUtils().isValidURL(url);
    }

    @Override
    public void checkConnection(GitConfig gitConfig) {
        HelperFactory.gitCmd(gitConfig, null).checkConnection();
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
    public void populateRevisionData(GitConfig gitConfig, String branch, String latestSHA, Map<String, String> data) {
        data.put("CURRENT_BRANCH", branch);
    }

    @Override
    public ScmPluginConfigurationView getScmConfigurationView() {
        return new GitScmPluginConfigurationView();
    }

    @Override
    public GeneralPluginConfigurationView getGeneralConfigurationView() {
        return new DefaultGeneralPluginConfigurationView();
    }

}
