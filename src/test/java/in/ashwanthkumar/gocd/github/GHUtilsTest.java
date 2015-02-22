package in.ashwanthkumar.gocd.github;

import org.junit.Test;

import static in.ashwanthkumar.gocd.github.GHUtils.parseGithubUrl;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class GHUtilsTest {
    @Test
    public void shouldParseSSH() {
        assertThat(parseGithubUrl("git@github.com:ashwanthkumar/gocd-build-github-pull-requests.git"), is("ashwanthkumar/gocd-build-github-pull-requests"));
        assertThat(parseGithubUrl("git@github.com:ashwanthkumar/gocd-build-github-pull-requests"), is("ashwanthkumar/gocd-build-github-pull-requests"));
        assertThat(parseGithubUrl("git@Github.Com:ashwanthkumar/gocd-build-github-pull-requests"), is("ashwanthkumar/gocd-build-github-pull-requests"));
        assertThat(parseGithubUrl("git@code.corp.yourcompany.com/username/repo"), is("username/repo"));
    }

    @Test
    public void shouldParseHTTPS() {
        assertThat(parseGithubUrl("https://github.com/ashwanthkumar/gocd-build-github-pull-requests.git"), is("ashwanthkumar/gocd-build-github-pull-requests"));
        assertThat(parseGithubUrl("https://github.com/ashwanthkumar/gocd-build-github-pull-requests"), is("ashwanthkumar/gocd-build-github-pull-requests"));
        assertThat(parseGithubUrl("https://Github.Com/ashwanthkumar/gocd-build-github-pull-requests"), is("ashwanthkumar/gocd-build-github-pull-requests"));
        assertThat(parseGithubUrl("https://Github.Com/Ashwanthkumar/gocd-build-github-pull-requests"), is("Ashwanthkumar/gocd-build-github-pull-requests"));
        assertThat(parseGithubUrl("http://code.corp.yourcompany.com/username/repo"), is("username/repo"));
    }

    @Test
    public void shouldCheckForValidUrl() {
        assertTrue(GHUtils.isValidGHUrl("git@github.com:ashwanthkumar/gocd-build-github-pull-requests.git"));
        assertTrue(GHUtils.isValidGHUrl("git@Github.com:ashwanthkumar/gocd-build-github-pull-requests.git"));
        assertTrue(GHUtils.isValidGHUrl("https://github.com/ashwanthkumar/gocd-build-github-pull-requests.git"));
        assertTrue(GHUtils.isValidGHUrl("https://github.com/ashwanthkumar/gocd-build-github-pull-requests"));
        assertTrue(GHUtils.isValidGHUrl("https://Github.com/ashwanthkumar/gocd-build-github-pull-requests"));
        assertFalse(GHUtils.isValidGHUrl("http://ashwanthkumar.in/"));
    }

    @Test
    public void shouldExtractPullRequestIdFromDiffUrl() {
        assertThat(GHUtils.prIdFrom("https://github.com/phanan/htaccess/pull/13.diff"), is(13));
        assertThat(GHUtils.prIdFrom("https://github.com/phanan/htaccess/pull/133.diff"), is(133));
        assertThat(GHUtils.prIdFrom("https://github.com/phanan/htaccess/pull/1.diff"), is(1));
    }


}
