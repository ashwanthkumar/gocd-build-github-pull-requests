package in.ashwanthkumar.gocd.github.model;

import java.util.Date;
import java.util.List;

public class Revision {
    private String revision;
    private Date timestamp;
    private String comment;
    private String user;
    private List<ModifiedFile> modifiedFiles;

    public Revision(String revision, int timestamp, String comment, String user, List<ModifiedFile> modifiedFiles) {
        this.revision = revision;
        this.timestamp = new Date(timestamp);
        this.comment = comment;
        this.user = user;
        this.modifiedFiles = modifiedFiles;
    }

    public String getRevision() {
        return revision;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public String getComment() {
        return comment;
    }

    public String getUser() {
        return user;
    }

    public List<ModifiedFile> getModifiedFiles() {
        return modifiedFiles;
    }

    @Override
    public String toString() {
        return "Revision{" +
                "revision='" + revision + '\'' +
                ", timestamp=" + timestamp +
                ", comment='" + comment + '\'' +
                ", user='" + user + '\'' +
                ", modifiedFiles=" + modifiedFiles +
                '}';
    }
}
