package in.ashwanthkumar.gocd.github.provider.gitlab;

import org.junit.Test;

import static in.ashwanthkumar.gocd.github.provider.gitlab.GitLabUtils.parseGitlabUrl;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class GitLabUtilsTest {
    @Test
    public void shouldParseSSH() {
        assertThat(parseGitlabUrl("git@github.com:ashwanthkumar/gocd-build-github-pull-requests.git"), is("ashwanthkumar/gocd-build-github-pull-requests"));
        assertThat(parseGitlabUrl("git@github.com:ashwanthkumar/gocd-build-github-pull-requests"), is("ashwanthkumar/gocd-build-github-pull-requests"));
        assertThat(parseGitlabUrl("git@Github.Com:ashwanthkumar/gocd-build-github-pull-requests"), is("ashwanthkumar/gocd-build-github-pull-requests"));
        assertThat(parseGitlabUrl("git@code.corp.yourcompany.com:username/repo"), is("username/repo"));
    }

    @Test
    public void shouldParseHTTPS() {
        assertThat(parseGitlabUrl("https://github.com/ashwanthkumar/gocd-build-github-pull-requests.git"), is("ashwanthkumar/gocd-build-github-pull-requests"));
        assertThat(parseGitlabUrl("https://github.com/ashwanthkumar/gocd-build-github-pull-requests"), is("ashwanthkumar/gocd-build-github-pull-requests"));
        assertThat(parseGitlabUrl("https://Github.Com/ashwanthkumar/gocd-build-github-pull-requests"), is("ashwanthkumar/gocd-build-github-pull-requests"));
        assertThat(parseGitlabUrl("https://Github.Com/Ashwanthkumar/gocd-build-github-pull-requests"), is("Ashwanthkumar/gocd-build-github-pull-requests"));
        assertThat(parseGitlabUrl("http://code.corp.yourcompany.com:username/repo"), is("username/repo"));
        assertThat(parseGitlabUrl("https://github.company.com/user/test-repo.git"), is("user/test-repo"));
    }
}
