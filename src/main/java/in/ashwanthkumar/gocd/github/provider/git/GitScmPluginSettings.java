package in.ashwanthkumar.gocd.github.provider.git;

import in.ashwanthkumar.gocd.github.settings.scm.DefaultScmPluginSettings;

public class GitScmPluginSettings extends DefaultScmPluginSettings {

    private String branchBlacklist;
    private String branchWhitelist;

    public GitScmPluginSettings(String url, String username, String password, String branch, String pipelineName, String branchBlacklist, String branchWhitelist) {
        super(url, username, password, branch, pipelineName);
        this.branchBlacklist = branchBlacklist;
        this.branchWhitelist = branchWhitelist;
    }

    public String getBranchBlacklist() {
        return branchBlacklist;
    }

    public String getBranchWhitelist() {
        return branchWhitelist;
    }
}
