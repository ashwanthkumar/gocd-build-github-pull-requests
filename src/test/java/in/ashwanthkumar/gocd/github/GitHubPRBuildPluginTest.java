package in.ashwanthkumar.gocd.github;

import com.google.gson.Gson;
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import com.tw.go.plugin.model.Revision;
import in.ashwanthkumar.gocd.github.model.PullRequestStatus;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class GitHubPRBuildPluginTest {
    public static final String TEST_DIR = "/tmp/" + UUID.randomUUID();

    @Before
    public void setUp() {
        FileUtils.deleteQuietly(new File(TEST_DIR));
    }

    @After
    public void tearDown() {
        FileUtils.deleteQuietly(new File(TEST_DIR));
    }

    @Test
    public void shouldHandleInvalidURLCorrectly_ValidationRequest() {
        Map request = createRequestMap(Arrays.asList(new Pair("url", "crap")));

        GoPluginApiResponse response = new GitHubPRBuildPlugin().handle(createGoPluginApiRequest(GitHubPRBuildPlugin.REQUEST_VALIDATE_SCM_CONFIGURATION, request));

        verifyResponse(response.responseBody(), Arrays.asList(new Pair("url", "Invalid URL")));
    }

    @Test
    public void shouldHandleValidURLCorrectly_ValidationRequest() throws IOException {
        verifyValidationSuccess("https://github.com/ashwanthkumar/foo");
        verifyValidationSuccess("https://github.com/ashwanthkumar/bar");
        verifyValidationSuccess("git@github.com:ashwanthkumar/baz");
    }

    @Ignore
    @Test
    public void shouldGetLatestRevision() {
        GitHubPRBuildPlugin plugin = new GitHubPRBuildPlugin();
        GitHubPRBuildPlugin pluginSpy = spy(plugin);

        GoPluginApiRequest request = mock(GoPluginApiRequest.class);
        when(request.requestBody()).thenReturn("{scm-configuration: {url: {value: \"https://github.com/mdaliejaz/samplerepo.git\"}}, flyweight-folder: \"" + TEST_DIR + "\"}");

        pluginSpy.handleGetLatestRevision(request);

        ArgumentCaptor<Revision> revision = ArgumentCaptor.forClass(Revision.class);
        ArgumentCaptor<Map> prStatuses = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<PullRequestStatus> currentPR = ArgumentCaptor.forClass(PullRequestStatus.class);
        verify(pluginSpy).getRevisionMap(revision.capture(), prStatuses.capture(), currentPR.capture());

        assertThat(revision.getValue().getRevision(), is("a683e0a27e66e710126f7697337efca052396a32"));
        assertThat(prStatuses.getValue().size(), is(1));
        assertThat(((Map<String, String>) prStatuses.getValue()).get("1"), is("aabd0f242bd40bfaaa4ce359123b2a2d976077d1"));
        assertThat(currentPR.getValue(), is(nullValue()));
    }

    @Ignore
    @Test
    public void shouldGetLatestRevisionSince() {
        GitHubPRBuildPlugin plugin = new GitHubPRBuildPlugin();
        GitHubPRBuildPlugin pluginSpy = spy(plugin);

        GoPluginApiRequest request = mock(GoPluginApiRequest.class);
        when(request.requestBody()).thenReturn("{scm-configuration: {url: {value: \"https://github.com/mdaliejaz/samplerepo.git\"}}, previous-revision: {revision: \"a683e0a27e66e710126f7697337efca052396a32\", data: {ACTIVE_PULL_REQUESTS: \"{1: \\\"aabd0f242bd40bfaaa4ce359123b2a2d976077d1\\\"}\"}}, flyweight-folder: \"" + TEST_DIR + "\"}");

        pluginSpy.handleLatestRevisionSince(request);

        verify(pluginSpy).renderJSON(eq(200), eq(nullValue()));
    }

    // TODO - Write proper tests for the plugin

    private void verifyValidationSuccess(String url) {
        Map request = createRequestMap(Arrays.asList(new Pair("url", url)));

        GoPluginApiResponse response = new GitHubPRBuildPlugin().handle(createGoPluginApiRequest(GitHubPRBuildPlugin.REQUEST_VALIDATE_SCM_CONFIGURATION, request));

        verifyResponse(response.responseBody(), null);
    }

    private Map createRequestMap(List<Pair> pairs) {
        final Map request = new HashMap();
        Map scmConfiguration = new HashMap();

        for (Pair pair : pairs) {
            Map valueMap = new HashMap();
            valueMap.put("value", pair.value);
            scmConfiguration.put(pair.key, valueMap);
        }

        request.put("scm-configuration", scmConfiguration);
        return request;
    }

    private void verifyResponse(String responseBody, List<Pair> pairs) {
        List response = new Gson().fromJson(responseBody, List.class);
        for (Object r : response) {
            System.out.println(r);
        }
        if (pairs == null) {
            assertThat(response.size(), is(0));
        } else {
            for (int i = 0; i < pairs.size(); i++) {
                assertThat((String) ((Map) response.get(i)).get("key"), is(pairs.get(i).key));
                assertThat((String) ((Map) response.get(i)).get("message"), is(pairs.get(i).value));
            }
        }
    }

    private GoPluginApiRequest createGoPluginApiRequest(final String requestName, final Map request) {
        return new GoPluginApiRequest() {
            @Override
            public String extension() {
                return null;
            }

            @Override
            public String extensionVersion() {
                return null;
            }

            @Override
            public String requestName() {
                return requestName;
            }

            @Override
            public Map<String, String> requestParameters() {
                return null;
            }

            @Override
            public Map<String, String> requestHeaders() {
                return null;
            }

            @Override
            public String requestBody() {
                return new Gson().toJson(request);
            }
        };
    }

    class Pair {
        String key;
        String value;

        public Pair(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }
}