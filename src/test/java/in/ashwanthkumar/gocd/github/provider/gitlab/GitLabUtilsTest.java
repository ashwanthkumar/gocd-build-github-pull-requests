package in.ashwanthkumar.gocd.github.provider.gitlab;

import org.junit.Test;

import static in.ashwanthkumar.gocd.github.provider.gitlab.GitLabUtils.*;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class GitLabUtilsTest {
    @Test
    public void shouldIdentifySSHUrl() {
        assertThat(isSSHUrl("git@gitlab.com:ashwanthkumar/gocd-build-github-pull-requests.git"), is(true));
        assertThat(isSSHUrl("https://gitlab.com/ashwanthkumar/gocd-build-github-pull-requests.git"), is(false));
        assertThat(isSSHUrl("git@Gitlab.Com:ashwanthkumar/gocd-build-github-pull-requests"), is(true));
        assertThat(isSSHUrl("git@code.corp.yourcompany.com:username/repo"), is(true));
    }

    @Test
    public void shouldGetServerUrl() {
        assertThat(getServerUrl("https://gitlab.com/ashwanthkumar/gocd-build-github-pull-requests.git"), is("https://gitlab.com"));
        assertThat(getServerUrl("git@code.corp.yourcompany.com:username/repo"), is("https://code.corp.yourcompany.com"));
        assertThat(getServerUrl("https://Github.Com/ashwanthkumar/gocd-build-github-pull-requests"),
                is("https://Github.Com"));
        assertThat(getServerUrl("https://gitlab.company.com/user/test-repo.git"), is("https://gitlab.company.com"));
    }
}
