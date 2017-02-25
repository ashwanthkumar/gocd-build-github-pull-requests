package in.ashwanthkumar.gocd.github.settings.scm;

import com.tw.go.plugin.model.GitConfig;

public interface ScmPluginSettings {

    GitConfig getGitConfig();

    String getPipelineName();

}
