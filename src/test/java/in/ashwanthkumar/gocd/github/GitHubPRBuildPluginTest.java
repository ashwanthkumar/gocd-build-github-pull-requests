package in.ashwanthkumar.gocd.github;

import com.google.gson.Gson;
import com.thoughtworks.go.plugin.api.GoApplicationAccessor;
import com.thoughtworks.go.plugin.api.request.GoApiRequest;
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.DefaultGoApiResponse;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import com.tw.go.plugin.GitHelper;
import com.tw.go.plugin.model.GitConfig;
import com.tw.go.plugin.model.ModifiedFile;
import com.tw.go.plugin.model.Revision;
import in.ashwanthkumar.gocd.github.jsonapi.PipelineStatus;
import in.ashwanthkumar.gocd.github.jsonapi.Server;
import in.ashwanthkumar.gocd.github.jsonapi.ServerFactory;
import in.ashwanthkumar.gocd.github.provider.gerrit.GerritProvider;
import in.ashwanthkumar.gocd.github.provider.git.GitProvider;
import in.ashwanthkumar.gocd.github.provider.github.GHUtils;
import in.ashwanthkumar.gocd.github.provider.github.GitHubProvider;
import in.ashwanthkumar.gocd.github.settings.general.GeneralPluginSettings;
import in.ashwanthkumar.gocd.github.util.GitFactory;
import in.ashwanthkumar.gocd.github.util.GitFolderFactory;
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

import static java.util.Arrays.asList;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
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
        GitConfig gitConfig = plugin.getGitConfig(plugin.getProvider().getScmConfigurationView().getSettings(configuration));

        assertThat(gitConfig.getUrl(), is("url"));
        assertThat(gitConfig.getUsername(), is("config-username"));
        assertThat(gitConfig.getPassword(), is("config-password"));

        configuration.remove("username");
        configuration.remove("password");

        gitConfig = plugin.getGitConfig(plugin.getProvider().getScmConfigurationView().getSettings(configuration));

        assertThat(gitConfig.getUrl(), is("url"));
        assertThat(gitConfig.getUsername(), is(usernameProperty));
        assertThat(gitConfig.getPassword(), is(passwordProperty));
    }

    @Ignore("url validation is turned off")
    @Test
    public void shouldHandleInvalidURLCorrectly_ValidationRequest() {
        Map request = createRequestMap(asList(new Pair("url", "crap")));

        GoPluginApiResponse response = new GitHubPRBuildPlugin().handle(createGoPluginApiRequest(GitHubPRBuildPlugin.REQUEST_VALIDATE_SCM_CONFIGURATION, request));

        verifyResponse(response.responseBody(), asList(new Pair("url", "Invalid URL")));
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

    @Ignore
    @Test
    public void shouldReproduceGetLatestAndGetLatestSince() {
        GitHubPRBuildPlugin plugin = new GitHubPRBuildPlugin();
        Map url = new HashMap();
        url.put("value", "https://github.com/gocd/gocd.git");
        Map configuration = new HashMap();
        configuration.put("url", url);
        Map request = new HashMap();
        request.put("scm-configuration", configuration);
        request.put("flyweight-folder", "/tmp/flyweight-folder");

        GoPluginApiResponse response = plugin.handleGetLatestRevision(createGoPluginApiRequest("", request));
        Map responseMap = (Map) JSONUtils.fromJSON(response.responseBody());

        request.put("scm-data", responseMap.get("scm-data"));

        response = plugin.handleLatestRevisionSince(createGoPluginApiRequest("", request));
        responseMap = (Map) JSONUtils.fromJSON(response.responseBody());
    }

    @Test
    public void shouldParseJSONMapCorrectly() {
        String a = "{\"943\":\"949526c82f73bc0854c9e788a1104436c30d4968\",\"330\":\"c4a1a13c3ba781970c2471bddc23a49ef996e603\",\"949\":\"75aa18d0facbe0cf729795b3414a17acb50ac5f0\",\"337\":\"945b14a67508cd15b6d95a967dd55e0603db0eff\",\"947\":\"77e7a7ce904888306b033de28fe1c37ebecfa1c3\",\"748\":\"c543375eca07c8f7e32546efa0aab14221364815\",\"747\":\"8dee3c54646e26ea373a75f00ea4a8cb08d10e36\",\"600\":\"3d063ec84c91a7b2af82e4c4ac1ef7cf0d2a63a5\",\"153\":\"7a522ae27fffb6232f99e54907950fed775bad97\",\"746\":\"b742c87b6bb0ea2bcbd01072688ab66a56a0b066\",\"533\":\"a3344e14b8a535da96fede747d5a48a3838cfa8d\",\"532\":\"2293f4066239629005e27edf7554b4ade461882d\",\"740\":\"04f2302ae54b9e931371f178fdfe249791038c33\",\"608\":\"d8e989c685b94a37477de625c9c810e750660d76\",\"538\":\"8ee9a78376f770dfb84b863bbc8479e4d5e5f738\",\"607\":\"64d3ffea1eead6289420406f385d50c675972d5e\",\"537\":\"27fe8c424de93643e23d15e7ce35ba4804a9e498\",\"606\":\"adbf542efcd0e37cfc3c3d1dd61eb30b5c95dc9d\",\"536\":\"0b2b11c870335cf235d0fdcc178937894cfdbd8e\",\"744\":\"118794c4b73c6cca50004d01dbb9ea59a742837d\",\"605\":\"0ecd14652b68dafa0a1681679d0be757c826d66b\",\"743\":\"bee57d7437b139f7c186b92faf6baefb92447570\",\"604\":\"8916c9efbfc30a2868b666aa558c5a26c1209d13\",\"742\":\"34ed3ca03fca1ea5ceea6667f915f98e9c0907fc\",\"349\":\"8ab3cdc56a8fc476146c003ebf6739217ba4355b\",\"203\":\"e5c29270856cc7b9c00c61bf149512ab95c921d0\",\"204\":\"fef319de44cf363868a9507de11548f8afac54ad\",\"201\":\"bab5623311e833913081225e95b5eebfffdf75a1\",\"341\":\"63365da04d126e6eba4f5be01fbe111ca0a01deb\",\"169\":\"3d8f94d761ce3ef7c9fdb56a67ab81bca823c6da\",\"343\":\"b58eb2241f3033b678f6d1e41de0bcb721d3e2e1\",\"935\":\"d64ae87f339d451710cd1cb23beefbd49764efe0\",\"344\":\"8f61d15f2dda1900afa48577ed43eba0d9f44847\",\"936\":\"8b4ec1ef90d1df6d8bd350ae29b8b9044ac69be7\",\"206\":\"bd88e6ff91e3ddef83df71a00ab3db07c4608012\",\"166\":\"ca43b27d6a0452566fde65b0e864864bacb7ade2\",\"345\":\"daf0730737d64e1d98a35476ffd54ddd6bcea99a\",\"207\":\"5606df362eb275e596664e451f1a2ad4ac3ed421\",\"165\":\"431498d059f35296a7e2cf72f416edb2c9e01333\",\"346\":\"c9138e6ab052f97a4ea2f008d3257a49a4f88f7c\",\"208\":\"5ed42684ff5dde439fdcc97a88799a0c0e97bb5e\",\"347\":\"b37d013fad8ba1e86d766bd70cf6e624e8cce660\",\"939\":\"943ab0651b801acdfa27b180ada7cb095b832f70\",\"209\":\"8a70cef9dc9fd9d409972531fbed0ba3cbd04803\",\"167\":\"66f3c0b0d68d6cbd1a88ee7ceb4edca4080f4297\",\"735\":\"5c45e8f00cb62ec336834b1db5e55e63274b38d1\",\"409\":\"accfc9db8954e8b6614c645425b03ad304930907\",\"929\":\"f032b28b2265e7416cea7badc01d32984083718f\",\"737\":\"355229ba506a324d33a7a7453b4022d6f7999bbe\",\"163\":\"765679a595180f5d75ccbb531476569b6dd7e584\",\"542\":\"678ec33ea86108cb35b82f0764ea9c71f9f682e6\",\"164\":\"d19e45507385aabb0340b3bb1af878b900443051\",\"541\":\"877facce77017403f705498df8736a286c619d3f\",\"405\":\"8e76671128c62cc4160f7b1ad002b5614032b2d9\",\"544\":\"414491b938948883551572a877e2df7dabf077c9\",\"738\":\"de9230f0a4dc335ee24890be28716e35cbb355ea\",\"543\":\"553a7ca4d8760213e8b234b9c63373cfc32f49a3\",\"340\":\"366410d78d24c5c08f9702933e6181242f50104a\",\"407\":\"4a20f45767c6ccab59e553aa51ff50800b27777f\",\"160\":\"de318cbde3ba99107d888ca0d7bf436ccfa54973\",\"406\":\"3d3483285c33a34ef90e311360027babe0831829\",\"545\":\"128aa53bb6f536c8b755ad5c165b00cbeaa122c5\",\"547\":\"b01a5b06af22e0f37d9f5652f1de523b2527e708\",\"549\":\"38d81c560cd66633969403efd5279fdb44eb8fb0\",\"731\":\"694f54ec61e486b20cced24f39bedf5c83aa148d\",\"730\":\"12479cc68440cd83f53c22110cad29c3066d5a18\",\"824\":\"6e9b8cb78d76b2218620101758e0a9740c429341\",\"211\":\"d1dff05da5c25089a29d6d2eef2c41c397f27e8c\",\"920\":\"2fd67f2d395e186a421408ecf8dbfcb9ea8b2920\",\"212\":\"df73384849fcbcd792e9257640382552565a7fec\",\"822\":\"5beedd21e5a5d5348eb12bcaf7066942e74276f3\",\"823\":\"e45ea65521c22e02ae920a8956ba09203bf64f08\",\"210\":\"7f809e75e853d96daaa5445554aa74dfe90533fd\",\"828\":\"736a3b676b17abef363bea1330f4895b59e40225\",\"215\":\"2cb01030785be7978e85c218b33329dd878369fe\",\"318\":\"2e38b3de9f0f0bb4b2e78f1216ca01bbfd71743b\",\"319\":\"e27a0c0f11ada6558db90b8a6df84cc61afb9bed\",\"213\":\"3893b05fda6e442ab7c98ce4ecedda15308d1715\",\"316\":\"66df9f09ec5e299168fc1959840dc6351da9a8f7\",\"214\":\"554270c5f63c1323e5d4618c4465b5d604b7096d\",\"317\":\"2845ed59f4c0156712e036b1ffb8f439a18dc957\",\"927\":\"79480767d8f6051ad03a7a5c2ce1ca3a7fb4aa2a\",\"219\":\"e91c8b18c09d7713a107ab89a5198ebc503b1428\",\"179\":\"39283829424fc325f92e577a209f699fe5d96479\",\"314\":\"2b55b7cd7ba20c9d2fa06efb20d7fa5a8d557cbd\",\"928\":\"4b4809db87e518cf76101e7465f9aeb1c1b0d615\",\"178\":\"1cabad9905358b2f8e1d7aa63d1f8f0177beed32\",\"925\":\"55b9d76f0967df261231d9bf9ec2deb0d5e9356d\",\"177\":\"39452c5bdcdc890e6ab2f0d6b7837dd00e5b31cd\",\"312\":\"57cb1a944ac28be19eb8153cc520cd15b3f7f1ef\",\"926\":\"3d5a5ef708de337243c0f732f0e53bdb51090d40\",\"218\":\"3674a6f19535a8bf3425f4d4847ac545efade0b0\",\"176\":\"14877470e78bbf24725d2b1cdc44eae78b553471\",\"820\":\"5df4e64721f4ce1f9a16c9cc299594831347aba2\",\"310\":\"be1162408d58e947e323bf40f47b3eec96d7ba0a\",\"924\":\"6786ee84d3e378a301d53a4e3ec33b27a08e3956\",\"821\":\"9a1899fcc57efda2b20c939b8f1c4e0144a22558\",\"311\":\"2940c716ab75e4c1ff1c0162bfbb35d010654062\",\"921\":\"7e6caf645483c7f5619e718ea69b848dd3847277\",\"922\":\"8e66a78bd774a61237db74e68ed0aa09de28f2cf\",\"729\":\"d0bd2ac0434547dde749c19b42993e8a6e2f3e1c\",\"512\":\"4fb78ca9e6a7d9278127301a314adca6ca86fd44\",\"728\":\"502a9754feba4eb08115d4b94f18173c18b62756\",\"510\":\"6a27eb27e488f7df22d077318069a4283f785d09\",\"726\":\"110a7ae6a3d3604f37b4d0200e9031f0c10b3346\",\"623\":\"f86402648de5342c50f8b2db4a58ef91048ac96d\",\"725\":\"b433356ff15cf72c1e8478684fbc9796f745dc42\",\"175\":\"e8767559751011c883cd72819307ceffce6fc62d\",\"621\":\"a6815c9f8e846ccf7ab0b356d2cb974d31f8dfdd\",\"918\":\"13b28499e0b818e6bae5af2a00ed35316580ce30\",\"173\":\"46d5ba78ab455bc32cfb5ca683a184fbffd2bfe6\",\"627\":\"8bc9d7b75d4734e79793890959f01ead01599ed3\",\"626\":\"a66ccfc5967606e35bfb487e3931bf4f93a9f4c7\",\"720\":\"b5433069d2b5491355bd803cbe11710c3e528ddf\",\"625\":\"60046744fd1311b1642a9a88944e9d0d393630e6\",\"819\":\"1e9e6a00b4b7d2805941256965b6d3f1b56a21b5\",\"624\":\"a0766fe962957b847ab0bb9db140e99d1e387aa2\",\"412\":\"afc679b23a1c941caed7da8ff5f1ff3fab59f6e1\",\"628\":\"2730d416d92fef8320c070f503f75e1ccd3b30f8\",\"411\":\"5460dd862b1c6197c6c9d45cbfc01d6172e26a43\",\"514\":\"6ac85e284439265a9f2e3be50f5def534ef11648\",\"812\":\"07b89a8dd2eb16ed22ec557dd59fdaee3680c605\",\"814\":\"e5938021246259221422664fa7af6e8b63e4c80f\",\"327\":\"3d803e13b1ae15cb4dbf07367e60f9adeb6451db\",\"328\":\"17e02fa4728a393cef9531184c5867b0cc1251d2\",\"226\":\"ffa95fab20d8b443965d340f2e3aca07e4ebe3f9\",\"329\":\"8d6aeaa1623990e2ca4431df0e0196488c7eadee\",\"228\":\"ab4f3b8b72350ea061653207a8215ce5546f2bae\",\"323\":\"fd5e5e0e0de8964cd98c4e346d884ce236e88559\",\"915\":\"91584ee5daca11e937d91541ba576a929657647a\",\"229\":\"95835bfd3d896d45f79774b649f4e6c5b41d7231\",\"187\":\"a552bc95835f277589326714d1ed3cf6c38203dd\",\"320\":\"ccc64726ca1ed87943372cb7a77c85abcb54f894\",\"912\":\"b037046785dac26faf8e9c8b24d9ff2ecba23995\",\"913\":\"9747137518d695cd967d6582ac80bcb66a481d8b\",\"717\":\"b687b44809cb65b8a2d2899295b68b16a59d60fc\",\"522\":\"097aeef86a6848bebb021de8e76bb0c3f388b92c\",\"180\":\"78e1ef92d43595d7ee868a03ce3459c95ab79cd9\",\"521\":\"6490c9443eeba134a9c66b2a2c8b704b150df079\",\"524\":\"783fb4352a7665121da22d3a54414b07adb4c7c8\",\"182\":\"cead48ee0aa6356f5aec9944cf67b4880d1fa004\",\"908\":\"c9b44d93ac6c9adf78c949d3797f5209a732a4a8\",\"610\":\"8a71c9f8f3054fcd3dd49cf56cd7c023c060924a\",\"907\":\"c9b44d93ac6c9adf78c949d3797f5209a732a4a8\",\"712\":\"66935a99e7e1e59cddcb9b1e1f1c16c3ba5ffc08\",\"612\":\"256f1119b5cefa830bed6c57090752233ca58f6b\",\"185\":\"ffa207915dbfacd15ee1f34967c7b42a7b9c2752\",\"909\":\"3835da95f3c0fb6bb5714084c3ef589f6a013272\",\"611\":\"dfc8668bb1caf8cd897ee673990d4db1833be7b6\",\"809\":\"697fe5637d32ff54e1e4f0495498d011b5dc13bb\",\"614\":\"bd199604e51de1e9be87c541db1def9a21ccbabb\",\"808\":\"f6e8fa5704adfb70600326088eece1156291f1d8\",\"616\":\"2c837608f9f559b58eeecfd4f10b383983a9d9f7\",\"615\":\"d5756cff43336425e8732f184646b825db6f58f5\",\"617\":\"3ed1893795c99e4ae90bad1d874cd7e62ba8c3c2\",\"525\":\"caaf201998920c765caaa1b0cf9e8ba1af9c51f2\",\"528\":\"15e3b2f84217ee84754496e7ca7e6ebdfc0a80e8\",\"842\":\"46cb1ab66c4f8e9259c6aaed3669003075e1dd77\",\"987\":\"19df69d241a8c447e5ff8199efb336b74e53a93e\",\"840\":\"29db3ede88da85ae11f7a92659b8f31199e8d113\",\"982\":\"7bb484b35e0aac62b9ec06f6e29c9b4884da54e9\",\"981\":\"020293ad951e2742555a3277a22f48c59d8b3426\",\"848\":\"2684b5f05c60ce0aba216fe2be739dbc2c1a2a14\",\"847\":\"99028616461ff4a13dd81413fa49a96e26d0c98a\",\"433\":\"6feedd533ed3ff647d7dbe1b74f23f0696997abd\",\"431\":\"54f4ee2f3b2dead0cb85ddc0e100c764c9867580\",\"700\":\"a44fdc39d86710cf736d8a0c9a807ed3d1fdbfeb\",\"574\":\"249bff4f6937982039b92561dd03029084f8720f\",\"704\":\"3a662497cec237bd6066f68802cc14d2c8b8e28a\",\"702\":\"d437f47977d6b33bf5524c7d9e14dddb6bd9e233\",\"578\":\"92197257c217d86d3080f9da5e18eb0faa67d225\",\"576\":\"dd3f7e1e45cf333ec9a5d1d3f58f926fbceef409\",\"577\":\"0989d5c0d807f3f33b750b8a3ade639297d03d96\",\"977\":\"c2f63685589dccb16c28f15d41dbea5c8515a695\",\"125\":\"ac05850ff6a721fa77fef69fc4f37faedbb9cdec\",\"126\":\"a3ca426c5e58dd56beca40770143a409791c038d\",\"300\":\"f8d26e3604caf958e7b40e950d5a1ef180ec9b7d\",\"302\":\"2b409da621011a16e10a8f9dd53cff671b89896e\",\"580\":\"a6ff3fa59a5a227411ddbfdad7b415790a46316f\",\"582\":\"b0a6c20d3d52b82e67d70d0fceedf604b8aeaea8\",\"581\":\"8ca1306ecfb33ec1e06abf3900974f13fb8d8582\",\"970\":\"5893e20fc1931f936ef368e983166ea1f02c4e3d\",\"973\":\"4e1c1d08809be3168df3e0181d096923d967b592\",\"129\":\"80daca0b7ab13b95d344f4da63bf3916fc093e37\",\"974\":\"de94f4e146b36af3ed036477056ddba2f86684c1\",\"445\":\"634add4b1f515e143055c51477b9c75ffc4b60bf\",\"446\":\"dd526925c3c0ecbefcee617d9e45e415478c77ef\",\"440\":\"0bbcd2702cdeab94d29d2d6dc0a585f4e48e87b8\",\"442\":\"a397c13b351a3a7ddf82f3e8691bdf0e9311ac5d\",\"586\":\"a3a7664653209a8533dc400b5d74202e2de9aa42\",\"587\":\"baf36899aba3101be879011056fac0a06ffd6544\",\"589\":\"7194dd9979cbcdd3a20c9712621f6b10358e9638\",\"861\":\"34c85212e7e9c830e6d20ddfae089fec804c291a\",\"860\":\"0dec3624ba1c646ba38dc8eee51981182ac30ba8\",\"132\":\"505117b0080729b0162be40d72ed3935f108a8ba\",\"969\":\"2cc8ea4ea4bcc90f4802636fc63df1f4b58c2c21\",\"968\":\"87b884bd9646af649c2175d5c486ecac170328db\",\"865\":\"3dc3601ae10ad7517df22c1f2f6483db4aa99479\",\"136\":\"61975d18e07c4d7aa0d29adb4718aa49515a330b\",\"862\":\"4e4c5684a1cc93553230a3e2f590506a6b376015\",\"964\":\"73ab97555e892056403b08a012fe38a4c147b970\",\"868\":\"0091d95aba18059f039763d462968540785fac1d\",\"961\":\"9e430231939c44b3cea825d2c583585cace1b4fc\",\"960\":\"6fbdac986939408d71915c19dcc5007fad0ddb4e\",\"454\":\"aab45bfdf0dfe9dfceb1523ee0489c87b9d692a8\",\"457\":\"bc346ea58b715b49522d0b397f4d9fc9b66af462\",\"458\":\"b8fdfadd36f76d86bb19889c50fdb131d2b95df0\",\"455\":\"fb94cc785b244bd7e956892f541ad118ba972bef\",\"456\":\"724a3faeb655bd4a519c38b387fd520516ff8a0c\",\"556\":\"de892423f92a7b35c71068fbf954ce8442b09838\",\"557\":\"fa28e021641d21d31499c0cbcfb8022fa3481733\",\"459\":\"2d6d0d1d5cb543f2e7e4b0f4254318a2c6c6392f\",\"554\":\"0585bee1ef3dad3882932b863b0d82204edad94a\",\"555\":\"da07b95fa1a206f61b9d609bfe1414f9b43300bd\",\"552\":\"d680a8c4c11bf91879b35b2d645caf7399e098c7\",\"553\":\"bc45c4c036bd84a2404976c971f2caed070a9573\",\"550\":\"82871dd7dfb372c7c0bb0e9991961c1457fc4d5f\",\"551\":\"8eb40d53dff9b98c8a33514988ca4262caed6f31\",\"143\":\"d44e52c4a2220029b0d42ddd73d64f6bdb417006\",\"850\":\"f438ea2ee3da6cc21f567edec329bba82a1d9b50\",\"955\":\"aa654264550c295a99100ee2e7b634207fb4c635\",\"853\":\"437ca4768fd2708a1fbc7a067816b26429bef598\",\"856\":\"deda0bf4e4454baee946450a7d7fe8bbfaa6cc6b\",\"855\":\"5eff40ab3f9cb2b998dc6b8073dc7a61c569f19c\",\"858\":\"8e365327ddf1761c8971f8b429f3569e0c3f8089\",\"952\":\"1523fdc612f3251660ae03f4163f77f262cfc2ed\",\"857\":\"cbd9bf5a2f2daa11e606bc024f4e049b9137d3b3\",\"461\":\"9354664db940e077ad1ab51942040ee05f6d7cb8\",\"460\":\"2f84d9980722f8835e72de22f4cfbdfbc0ada0a9\",\"462\":\"6afd49c02dfd0ffc38270a14ee507d18d837581e\",\"1203\":\"e97aa3a97eaf6a2e1a025008ef6cf13347ee03d0\",\"463\":\"7537c21ad46be9a5e9b27dedaf2fb7bbdef15416\",\"1206\":\"55c49cd5bbc295189343cfb1e70bb3830c9914a9\",\"1205\":\"96f1553ccc637d27c0c535d21fb5ef1538419cdc\",\"465\":\"c546892a1495241a0a6c698f0cb1bc3ccec9b10a\",\"1200\":\"aad4a05e0dfab35b38893ab4abd57605cc7ddd26\",\"466\":\"69e08fcf70ab0317eb57adc45a125c6f443a1ec2\",\"569\":\"46e3799c58d2fb2a5703282f1a642860e76c607e\",\"468\":\"b8addfff8583afe7223dc0a106c47356b0176da8\",\"1201\":\"81a796eb43e4ac8b7bbb4878ca0d49afe3ea8260\",\"565\":\"d66c56e5cf4e0ec5eb89c47a02e0794fad95760a\",\"567\":\"8cdebc5729b0348c72941d31e99ec582fdaabac6\",\"568\":\"6410380ebc80b061e8315b7acf3384e93901c71d\",\"1208\":\"544303650371fe0ccce01d32a736db0fd88b1fb7\",\"562\":\"ebdb16af10f7d9bf6f15df5c224a7c7812318e53\",\"563\":\"66ea9732cf45f26fe39c2a327fad40ee9675cc26\",\"1209\":\"d5bda03e7fabb912e76fafc91da1b99d88ca1dd8\",\"687\":\"58c7497070332b28a867f24f6e92870672339b7b\",\"686\":\"106aa27992a3a8064ac4d362072ea2e03a7717f0\",\"689\":\"596794c18abd85a682f8fa681c62900cdfbbcfde\",\"688\":\"2d86747019d95bbb73b122ece58259f068e9d0da\",\"683\":\"826f3131d1939f8bc4abd155dca1df33becfa91d\",\"682\":\"5c98b4a08418996578440acb38db62be846083e4\",\"685\":\"668362e7007f9aeba2e3ffc939d8107992297fd2\",\"684\":\"170d84aa8b57a6340b92b18cd87228f81ee3196e\",\"888\":\"c95237581dc1d7eca06752225e267a42d523931d\",\"886\":\"08997629c33eaafddcf7f9a4e960a4db3d7b1673\",\"680\":\"055ed9213f5686fc7fee0087ac1cad1f7d6caf24\",\"887\":\"2b0132b920fce5d7e36e61950027948cefcf1919\",\"880\":\"f194b3dcf1c36febce5847510a42f45d6377bed6\",\"881\":\"66c6fb2d629f854bc3fe69b67e4fa372f8f2af76\",\"678\":\"5ef68942a91296b5b996ca0c6101e3779eebfd10\",\"677\":\"eb5797e8add56390b1417e0812593be768056b51\",\"676\":\"d5544075ef8c89b2e3742160abddd5cb3696fbfe\",\"675\":\"5eb051c1b2e2efad17e40edbc592e92c711d4777\",\"673\":\"6676ec6eca5828a8dfd5daa0874913af4a579e10\",\"671\":\"f9ad6db1e93dab18e0dd7c5ee7470eee8950228c\",\"877\":\"a2cd0b720830e1cd2ec4d87428d26a30e8fbca81\",\"872\":\"d5469abe2565bf6e73b9b6ec0c19294f6b924c4b\",\"870\":\"64eb1eb063476fd86fb4a03437ceaee67d416b1b\",\"696\":\"7cc30c709f14284047f911a7f104da121555dc8e\",\"694\":\"e253bf5efbdbc105d8e419bc8bd6c3d97eb8a50c\",\"693\":\"d0d1f3a34f4e033edc838e54f85c4603d27b724e\",\"699\":\"0e84bbe12ed948012d71a35b53f9f1176897e18d\",\"698\":\"fd903ce91169f80b0b6b986ab1418c1cc78e18ba\",\"799\":\"4c23b340e46654885dd47e11109bd639f122d37f\",\"798\":\"6891a36906dc89afed8bcef3c0570a489f070519\",\"794\":\"bd58f77ff0cc7bc94ae0e82cb15de7a4033a2755\",\"792\":\"f3f3a82b3f96f656e96882dea2b7d51d2e354b3d\",\"997\":\"fda20a7523313fafd7eb8eaab584141248ad7411\",\"994\":\"b1daa73eee7445ae60e6e36ecc046e62baabc1a1\",\"899\":\"6644ca03ab61b6dc755a43434ce932f48c2e08f9\",\"891\":\"e582eb8fd03b1e846f6ef57e69cf9c078db9abd4\",\"892\":\"671c7297448be28cddcc61a33675bd58a4d518df\",\"897\":\"d44790eff4aeca8e79adcde5681882dd63101a4d\",\"691\":\"1afde719600ac0d78c053bbbbd18398047b0199b\",\"895\":\"86cd331ed38b931f75f22876c141d8dd6b85aef1\",\"896\":\"922f67569a5ea52208e2e063390562690713bb38\",\"690\":\"10ae7bdfd857667d873976b18ac5a50e82bc1d2f\",\"781\":\"6cf43e32d9cfb09e048ada36d5f527e7d0375c9f\",\"782\":\"d5edf28cd7bfc64344b1a1c30c34deaf56a9be92\",\"783\":\"8025444643f7852186e087dd21926b24a7fb67b1\",\"784\":\"71c2773243396b9cffae073ea1c5fe9407c9fa40\",\"785\":\"06223af361f420a3878843164ef07aedf503ebaa\",\"647\":\"c6338ec97da154adf036ba07af4056a9a4d8716d\",\"787\":\"d474694c32c0169c4247abb7f31874da1b211ed6\",\"788\":\"7d072da74a02468cefb43e7355dbc47bf8d208ec\",\"789\":\"c32d652bf9f9612afc0ea45652385cd4ebe0fe3a\",\"642\":\"88fb2718853063ea112c7a6c288e3b22ab54287e\",\"195\":\"00a9542594bf6999d58e5c2a598938d8f4cddd48\",\"645\":\"9eca6fa2d5bc6961b646b822b64c9d718e8aaa2e\",\"191\":\"3e7c9f0f929b73f3de46487c7c15a76e1fbed2c8\",\"193\":\"d4cb255d3aed0263ffc442f62721e24b70251023\",\"192\":\"f9a8ef8279fcd35363c40ab3a53c5111410ce5e1\",\"198\":\"4b9033d8b13de8f752078a0e7f25ee4e802f8550\",\"505\":\"2b120fe4683e6e935c91609db78753ee96cb788e\",\"773\":\"3b35d327e28fa154c51085be7e933c003e92a0f5\",\"506\":\"3e0f52b72e5bc69ec68556b3ac6c0d6e18ad933f\",\"770\":\"b0ef27969aba5ad55eca2c31f23905cc0f44dd87\",\"639\":\"645138858f6a29b0bc62c34ae3fee4fe178e404a\",\"503\":\"14dd5fad515881816397e394465acc81406627d6\",\"504\":\"0c3e585ccfdfed40a66c6d69909477d068c7bcbd\",\"509\":\"eba1d068d21363201f4d6e85387d288aa685d0e1\",\"777\":\"11b452865fe375dc9b9a87db004a1b403710a0ef\",\"638\":\"dbc3cbfb484d97ddc0fd4ab48baa3ff6c4fffead\",\"507\":\"d5eb81d9c4376c2d1f57a9bcc1a899bd11163500\",\"508\":\"11100a324fcb6fe4f5b46de4d0e762e1519e9e87\",\"633\":\"c24d940ef24ef013e4e891642e688aa78aa6d57e\",\"634\":\"0551e07f4c71ef96ba9c7a84ac8178ff4c6ee991\",\"778\":\"7f42682a62ddb41b1655048cfb356920df696ade\",\"631\":\"89442cfbb75236c00391d846bf2364ef4beef4cd\",\"779\":\"8dd177a96bcd946d30f96806f097c840eea2d36f\",\"501\":\"e2aa3af2c6f9dea3cd9cdbebf78a292ef1dab798\",\"630\":\"b137495186477da0c91485949e165bea51bed0cc\",\"502\":\"63ac041cb31de35991fe8220f2810b0a9615d7c2\",\"500\":\"63ac041cb31de35991fe8220f2810b0a9615d7c2\",\"668\":\"48e2cf54c5aa52ea64a2093d4210c56392196315\",\"764\":\"643c6d11a67586d7834b7c24398599de7fde2652\",\"669\":\"c71a5ae9eb6b101595183474973656dcc80958e3\",\"765\":\"42ef1873bcc3a85888889f30cb892211f5285f99\",\"766\":\"5a2c5a9941852b59f2793fe7ba78146816a90096\",\"760\":\"d2d5c8ff0af2047f616aa2f18f149f630004db33\",\"762\":\"745d31b1b086683a54da3c2b1312504d255d3d82\",\"660\":\"2b71fbf141af77cac2a7463596d94fe0e6891027\",\"661\":\"e78a8a13ce98e75ed91597c98d29f57d0306f2a9\",\"662\":\"b99bc8f2195c0e2780c731f1438c6cb41b1be77d\",\"663\":\"1cf34d06bbba27e561256fd3f87cc63eece0fcdb\",\"767\":\"cbec909d585953a5aa85b664632a968f65e91e18\",\"664\":\"e3fa303825001f7684ba58fe14292555d17a9dc6\",\"768\":\"617ba6cd0173cee04dc52e66c8b03862f93c9807\",\"769\":\"34c65c2441339eeb2da7ab65fc30949971922bb8\",\"754\":\"ccd320ae47e4fd2a0fd12e72db6f687310b77bd9\",\"755\":\"cbf73d9c2d7328f51b6bbb359ecc83049a47f795\",\"657\":\"f382e179b4887f85dba03613594c0c496ae2493d\",\"753\":\"d509058b64c742ac2e64b5ff05dd5372d6b6fb8a\",\"658\":\"cbc9deaff6694c4fd2dfbd78b5f940fbbc8123c9\",\"750\":\"0c8a39409ee1210db21994aeb05d6a7c20f86ce2\",\"751\":\"a21fa183dd3864ac397da0882083d79902078184\",\"650\":\"c5f6d84c339ead90df26c3c1496a941d5caf4c7c\",\"759\":\"6b2180a3d3b083b2a8e6db83833c03b4af53c949\",\"757\":\"83238e4edaa0847cca80a170ee2557a285778076\",\"1195\":\"c5c31b0052c3ef8f4f0a0d1863d792e253e2255e\",\"1198\":\"94db8a4b3591f8967cd70dacbf3b15deea00f051\",\"1192\":\"7380b83a143d8c64902d8b3a8048399d972d247e\",\"1191\":\"36315ccbd04fd6c6143f4825509cbb36c3befc5a\",\"1194\":\"95674ad8ed741192a1ea52d77119a0e5c449ed12\",\"1092\":\"0ff967bc0175b6912d519592b7e22b372de2946a\",\"1190\":\"8724f6d97a6f8e61781094196e77b2b777da39c7\",\"1095\":\"a0d5bafffffd76b58d70cf0f08bdaeb49cc34dae\",\"1094\":\"9aafbf2bc9868ee8ef543da14d4822d87f23f642\",\"1188\":\"9a6c0f21dfdc1c920aa7c1cf243e755b182576a4\",\"1189\":\"ab501d1edb225d946129db7e62957109506f1e2a\",\"1183\":\"00ede747b4c49f96c5a3185784917074b079db70\",\"1182\":\"ff2c08aeb1b19a09b6cf002ae2aa0a620c664777\",\"1181\":\"4f5673221c01d16275d897416799a424718a0b06\",\"1180\":\"06255fee65358eeccf4c17c48c7b35137ab1b998\",\"1083\":\"aab63ad1c20b3dbc37d753521e927616b9e5c454\",\"1177\":\"41e6674fdfa96dc32cc853905c869171f0715b35\",\"1075\":\"3e74f02d0c330d0d2ddc336cc40a4d0db740b6c4\",\"1074\":\"87275073ca39275fb5f281e6ad5e60a11d29ab94\",\"1077\":\"fbbf392d3e8a806ba71934e1fc56f994a084d286\"}";
        Map b = (Map) JSONUtils.fromJSON(a);
        assertThat(b.get("943"), is(not(nullValue())));
    }

    @Test
    public void shouldBuildWhitelistedBranch() throws Exception {
        GitFactory gitFactory = mock(GitFactory.class);
        GitFolderFactory gitFolderFactory = mock(GitFolderFactory.class);
        GitHubPRBuildPlugin plugin = new GitHubPRBuildPlugin(
                new GitProvider(),
                gitFactory,
                gitFolderFactory,
                mockServerFactory(),
                mockGoApplicationAccessor()
        );
        GitHubPRBuildPlugin pluginSpy = spy(plugin);

        GoPluginApiRequest request = mockRequest();
        mockGitHelperToReturnBranch(gitFactory, "test-1");

        GoPluginApiResponse response = pluginSpy.handleLatestRevisionSince(request);

        Map<String, Map<String, String>> responseBody = (Map<String, Map<String, String>>)JSONUtils.fromJSON(response.responseBody());
        assertThat(responseBody.get("scm-data").get("BRANCH_TO_REVISION_MAP"), is("{\"test-1\":\"abcdef01234567891\"}"));
        assertThat(response.responseCode(), is(200));
    }

    @Test
    public void shouldNotBuildBlacklistedBranch() throws Exception {
        GitFactory gitFactory = mock(GitFactory.class);
        GitFolderFactory gitFolderFactory = mock(GitFolderFactory.class);
        GitHubPRBuildPlugin plugin = new GitHubPRBuildPlugin(
                new GitProvider(),
                gitFactory,
                gitFolderFactory,
                mockServerFactory(),
                mockGoApplicationAccessor()
        );
        GitHubPRBuildPlugin pluginSpy = spy(plugin);

        GoPluginApiRequest request = mockRequest();
        mockGitHelperToReturnBranch(gitFactory, "master");

        GoPluginApiResponse response = pluginSpy.handleLatestRevisionSince(request);

        Map<String, Map<String, String>> responseBody = (Map<String, Map<String, String>>)JSONUtils.fromJSON(response.responseBody());
        assertThat(responseBody.get("scm-data").get("BRANCH_TO_REVISION_MAP"), is("null"));
        assertThat(response.responseCode(), is(200));
    }

    @Test
    public void shouldBuildBlacklistedBranchIfBlacklistingNotEnabled() throws Exception {
        GitFactory gitFactory = mock(GitFactory.class);
        GitFolderFactory gitFolderFactory = mock(GitFolderFactory.class);
        GitHubPRBuildPlugin plugin = new GitHubPRBuildPlugin(
                new GerritProvider(),
                gitFactory,
                gitFolderFactory,
                mockServerFactory(),
                mockGoApplicationAccessor()
        );
        GitHubPRBuildPlugin pluginSpy = spy(plugin);

        GoPluginApiRequest request = mockRequest();
        mockGitHelperToReturnBranch(gitFactory, "master");

        GoPluginApiResponse response = pluginSpy.handleLatestRevisionSince(request);

        Map<String, Map<String, String>> responseBody = (Map<String, Map<String, String>>)JSONUtils.fromJSON(response.responseBody());
        assertThat(responseBody.get("scm-data").get("BRANCH_TO_REVISION_MAP"), is("{\"master\":\"abcdef01234567891\"}"));
        assertThat(response.responseCode(), is(200));
    }

    private GoPluginApiRequest mockRequest() {
        GoPluginApiRequest request = mock(GoPluginApiRequest.class);
        when(request.requestBody()).thenReturn("{\n" +
                "    \"scm-configuration\": {\n" +
                "        \"url\": {\n" +
                "            \"value\": \"https://github.com/mdaliejaz/samplerepo.git\"\n" +
                "        },\n" +
                "        \"branchwhitelist\": {\n" +
                "            \"value\": \"test*, feat*\"\n" +
                "        },\n" +
                "        \"branchblacklist\": {\n" +
                "            \"value\": \"master\"\n" +
                "        }\n" +
                "    },\n" +
                "    \"scm-data\": {\n" +
                "        \"BRANCH_TO_REVISION_MAP\": \"{}\"\n" +
                "    },\n" +
                "    \"flyweight-folder\": \"\"\n" +
                "}\n");
        return request;
    }

    private void mockGitHelperToReturnBranch(GitFactory gitFactory, final String branch) {
        GitHelper helper = mock(GitHelper.class);
        when(gitFactory.create(any(GitConfig.class), any(File.class))).thenReturn(helper);
        when(helper.getBranchToRevisionMap(anyString())).thenReturn(new HashMap<String, String>() {{
            put(branch, "abcdef01234567891");
        }});
        when(helper.getDetailsForRevision("abcdef01234567891")).thenReturn(
                new Revision("abcdef01234567891", new Date(), "", "", "", Collections.<ModifiedFile>emptyList())
        );
    }

    private void assertPRToRevisionMap(ArgumentCaptor<Map> prStatuses) {
        assertThat(prStatuses.getValue().size(), is(2));
        assertThat(((Map<String, String>) prStatuses.getValue()).get("1"), is("12c6ef2ae9843842e4800f2c4763388db81d6ec7"));
        assertThat(((Map<String, String>) prStatuses.getValue()).get("2"), is("f985e61e556fc37f952385152d837de426b5cd8a"));
    }

    // TODO - Write proper tests for the plugin

    private void verifyValidationSuccess(String url) {
        Map request = createRequestMap(asList(new Pair("url", url)));

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

    private ServerFactory mockServerFactory() throws Exception {
        ServerFactory serverFactory = mock(ServerFactory.class);
        Server server = mock(Server.class);
        PipelineStatus status = new PipelineStatus();
        status.schedulable = true;
        when(server.getPipelineStatus(anyString())).thenReturn(status);

        when(serverFactory.getServer(any(GeneralPluginSettings.class))).thenReturn(server);
        return serverFactory;
    }

    private GoApplicationAccessor mockApplicationAccessor() {
        GoApplicationAccessor accessor = mock(GoApplicationAccessor.class);
        DefaultGoApiResponse respose = new DefaultGoApiResponse(200);
        respose.setResponseBody("{}");
        when(accessor.submit(any(GoApiRequest.class))).thenReturn(respose);
        return accessor;
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

    private GoApplicationAccessor mockGoApplicationAccessor() {
        GoApplicationAccessor accessor = mock(GoApplicationAccessor.class);
        DefaultGoApiResponse respose = new DefaultGoApiResponse(200);
        respose.setResponseBody("{}");
        when(accessor.submit(any(GoApiRequest.class))).thenReturn(respose);
        return accessor;
    }
}