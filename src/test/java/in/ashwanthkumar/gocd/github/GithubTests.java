package in.ashwanthkumar.gocd.github;

import org.junit.Test;

import java.util.Arrays;
import java.util.regex.Pattern;

public class GithubTests {
    @Test
    public void shouldConnectToGithub() throws Exception {
        String repo = "git@github.com:ashwanthkumar/test-pr.git";
        String flyoutDirectory = "/tmp/tpr";

//        GHRepository repository = GitHub.connect().getRepository(GHUtils.parseGithubUrl(repo));
//        GHPullRequest pullRequest1 = repository.getPullRequests(GHIssueState.OPEN).get(0);
//        int prID = GHUtils.prIdFrom(pullRequest1.getDiffUrl().toString());
//        System.out.println("Checking out PR-#" + prID);
//        String repoUrl = repository.getSshUrl();
//        List<GHPullRequest> pullRequests = repository.getPullRequests(GHIssueState.OPEN);
//        for (GHPullRequest pullRequest : pullRequests) {
//            System.out.println(pullRequest.getDiffUrl());
//            System.out.println("Base - " + pullRequest.getBase().getRef());
//            System.out.println("HEAD Ref - " + pullRequest.getHead().getRef());
//        }
        JGitHelper git = new JGitHelper();
        git.cloneOrFetch(repo, flyoutDirectory);
        git.fetchRepository(repo, flyoutDirectory, "+refs/pull/*/merge:refs/gh-merge/remotes/origin/*");
//        git.fetchRepository(repo, flyoutDirectory, "+refs/pull/*/head:refs/gh-pull/remotes/origin/*");
//        git.fetchRepository(repo, flyoutDirectory, "+refs/pull/*/merge:refs/gh-merge/remotes/origin/*");
//        Map<String, Ref> refs = git.refMap(flyoutDirectory);
//        for (Ref ref : refs.values()) {
//            System.out.println(ref.getName() + " with head as " + ref.getObjectId().name());
//        }
//        git.checkoutToRevision(flyoutDirectory, pullRequest1.getHead().getSha());
//        Ref revision = refs.get("refs/gh-merge/remotes/origin/3");
//        System.out.println("Checking out " + revision.getObjectId().name());
//        git.merge(flyoutDirectory,revision);
//        git.checkout(flyoutDirectory, revision);
//        git.checkout(flyoutDirectory, "refs/gh-merge/remotes/origin/3");
        System.out.println(idFromRef("refs/gh-merge/remotes/origin/3"));
    }

    private int idFromRef(String ref) {
        System.out.println(ref);
        Pattern pattern = Pattern.compile("/");
        System.out.println(Arrays.toString(pattern.split(ref)));

        System.out.println(pattern.matcher(ref).groupCount());
        System.out.println(pattern.matcher(ref).matches());
        System.out.println(pattern.matcher(ref).toMatchResult());
        return Integer.parseInt(pattern.matcher(ref).group(1));
    }


}
