package in.ashwanthkumar.gocd.github;

import in.ashwanthkumar.gocd.github.model.MergeRefs;
import in.ashwanthkumar.gocd.github.model.ModifiedFile;
import in.ashwanthkumar.gocd.github.model.Revision;
import in.ashwanthkumar.utils.collections.Lists;
import in.ashwanthkumar.utils.func.Function;
import in.ashwanthkumar.utils.func.Predicate;
import in.ashwanthkumar.utils.lang.StringUtils;
import in.ashwanthkumar.utils.lang.option.Option;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static java.lang.String.format;

public class JGitHelper {
    public void cloneOrFetch(String url, String folder) throws Exception {
        if (!new File(folder).exists() || !getGitDir(folder).exists()) {
            cloneRepository(url, folder);
        } else {
            cleanRepository(folder);
            fetchRepository(url, folder);
            gcRepository(folder);
            resetRepository(folder, "origin/master");
            cleanRepository(folder);
        }
    }

    public Iterable<Ref> refs(String folder) throws Exception {
        Repository repository = null;
        try {
            repository = new FileRepositoryBuilder().setGitDir(getGitDir(folder)).readEnvironment().findGitDir().build();
            Git git = new Git(repository);
            return git.getRepository().getAllRefs().values();
        } finally {
            if (repository != null) {
                repository.close();
            }
        }
    }

    private void cloneRepository(String url, String folder) throws Exception {
        // delete if exists
        new File(folder).mkdirs();

        CloneCommand cloneCommand = Git.cloneRepository().setURI(url).setDirectory(new File(folder));
        if (url.startsWith("http") || url.startsWith("https")) {
            // set credentials
        }
        cloneCommand.call();
    }

    public void cleanRepository(String folder) throws Exception {
        Repository repository = null;
        try {
            repository = new FileRepositoryBuilder().setGitDir(getGitDir(folder)).readEnvironment().findGitDir().build();
            Git git = new Git(repository);
            CleanCommand clean = git.clean().setCleanDirectories(true);
            clean.call();
        } finally {
            if (repository != null) {
                repository.close();
            }
        }
    }

    public void fetchRepository(String url, String folder, String... refs) throws Exception {
        // check remote url - if ok
        resetRepository(folder);

        Repository repository = null;
        try {
            repository = new FileRepositoryBuilder().setGitDir(getGitDir(folder)).readEnvironment().findGitDir().build();
            Git git = new Git(repository);
            List<RefSpec> refSpecs = Lists.map(Lists.of(refs), new Function<String, RefSpec>() {
                @Override
                public RefSpec apply(String input) {
                    return new RefSpec(input);
                }
            });
            FetchCommand fetch = git.fetch().setRemoveDeletedRefs(true).setRefSpecs(refSpecs);
            if (url.startsWith("http") || url.startsWith("https")) {
                // TODO - if url is http/https - set credentials
            }
            fetch.call();
        } finally {
            if (repository != null) {
                repository.close();
            }
        }
        // else delete folder & clone
    }

    private void gcRepository(String folder) throws Exception {
        Repository repository = null;
        try {
            repository = new FileRepositoryBuilder().setGitDir(getGitDir(folder)).readEnvironment().findGitDir().build();
            Git git = new Git(repository);
            GarbageCollectCommand gc = git.gc();
            gc.call();
        } finally {
            if (repository != null) {
                repository.close();
            }
        }
    }

    private void resetRepository(String folder) throws Exception {
        resetRepository(folder, null);
    }

    private void resetRepository(String folder, String revision) throws Exception {
        Repository repository = null;
        try {
            repository = new FileRepositoryBuilder().setGitDir(getGitDir(folder)).readEnvironment().findGitDir().build();
            Git git = new Git(repository);
            ResetCommand reset = git.reset().setMode(ResetCommand.ResetType.HARD);
            if (StringUtils.isNotEmpty(revision)) reset.setRef(revision);
            reset.call();
        } finally {
            if (repository != null) {
                repository.close();
            }
        }
    }

    public Revision getLatestRevision(String folder) throws Exception {
        Repository repository = null;
        try {
            repository = new FileRepositoryBuilder().setGitDir(getGitDir(folder)).readEnvironment().findGitDir().build();
            Git git = new Git(repository);
            Iterable<RevCommit> log = git.log().call();
            Iterator<RevCommit> iterator = log.iterator();
            if (iterator.hasNext()) {
                return getRevisionObj(repository, iterator.next());
            }
        } finally {
            if (repository != null) {
                repository.close();
            }
        }
        return null;
    }

    public List<Revision> getNewerRevisions(String folder, String previousRevision) throws Exception {
        Repository repository = null;
        try {
            repository = new FileRepositoryBuilder().setGitDir(getGitDir(folder)).readEnvironment().findGitDir().build();
            Git git = new Git(repository);
            Iterable<RevCommit> log = git.log().call();
            Iterator<RevCommit> iterator = log.iterator();
            List<RevCommit> newCommits = new ArrayList<RevCommit>();
            while (iterator.hasNext()) {
                RevCommit commit = iterator.next();
                if (commit.getName().equals(previousRevision)) {
                    break;
                }
                newCommits.add(commit);
            }

            if (newCommits.isEmpty()) {
                return null;
            }

            List<Revision> revisionObjs = new ArrayList<Revision>();
            for (RevCommit newCommit : newCommits) {
                Revision revisionObj = getRevisionObj(repository, newCommit);
                revisionObjs.add(revisionObj);
            }
            return revisionObjs;
        } finally {
            if (repository != null) {
                repository.close();
            }
        }
    }

    public void checkoutToRevision(String folder, String revision) throws Exception {
        resetRepository(folder, revision);
    }

    /**
     * Returns true if post merge there are no conflicts, false otherwise.
     */
    public boolean hasMergeRef(String folder, int prID) throws Exception {
        Repository repository = null;
        try {
            repository = new FileRepositoryBuilder().setGitDir(getGitDir(folder)).readEnvironment().findGitDir().build();
            Git git = new Git(repository);
            return git.getRepository().getRef(String.valueOf(String.format("%s/%d", GitConstants.PR_MERGE_PREFIX, prID))) != null;
        } finally {
            if (repository != null) {
                repository.close();
            }
        }
    }

    public MergeRefs findMergeRef(String folder) throws Exception {
        Repository repository = null;
        try {
            repository = new FileRepositoryBuilder().setGitDir(getGitDir(folder)).readEnvironment().findGitDir().build();
            Git git = new Git(repository);
            return new MergeRefs(Lists.filter(git.getRepository().getAllRefs().values(), new Predicate<Ref>() {
                @Override
                public Boolean apply(Ref input) {
                    return input.getName().startsWith(GitConstants.PR_MERGE_PREFIX);
                }
            }));
        } finally {
            if (repository != null) {
                repository.close();
            }
        }
    }

    private Revision getRevisionObj(Repository repository, RevCommit commit) throws IOException {
        String commitSHA = commit.getName();
        int commitTime = commit.getCommitTime();
        String comment = commit.getFullMessage().trim();
        String user = commit.getAuthorIdent().getEmailAddress();
        List<ModifiedFile> modifiedFiles = new ArrayList<ModifiedFile>();
        if (commit.getParentCount() == 0) {
            TreeWalk treeWalk = new TreeWalk(repository);
            treeWalk.addTree(commit.getTree());
            treeWalk.setRecursive(false);
            while (treeWalk.next()) {
                modifiedFiles.add(new ModifiedFile(treeWalk.getPathString(), "added"));
            }
        } else {
            RevWalk rw = new RevWalk(repository);
            RevCommit parent = rw.parseCommit(commit.getParent(0).getId());
            DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE);
            diffFormatter.setRepository(repository);
            diffFormatter.setDiffComparator(RawTextComparator.DEFAULT);
            diffFormatter.setDetectRenames(true);
            List<DiffEntry> diffEntries = diffFormatter.scan(parent.getTree(), commit.getTree());
            for (DiffEntry diffEntry : diffEntries) {
                modifiedFiles.add(new ModifiedFile(diffEntry.getNewPath(), getAction(diffEntry.getChangeType().name())));
            }
        }

        return new Revision(commitSHA, commitTime, comment, user, modifiedFiles);
    }

    private String getAction(String gitAction) {
        if (gitAction.equalsIgnoreCase("ADD") || gitAction.equalsIgnoreCase("RENAME")) {
            return "added";
        }
        if (gitAction.equals("MODIFY")) {
            return "modified";
        }
        if (gitAction.equals("DELETE")) {
            return "deleted";
        }
        return "unknown";
    }

    private File getGitDir(String folder) {
        return new File(folder, ".git");
    }
}
