package in.ashwanthkumar.gocd.github.settings.scm;

import com.tw.go.plugin.model.GitConfig;

public class DefaultScmPluginSettings implements ScmPluginSettings {

    private GitConfig gitConfig;
    private String pipelineName;

    public DefaultScmPluginSettings(String url, String username, String password, String branch, String pipelineName) {
        this.gitConfig = new GitConfig(url, username, password, branch);
        this.pipelineName = pipelineName;
    }

    @Override
    public GitConfig getGitConfig() {
        return gitConfig;
    }

    @Override
    public String getPipelineName() {
        return pipelineName;
    }
}
