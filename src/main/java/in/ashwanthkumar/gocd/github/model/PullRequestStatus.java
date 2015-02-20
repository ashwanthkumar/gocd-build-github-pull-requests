package in.ashwanthkumar.gocd.github.model;

public class PullRequestStatus {
    private int id;
    private String ref;
    private String lastHead;
    private boolean alreadyScheduled;

    public PullRequestStatus(int id, String headSHA) {
        this.id = id;
        this.ref = String.format("refs/pull/%d/head", getId());
        this.lastHead = headSHA;
        this.alreadyScheduled = false;
    }

    private PullRequestStatus() {
    }

    public int getId() {
        return id;
    }

    public String getRef() {
        return ref;
    }

    public String getLastHead() {
        return lastHead;
    }

    public boolean isAlreadyScheduled() {
        return alreadyScheduled;
    }

    public PullRequestStatus scheduled() {
        this.alreadyScheduled = true;
        return this;
    }

    public PullRequestStatus merge(PullRequestStatus newPRStatus) {
        if (lastHead.equalsIgnoreCase(newPRStatus.getLastHead())) return copy();
        else return new PullRequestStatus(id, newPRStatus.getLastHead());
    }

    public boolean hasChanged(String newLatestHead) {
        return !lastHead.equalsIgnoreCase(newLatestHead);
    }

    private PullRequestStatus copy() {
        PullRequestStatus pullRequestStatus = new PullRequestStatus(id, lastHead);
        if (isAlreadyScheduled()) pullRequestStatus.scheduled();
        return pullRequestStatus;
    }

    @Override
    public String toString() {
        return "PullRequestStatus{" +
                "id=" + id +
                ", ref='" + ref + '\'' +
                ", lastHead='" + lastHead + '\'' +
                ", alreadyScheduled=" + alreadyScheduled +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PullRequestStatus that = (PullRequestStatus) o;

        if (alreadyScheduled != that.alreadyScheduled) return false;
        if (id != that.id) return false;
        if (lastHead != null ? !lastHead.equals(that.lastHead) : that.lastHead != null) return false;
        if (ref != null ? !ref.equals(that.ref) : that.ref != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + (ref != null ? ref.hashCode() : 0);
        result = 31 * result + (lastHead != null ? lastHead.hashCode() : 0);
        result = 31 * result + (alreadyScheduled ? 1 : 0);
        return result;
    }
}
