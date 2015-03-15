package in.ashwanthkumar.gocd.github.provider;

import com.tw.go.plugin.model.GitConfig;

import java.util.Map;

public interface Provider {
    public String getName();

    public void addConfigData(GitConfig gitConfig);

    public boolean isValidURL(String url);

    public void checkConnection(GitConfig gitConfig);

    public String getRefSpec();

    public String getRefPattern();

    public void populateRevisionData(GitConfig gitConfig, String prId, String prSHA, Map<String, String> data);
}
