package in.ashwanthkumar.gocd.github.model;

import in.ashwanthkumar.utils.collections.Iterables;
import in.ashwanthkumar.utils.collections.Lists;
import in.ashwanthkumar.utils.func.Function;
import in.ashwanthkumar.utils.func.Predicate;
import in.ashwanthkumar.utils.lang.option.Option;
import in.ashwanthkumar.utils.lang.tuple.Tuple2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static in.ashwanthkumar.utils.collections.Iterables.toMap;
import static in.ashwanthkumar.utils.collections.Lists.filter;
import static in.ashwanthkumar.utils.collections.Lists.map;
import static in.ashwanthkumar.utils.lang.option.Option.option;
import static in.ashwanthkumar.utils.lang.tuple.Tuple2.tuple2;

public class PullRequests {
    private Map<Integer, PullRequestStatus> pullRequestStatuses;

    public Iterable<PullRequestStatus> getPullRequestStatuses() {
        return pullRequestStatuses.values();
    }

    public PullRequests setPullRequestStatuses(List<PullRequestStatus> pullRequestStatuses) {
        this.pullRequestStatuses = toMap(map(pullRequestStatuses, new Function<PullRequestStatus, Tuple2<Integer, PullRequestStatus>>() {
            @Override
            public Tuple2<Integer, PullRequestStatus> apply(PullRequestStatus input) {
                return tuple2(input.getId(), input);
            }
        }));
        return this;
    }

    public PullRequests mergeWith(PullRequestStatus currentPullRequest, final MergeRefs mergeRefs) {
        PullRequests newPullRequests = new PullRequests();
        List<PullRequestStatus> prs = Lists.Nil();
        if (hasId(currentPullRequest.getId())) {
            prs.add(pullRequestStatuses.get(currentPullRequest.getId()).merge(currentPullRequest));
        } else {
            prs.add(currentPullRequest);
        }

        List<PullRequestStatus> purgedPRs = filter(concat(pullRequestStatuses.values(), prs), new Predicate<PullRequestStatus>() {
            @Override
            public Boolean apply(PullRequestStatus input) {
                return mergeRefs.hasPullRequest(input.getId());
            }
        });
        newPullRequests.setPullRequestStatuses(purgedPRs);
        return newPullRequests;
    }

    public <T> Iterable<T> concat(Collection<T> left, Collection<T> right) {
        if(left == null) return right;
        else if (right == null) return left;
        else {
            ArrayList<T> concat = new ArrayList<T>();
            concat.addAll(left);
            concat.addAll(right);
            return concat;
        }
    }

    @Override
    public String toString() {
        return pullRequestStatuses.toString();
    }

    public Option<PullRequestStatus> findById(final int prId) {
        return option(pullRequestStatuses.get(prId));
    }

    public boolean hasId(final int prId) {
        return pullRequestStatuses.containsKey(prId);
    }

    public boolean hasNotId(final int prId) {
        return !hasId(prId);
    }

    public boolean hasChanged(int pullRequestId, String newMergedSHA) {
        return hasId(pullRequestId) && !pullRequestStatuses.get(pullRequestId).getMergeSHA().equals(newMergedSHA);
    }

    public PullRequestStatus get(int prId) {
        return pullRequestStatuses.get(prId);
    }
}
