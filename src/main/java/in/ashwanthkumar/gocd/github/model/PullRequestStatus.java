package in.ashwanthkumar.gocd.github.model;

import in.ashwanthkumar.gocd.github.GitConstants;
import in.ashwanthkumar.gocd.github.json.Exclude;

public class PullRequestStatus {
    private int id;
    private String mergeRef;
    // We use this while computing the revisions
    private String lastHead;
    // We use this to find changes in a PR
    private String mergeSHA;
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

    public PullRequestStatus(int id, String lastHead, String mergedSHA, String prBranch, String toBranch, String url,
                             String author, String authorEmail, String description, String title) {
        this.id = id;
        this.mergeRef = String.format("%s/%d", GitConstants.PR_MERGE_PREFIX , getId());
        this.lastHead = lastHead;
        this.mergeSHA = mergedSHA;
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

    public String getLastHead() {
        return lastHead;
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

    public String getMergeRef() {
        return mergeRef;
    }

    public String getMergeSHA() {
        return mergeSHA;
    }

    public PullRequestStatus merge(PullRequestStatus newPRStatus) {
        if (mergeSHA.equalsIgnoreCase(newPRStatus.getMergeSHA())) return copy().mergePRFields(newPRStatus);
        else return new PullRequestStatus(id, lastHead, newPRStatus.mergeSHA, newPRStatus.prBranch, newPRStatus.toBranch,
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
        return new PullRequestStatus(id, lastHead, mergeSHA, prBranch, toBranch, url, author, authorEmail, description, title);
    }

    @Override
    public String toString() {
        return "PullRequestStatus{" +
                "id=" + id +
                ", mergeRef='" + mergeRef + '\'' +
                ", lastHead='" + lastHead + '\'' +
                ", mergeSHA='" + mergeSHA + '\'' +
                ", prBranch='" + prBranch + '\'' +
                ", toBranch='" + toBranch + '\'' +
                ", url='" + url + '\'' +
                ", author='" + author + '\'' +
                ", authorEmail='" + authorEmail + '\'' +
                ", description='" + description + '\'' +
                ", title='" + title + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PullRequestStatus that = (PullRequestStatus) o;

        if (id != that.id) return false;
        if (author != null ? !author.equals(that.author) : that.author != null) return false;
        if (authorEmail != null ? !authorEmail.equals(that.authorEmail) : that.authorEmail != null) return false;
        if (description != null ? !description.equals(that.description) : that.description != null) return false;
        if (lastHead != null ? !lastHead.equals(that.lastHead) : that.lastHead != null) return false;
        if (mergeRef != null ? !mergeRef.equals(that.mergeRef) : that.mergeRef != null) return false;
        if (mergeSHA != null ? !mergeSHA.equals(that.mergeSHA) : that.mergeSHA != null) return false;
        if (prBranch != null ? !prBranch.equals(that.prBranch) : that.prBranch != null) return false;
        if (title != null ? !title.equals(that.title) : that.title != null) return false;
        if (toBranch != null ? !toBranch.equals(that.toBranch) : that.toBranch != null) return false;
        if (url != null ? !url.equals(that.url) : that.url != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + (mergeRef != null ? mergeRef.hashCode() : 0);
        result = 31 * result + (lastHead != null ? lastHead.hashCode() : 0);
        result = 31 * result + (mergeSHA != null ? mergeSHA.hashCode() : 0);
        result = 31 * result + (prBranch != null ? prBranch.hashCode() : 0);
        result = 31 * result + (toBranch != null ? toBranch.hashCode() : 0);
        result = 31 * result + (url != null ? url.hashCode() : 0);
        result = 31 * result + (author != null ? author.hashCode() : 0);
        result = 31 * result + (authorEmail != null ? authorEmail.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (title != null ? title.hashCode() : 0);
        return result;
    }
}
