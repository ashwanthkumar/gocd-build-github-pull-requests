package in.ashwanthkumar.gocd.github.util;

public class BranchFilter {

    public static final String NO_BRANCHES = "";
    private final BranchMatcher blacklistedBranches;
    private final BranchMatcher whitelistedBrandches;

    public BranchFilter() {
        this(NO_BRANCHES, NO_BRANCHES);
    }

    public BranchFilter(String blacklistOption, String whitelistOption) {
        this.blacklistedBranches = new BranchMatcher(blacklistOption, BranchMatcher.Mode.FAIL_EMPTY);
        this.whitelistedBrandches = new BranchMatcher(whitelistOption, BranchMatcher.Mode.PASS_EMPTY);
    }

    public boolean isBranchValid(String branch) {
        if (branch == null) {
            return false;
        } else if (whitelistedBrandches.isEmpty() && blacklistedBranches.isEmpty()) {
            return true;
        } else if (whitelistedBrandches.matches(branch) && !blacklistedBranches.matches(branch)) {
            return true;
        } else {
            return false;
        }
    }

}
