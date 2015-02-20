package in.ashwanthkumar.gocd.github.model;

import in.ashwanthkumar.utils.collections.Lists;
import in.ashwanthkumar.utils.func.Function;
import in.ashwanthkumar.utils.func.Predicate;
import in.ashwanthkumar.utils.lang.option.Option;
import in.ashwanthkumar.utils.lang.tuple.Tuple2;

import java.util.List;
import java.util.Map;

import static in.ashwanthkumar.utils.collections.Iterables.toMap;
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

    public PullRequests mergeWith(List<PullRequestStatus> newPRStatuses) {
        PullRequests newPullRequests = new PullRequests();
        List<PullRequestStatus> updatedPRs = Lists.map(newPRStatuses, new Function<PullRequestStatus, PullRequestStatus>() {
            @Override
            public PullRequestStatus apply(PullRequestStatus newPRStatus) {
                if (hasId(newPRStatus.getId())) return pullRequestStatuses.get(newPRStatus.getId()).merge(newPRStatus);
                else return newPRStatus;
            }
        });
        List<PullRequestStatus> purgedPRs = Lists.filter(updatedPRs, new Predicate<PullRequestStatus>() {
            @Override
            public Boolean apply(PullRequestStatus input) {
                return pullRequestStatuses.containsKey(input.getId());
            }
        });
        newPullRequests.setPullRequestStatuses(purgedPRs);
        return newPullRequests;
    }

    @Override
    public String toString() {
        return pullRequestStatuses.toString();
    }

    public Option<PullRequestStatus> nextNotProcessed() {
        return Lists.find(pullRequestStatuses.values(), new Predicate<PullRequestStatus>() {
            @Override
            public Boolean apply(PullRequestStatus input) {
                return !input.isAlreadyScheduled();
            }
        });
    }

    public Option<PullRequestStatus> findById(final int prId) {
        return option(pullRequestStatuses.get(prId));
    }

    public boolean hasId(final int prId) {
        return pullRequestStatuses.containsKey(prId);
    }

    public PullRequestStatus get(int prId) {
        return pullRequestStatuses.get(prId);
    }

    public void schedule(int prID) {
        if (hasId(prID)) {
            PullRequestStatus pullRequestStatus = get(prID);
            pullRequestStatus.scheduled();
            pullRequestStatuses.put(prID, pullRequestStatus);
        }
    }
}
