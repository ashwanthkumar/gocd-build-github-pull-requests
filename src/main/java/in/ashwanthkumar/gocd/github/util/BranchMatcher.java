package in.ashwanthkumar.gocd.github.util;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.List;

public class BranchMatcher {

    public static final String SEPARATOR = ",(?![^{]*\\})";

    public enum Mode {
        PASS_EMPTY,
        FAIL_EMPTY
    }

    private String pattern;
    private Mode mode;
    private List<PathMatcher> patterns = new ArrayList<>();

    public BranchMatcher(String branchPattern, Mode mode) {
        if (branchPattern != null) {
            pattern = branchPattern;
            for (String branch : branchPattern.split(SEPARATOR)) {
                PathMatcher matcher = getMatcher(branch.trim());
                if (!branch.trim().isEmpty()) {
                    patterns.add(matcher);
                }
            }
        }
        this.mode = mode;
    }

    public boolean isEmpty() {
        return patterns.isEmpty();
    }

    public boolean matches(String branch) {
        if (patterns.isEmpty()) {
            return mode == Mode.PASS_EMPTY;
        }

        Path branchAsPath = getAsPath(branch);
        for (PathMatcher pathMatcher : patterns) {
            if (pathMatcher.matches(branchAsPath)) {
                return true;
            }
        }
        return false;
    }

    private PathMatcher getMatcher(String branch) {
        return FileSystems.getDefault().getPathMatcher(String.format("glob:%s", branch));
    }

    private Path getAsPath(String branch) {
        return FileSystems.getDefault().getPath(branch);
    }

}
