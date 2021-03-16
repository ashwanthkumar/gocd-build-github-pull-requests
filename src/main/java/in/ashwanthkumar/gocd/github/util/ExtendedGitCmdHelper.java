package in.ashwanthkumar.gocd.github.util;

import java.io.File;

import com.tw.go.plugin.cmd.Console;
import com.tw.go.plugin.cmd.ProcessOutputStreamConsumer;
import com.tw.go.plugin.git.GitCmdHelper;
import com.tw.go.plugin.model.GitConfig;
import org.apache.commons.exec.CommandLine;

public class ExtendedGitCmdHelper extends GitCmdHelper {

    public ExtendedGitCmdHelper(GitConfig gitConfig, File workingDir) {
        super(gitConfig, workingDir);
    }

    public ExtendedGitCmdHelper(GitConfig gitConfig, File workingDir, ProcessOutputStreamConsumer stdOut,
            ProcessOutputStreamConsumer stdErr) {
        super(gitConfig, workingDir, stdOut, stdErr);
    }

    public void checkoutNewBranch(String branchName) {
        CommandLine gitCheckout = Console.createCommand("checkout", "-B", branchName);
        Console.runOrBomb(gitCheckout, workingDir, stdOut, stdErr);
    }
}
