package in.ashwanthkumar.gocd.github.model;

import in.ashwanthkumar.utils.collections.Iterables;
import in.ashwanthkumar.utils.collections.Lists;
import in.ashwanthkumar.utils.func.Predicate;
import in.ashwanthkumar.utils.lang.option.Option;
import org.eclipse.jgit.lib.Ref;

import java.util.ArrayList;
import java.util.List;

import static in.ashwanthkumar.gocd.github.GHUtils.pullRequestIdFromRef;

public class MergeRefs {
    private List<Ref> mergeRefs = new ArrayList<Ref>();

    public MergeRefs(List<Ref> mergeRefs) {
        this.mergeRefs = mergeRefs;
    }

    public boolean isEmpty() {
        return mergeRefs.isEmpty();
    }

    public boolean nonEmpty() {
        return !isEmpty();
    }

    public Ref head() {
        return Iterables.head(mergeRefs);
    }

    public Option<Ref> headOption() {
        return Iterables.headOption(mergeRefs);
    }

    public boolean hasPullRequest(final int pullRequestId) {
        return Iterables.exists(mergeRefs, new Predicate<Ref>() {
            @Override
            public Boolean apply(Ref input) {
                // ref pattern are of the form "refs/gh-merge/remotes/origin/3"
                return input.getName().endsWith(String.valueOf(pullRequestId));
            }
        });
    }

    public Option<Ref> findNotProcessed(final PullRequests existingPullRequestsInfo) {
        return Lists.find(mergeRefs, new Predicate<Ref>() {
            @Override
            public Boolean apply(Ref input) {
                int pullRequestId = pullRequestIdFromRef(input);
                return existingPullRequestsInfo.hasNotId(pullRequestId) || existingPullRequestsInfo.hasChanged(pullRequestId, input.getObjectId().name());
            }
        });
    }
}
