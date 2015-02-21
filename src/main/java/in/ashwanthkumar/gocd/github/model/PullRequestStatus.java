package in.ashwanthkumar.gocd.github.model;

import in.ashwanthkumar.gocd.github.json.Exclude;

public class PullRequestStatus {
    private int id;
    private String ref;
    private String mergeRef;
    private String lastHead;
    private boolean alreadyScheduled;
    @Exclude
    private String prBranch;
    @Exclude
    private String toBranch;
    @Exclude
    private String url;
    @Exclude
    private String author;
    @Exclude
    private String authorEmail;
    @Exclude
    private String description;
    @Exclude
    private String title;

    public PullRequestStatus(int id, String headSHA, String prBranch, String toBranch, String url,
                             String author, String authorEmail, String description, String title) {
        this.id = id;
        this.ref = String.format("refs/pull/%d/head", getId());
        this.mergeRef = String.format("refs/pull/%d/merge", getId());
        this.lastHead = headSHA;
        this.alreadyScheduled = false;
        this.prBranch = prBranch;
        this.toBranch = toBranch;
        this.url = url;
        this.author = author;
        this.authorEmail = authorEmail;
        this.description = description;
        this.title = title;
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

    public String getPrBranch() {
        return prBranch;
    }

    public String getToBranch() {
        return toBranch;
    }

    public String getUrl() {
        return url;
    }

    public String getAuthor() {
        return author;
    }

    public String getAuthorEmail() {
        return authorEmail;
    }

    public String getDescription() {
        return description;
    }

    public String getTitle() {
        return title;
    }

    public PullRequestStatus scheduled() {
        this.alreadyScheduled = true;
        return this;
    }

    public String getMergeRef() {
        return mergeRef;
    }

    public PullRequestStatus merge(PullRequestStatus newPRStatus) {
        if (lastHead.equalsIgnoreCase(newPRStatus.getLastHead())) return copy().mergePRFields(newPRStatus);
        else return new PullRequestStatus(id, newPRStatus.getLastHead(), newPRStatus.prBranch, newPRStatus.toBranch,
                newPRStatus.url, newPRStatus.author, newPRStatus.authorEmail, newPRStatus.description, newPRStatus.title);
    }

    private PullRequestStatus mergePRFields(PullRequestStatus statusWithPRData) {
        this.prBranch = statusWithPRData.prBranch;
        this.toBranch = statusWithPRData.toBranch;
        this.url = statusWithPRData.url;
        this.author = statusWithPRData.author;
        this.authorEmail = statusWithPRData.authorEmail;
        this.description = statusWithPRData.description;
        this.title = statusWithPRData.title;
        return this;
    }

    public boolean hasChanged(String newLatestHead) {
        return !lastHead.equalsIgnoreCase(newLatestHead);
    }

    private PullRequestStatus copy() {
        PullRequestStatus clone = new PullRequestStatus(id, lastHead, prBranch, toBranch, url, author, authorEmail, description, title);
        if (isAlreadyScheduled()) clone.scheduled();
        return clone;
    }


    @Override
    public String toString() {
        return "PullRequestStatus{" +
                "id=" + id +
                ", ref='" + ref + '\'' +
                ", lastHead='" + lastHead + '\'' +
                ", prBranch='" + prBranch + '\'' +
                ", toBranch='" + toBranch + '\'' +
                ", url='" + url + '\'' +
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
        if (prBranch != null ? !prBranch.equals(that.prBranch) : that.prBranch != null) return false;
        if (ref != null ? !ref.equals(that.ref) : that.ref != null) return false;
        if (toBranch != null ? !toBranch.equals(that.toBranch) : that.toBranch != null) return false;
        if (url != null ? !url.equals(that.url) : that.url != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + (ref != null ? ref.hashCode() : 0);
        result = 31 * result + (lastHead != null ? lastHead.hashCode() : 0);
        result = 31 * result + (prBranch != null ? prBranch.hashCode() : 0);
        result = 31 * result + (toBranch != null ? toBranch.hashCode() : 0);
        result = 31 * result + (url != null ? url.hashCode() : 0);
        result = 31 * result + (alreadyScheduled ? 1 : 0);
        return result;
    }
}
