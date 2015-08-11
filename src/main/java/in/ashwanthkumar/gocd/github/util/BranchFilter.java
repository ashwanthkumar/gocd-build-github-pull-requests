package in.ashwanthkumar.gocd.github.util;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.HashSet;
import java.util.Set;

public class BranchFilter {

    public static final String SEPARATOR = ",(?![^{]*\\})";

    private final Set<PathMatcher> blacklistedBranches = new HashSet<>();

    public BranchFilter(String blacklistOption) {
        if (blacklistOption != null) {
            for (String branch : blacklistOption.split(SEPARATOR)) {
                PathMatcher matcher = getMatcher(branch.trim());
                blacklistedBranches.add(matcher);
            }
        }
    }

    private PathMatcher getMatcher(String branch) {
        return FileSystems.getDefault().getPathMatcher(String.format("glob:%s", branch));
    }

    private Path getAsPath(String branch) {
        return FileSystems.getDefault().getPath(branch);
    }

    public boolean isBranchBlacklisted(String branch) {
        if (blacklistedBranches.isEmpty()) {
            return false;
        }

        Path branchAsPath = getAsPath(branch);
        for (PathMatcher m : blacklistedBranches) {
            if (m.matches(branchAsPath)) {
                return true;
            }
        }
        return false;
    }

}
