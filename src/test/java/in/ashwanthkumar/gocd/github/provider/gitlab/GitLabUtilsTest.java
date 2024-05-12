package in.ashwanthkumar.gocd.github.provider.gitlab;

import org.junit.Test;

import static in.ashwanthkumar.gocd.github.provider.gitlab.GitLabUtils.*;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class GitLabUtilsTest {
    @Test
    public void shouldIdentifySSHUrl() {
        assertThat(isSSHUrl("git@gitlab.com:ashwanthkumar/gocd-build-gitlab-pull-requests.git"), is(true));
        assertThat(isSSHUrl("https://gitlab.com/ashwanthkumar/gocd-build-gitlab-pull-requests.git"), is(false));
        assertThat(isSSHUrl("git@Gitlab.Com:ashwanthkumar/gocd-build-gitlab-pull-requests"), is(true));
        assertThat(isSSHUrl("git@code.corp.yourcompany.com:username/repo"), is(true));
    }

    @Test
    public void shouldReturnCorrectProjectPath() {
        assertEquals("ashwanthkumar/gocd-build-gitlab-pull-requests",getProjectPathFromUrl("https://gitlab.com/ashwanthkumar/gocd-build-gitlab-pull-requests.git"));
        assertEquals("username/repo",getProjectPathFromUrl("git@code.corp.yourcompany.com:username/repo"));
        assertEquals("ashwanthkumar/gocd-build-gitlab-pull-requests",getProjectPathFromUrl("https://gitlab.Com/ashwanthkumar/gocd-build-gitlab-pull-requests"));
        assertEquals("user/test-repo",getProjectPathFromUrl("https://gitlab.company.com/user/test-repo.git"));
        assertEquals("user/test-repo",getProjectPathFromUrl(" https://gitlab.company.com/user/test-repo.git   "));
        assertEquals("user/test-repo",getProjectPathFromUrl("https://gitlab.company.com/user/test-repo.git/"));

        //subgroups handling
        assertEquals("group/subgroup/subgroup2/repo",getProjectPathFromUrl("git@code.corp.yourcompany.com:group/subgroup/subgroup2/repo"));
        assertEquals("group1/subgroup2/user/test-repo",getProjectPathFromUrl("http://gitlab.company.com/group1/subgroup2/user/test-repo.git"));
        assertEquals("group1/subgroup2/user/test-repo",getProjectPathFromUrl("https://gitlab.company.com/group1/subgroup2/user/test-repo.git"));
        assertEquals("path/to/repo",getProjectPathFromUrl("ssh://user@host.xz/path/to/repo.git/"));
        assertEquals("group1/subgroup2/user/test-repo",getProjectPathFromUrl("https://gitlab.company.com:8956/group1/subgroup2/user/test-repo.git"));
    }

    @Test
    public void shouldGetServerUrl() {
        assertThat(getServerUrl("https://gitlab.com/ashwanthkumar/gocd-build-gitlab-pull-requests.git"), is("https://gitlab.com"));
        assertThat(getServerUrl("git@code.corp.yourcompany.com:username/repo"), is("https://code.corp.yourcompany.com"));
        assertThat(getServerUrl("https://gitlab.Com/ashwanthkumar/gocd-build-gitlab-pull-requests"),
                is("https://gitlab.Com"));
        assertThat(getServerUrl("https://gitlab.company.com/user/test-repo.git"), is("https://gitlab.company.com"));
    }
}
