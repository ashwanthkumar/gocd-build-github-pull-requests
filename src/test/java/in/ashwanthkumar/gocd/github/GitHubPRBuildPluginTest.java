package in.ashwanthkumar.gocd.github;

import com.google.gson.Gson;
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import com.tw.go.plugin.model.GitConfig;
import com.tw.go.plugin.model.Revision;
import in.ashwanthkumar.gocd.github.provider.github.GHUtils;
import in.ashwanthkumar.gocd.github.provider.github.GitHubProvider;
import in.ashwanthkumar.gocd.github.util.JSONUtils;
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
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class GitHubPRBuildPluginTest {
    public static final String TEST_DIR = "/tmp/" + UUID.randomUUID();
    public static File propertyFile;
    public static boolean propertyFileExisted = false;
    public static String usernameProperty;
    public static String passwordProperty;

    @Before
    public void setUp() throws Exception {
        FileUtils.deleteQuietly(new File(TEST_DIR));
        propertyFile = new File(System.getProperty("user.home"), ".github");
        if (propertyFile.exists()) {
            propertyFileExisted = true;
            Properties props = GHUtils.readPropertyFile();
            usernameProperty = props.getProperty("login");
            passwordProperty = props.getProperty("password");
        } else {
            usernameProperty = "props-username";
            passwordProperty = "props-password";
            FileUtils.writeStringToFile(propertyFile, "login=" + usernameProperty + "\npassword=" + passwordProperty);
        }
    }

    @After
    public void tearDown() {
        FileUtils.deleteQuietly(new File(TEST_DIR));
        if (!propertyFileExisted) {
            FileUtils.deleteQuietly(propertyFile);
        }
    }

    @Test
    public void shouldBuildGitConfig() {
        HashMap<String, String> configuration = new HashMap<String, String>();
        configuration.put("url", "url");
        configuration.put("username", "config-username");
        configuration.put("password", "config-password");

        GitHubPRBuildPlugin plugin = new GitHubPRBuildPlugin();
        plugin.setProvider(new GitHubProvider());
        GitConfig gitConfig = plugin.getGitConfig(configuration);

        assertThat(gitConfig.getUrl(), is("url"));
        assertThat(gitConfig.getUsername(), is("config-username"));
        assertThat(gitConfig.getPassword(), is("config-password"));

        configuration.remove("username");
        configuration.remove("password");

        gitConfig = plugin.getGitConfig(configuration);

        assertThat(gitConfig.getUrl(), is("url"));
        assertThat(gitConfig.getUsername(), is(usernameProperty));
        assertThat(gitConfig.getPassword(), is(passwordProperty));
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
        plugin.setProvider(new GitHubProvider());
        GitHubPRBuildPlugin pluginSpy = spy(plugin);

        GoPluginApiRequest request = mock(GoPluginApiRequest.class);
        when(request.requestBody()).thenReturn("{scm-configuration: {url: {value: \"https://github.com/mdaliejaz/samplerepo.git\"}}, flyweight-folder: \"" + TEST_DIR + "\"}");

        pluginSpy.handleGetLatestRevision(request);

        ArgumentCaptor<GitConfig> gitConfig = ArgumentCaptor.forClass(GitConfig.class);
        ArgumentCaptor<String> prId = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Revision> revision = ArgumentCaptor.forClass(Revision.class);
        verify(pluginSpy).getRevisionMap(gitConfig.capture(), prId.capture(), revision.capture());

        assertThat(prId.getValue(), is("master"));
        assertThat(revision.getValue().getRevision(), is("a683e0a27e66e710126f7697337efca052396a32"));
    }

    @Ignore
    @Test
    public void shouldGetLatestRevisionSince() {
        GitHubPRBuildPlugin plugin = new GitHubPRBuildPlugin();
        plugin.setProvider(new GitHubProvider());
        GitHubPRBuildPlugin pluginSpy = spy(plugin);

        GoPluginApiRequest request = mock(GoPluginApiRequest.class);
        when(request.requestBody()).thenReturn("{scm-configuration: {url: {value: \"https://github.com/mdaliejaz/samplerepo.git\"}}, previous-revision: {revision: \"a683e0a27e66e710126f7697337efca052396a32\", data: {ACTIVE_PULL_REQUESTS: \"{\\\"1\\\": \\\"12c6ef2ae9843842e4800f2c4763388db81d6ec7\\\"}\"}}, flyweight-folder: \"" + TEST_DIR + "\"}");

        pluginSpy.handleLatestRevisionSince(request);

        ArgumentCaptor<GitConfig> gitConfig = ArgumentCaptor.forClass(GitConfig.class);
        ArgumentCaptor<String> prId = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Revision> revision = ArgumentCaptor.forClass(Revision.class);
        verify(pluginSpy).getRevisionMap(gitConfig.capture(), prId.capture(), revision.capture());

        assertThat(prId.getValue(), is("2"));
        assertThat(revision.getValue().getRevision(), is("f985e61e556fc37f952385152d837de426b5cd8a"));
    }

    private void assertPRToRevisionMap(ArgumentCaptor<Map> prStatuses) {
        assertThat(prStatuses.getValue().size(), is(2));
        assertThat(((Map<String, String>) prStatuses.getValue()).get("1"), is("12c6ef2ae9843842e4800f2c4763388db81d6ec7"));
        assertThat(((Map<String, String>) prStatuses.getValue()).get("2"), is("f985e61e556fc37f952385152d837de426b5cd8a"));
    }

    // TODO - Write proper tests for the plugin

    private void verifyValidationSuccess(String url) {
        Map request = createRequestMap(Arrays.asList(new Pair("url", url)));

        GitHubPRBuildPlugin plugin = new GitHubPRBuildPlugin();
        plugin.setProvider(new GitHubProvider());
        GoPluginApiResponse response = plugin.handle(createGoPluginApiRequest(GitHubPRBuildPlugin.REQUEST_VALIDATE_SCM_CONFIGURATION, request));

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
        List response = (List) JSONUtils.fromJSON(responseBody);
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