package in.ashwanthkumar.gocd.github.util;

public class BranchFilter {

    private final BranchMatcher blacklistedBranches;
    private final BranchMatcher whitelistedBrandches;

    public BranchFilter(String blacklistOption, String whitelistOption) {
        this.blacklistedBranches = new BranchMatcher(blacklistOption);
        this.whitelistedBrandches = new BranchMatcher(whitelistOption);
    }

    public boolean isBranchValid(String branch) {
        if (branch == null) {
            return false;
        } else if (whitelistedBrandches.isEmpty() && blacklistedBranches.isEmpty()) {
            return true;
        } else {
            return whitelistedBrandches.matches(branch) && !blacklistedBranches.matches(branch);
        }
    }

}