package in.ashwanthkumar.gocd.github.util;

import com.tw.go.plugin.GitHelper;
import com.tw.go.plugin.HelperFactory;
import com.tw.go.plugin.model.GitConfig;

import java.io.File;

public class GitFactory {

    public GitHelper create(GitConfig config, File folder) {
        return HelperFactory.gitCmd(config, folder);
    }

}
