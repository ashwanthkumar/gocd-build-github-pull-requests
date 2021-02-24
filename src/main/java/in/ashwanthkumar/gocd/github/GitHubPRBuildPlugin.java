package in.ashwanthkumar.gocd.github;

import com.thoughtworks.go.plugin.api.GoApplicationAccessor;
import com.thoughtworks.go.plugin.api.GoPlugin;
import com.thoughtworks.go.plugin.api.GoPluginIdentifier;
import com.thoughtworks.go.plugin.api.annotation.Extension;
import com.thoughtworks.go.plugin.api.logging.Logger;
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import com.tw.go.plugin.GitHelper;
import com.tw.go.plugin.cmd.Console;
import com.tw.go.plugin.cmd.InMemoryConsumer;
import com.tw.go.plugin.cmd.ProcessOutputStreamConsumer;
import com.tw.go.plugin.model.GitConfig;
import com.tw.go.plugin.model.ModifiedFile;
import com.tw.go.plugin.model.Revision;
import com.tw.go.plugin.util.ListUtil;
import com.tw.go.plugin.util.StringUtil;
import in.ashwanthkumar.gocd.github.provider.Provider;
import in.ashwanthkumar.gocd.github.settings.scm.PluginConfigurationView;
import in.ashwanthkumar.gocd.github.util.BranchFilter;
import in.ashwanthkumar.gocd.github.util.GitFactory;
import in.ashwanthkumar.gocd.github.util.GitFolderFactory;
import in.ashwanthkumar.gocd.github.util.JSONUtils;
import in.ashwanthkumar.utils.collections.Lists;
import in.ashwanthkumar.utils.func.Function;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.text.SimpleDateFormat;
import java.util.*;

import static in.ashwanthkumar.gocd.github.util.JSONUtils.fromJSON;
import static java.util.Arrays.asList;

@Extension
public class GitHubPRBuildPlugin implements GoPlugin {
    private static Logger LOGGER = Logger.getLoggerFor(GitHubPRBuildPlugin.class);

    public static final String EXTENSION_NAME = "scm";
    private static final List<String> goSupportedVersions = asList("1.0");

    public static final String REQUEST_SCM_CONFIGURATION = "scm-configuration";
    public static final String REQUEST_SCM_VIEW = "scm-view";
    public static final String REQUEST_VALIDATE_SCM_CONFIGURATION = "validate-scm-configuration";
    public static final String REQUEST_CHECK_SCM_CONNECTION = "check-scm-connection";
    public static final String REQUEST_PLUGIN_CONFIGURATION = "go.plugin-settings.get-configuration";
    public static final String REQUEST_PLUGIN_VIEW = "go.plugin-settings.get-view";
    public static final String REQUEST_VALIDATE_PLUGIN_CONFIGURATION = "go.plugin-settings.validate-configuration";

    public static final String GET_PLUGIN_SETTINGS = "go.processor.plugin-settings.get";

    public static final String REQUEST_LATEST_REVISION = "latest-revision";
    public static final String REQUEST_LATEST_REVISIONS_SINCE = "latest-revisions-since";
    public static final String REQUEST_CHECKOUT = "checkout";

    public static final String BRANCH_TO_REVISION_MAP = "BRANCH_TO_REVISION_MAP";
    private static final String DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    public static final int SUCCESS_RESPONSE_CODE = 200;
    public static final int NOT_FOUND_RESPONSE_CODE = 404;
    public static final int INTERNAL_ERROR_RESPONSE_CODE = 500;

    private Provider provider;
    private GitFactory gitFactory;
    private GitFolderFactory gitFolderFactory;
    private GoApplicationAccessor goApplicationAccessor;

    public GitHubPRBuildPlugin() {
        try {
            Properties properties = new Properties();
            properties.load(getClass().getResourceAsStream("/defaults.properties"));

            Class<?> providerClass = Class.forName(properties.getProperty("provider"));
            Constructor<?> constructor = providerClass.getConstructor();
            provider = (Provider) constructor.newInstance();
            gitFactory = new GitFactory();
            gitFolderFactory = new GitFolderFactory();
        } catch (Exception e) {
            throw new RuntimeException("could not create provider", e);
        }
    }

    public GitHubPRBuildPlugin(Provider provider, GitFactory gitFactory, GitFolderFactory gitFolderFactory, GoApplicationAccessor goApplicationAccessor) {
        this.provider = provider;
        this.gitFactory = gitFactory;
        this.gitFolderFactory = gitFolderFactory;
        this.goApplicationAccessor = goApplicationAccessor;
    }

    @Override
    public void initializeGoApplicationAccessor(GoApplicationAccessor goApplicationAccessor) {
        this.goApplicationAccessor = goApplicationAccessor;
    }

    @Override
    public GoPluginApiResponse handle(GoPluginApiRequest goPluginApiRequest) {
        if (goPluginApiRequest.requestName().equals(REQUEST_SCM_CONFIGURATION)) {
            return handleSCMConfiguration();
        } else if (goPluginApiRequest.requestName().equals(REQUEST_SCM_VIEW)) {
            try {
                return handleSCMView();
            } catch (IOException e) {
                String message = "Failed to find template: " + e.getMessage();
                return renderJSON(INTERNAL_ERROR_RESPONSE_CODE, message);
            }
        } else if (goPluginApiRequest.requestName().equals(REQUEST_PLUGIN_CONFIGURATION)) {
            return handlePluginConfiguration();
        } else if (goPluginApiRequest.requestName().equals(REQUEST_PLUGIN_VIEW)) {
            try {
                return handlePluginView();
            } catch (IOException e) {
                String message = "Failed to find template: " + e.getMessage();
                return renderJSON(INTERNAL_ERROR_RESPONSE_CODE, message);
            }
        }  else if (goPluginApiRequest.requestName().equals(REQUEST_VALIDATE_PLUGIN_CONFIGURATION)) {
            return handlePluginValidation(goPluginApiRequest);
        }  else if (goPluginApiRequest.requestName().equals(REQUEST_VALIDATE_SCM_CONFIGURATION)) {
            return handleSCMValidation(goPluginApiRequest);
        } else if (goPluginApiRequest.requestName().equals(REQUEST_CHECK_SCM_CONNECTION)) {
            return handleSCMCheckConnection(goPluginApiRequest);
        } else if (goPluginApiRequest.requestName().equals(REQUEST_LATEST_REVISION)) {
            return handleGetLatestRevision(goPluginApiRequest);
        } else if (goPluginApiRequest.requestName().equals(REQUEST_LATEST_REVISIONS_SINCE)) {
            return handleLatestRevisionSince(goPluginApiRequest);
        } else if (goPluginApiRequest.requestName().equals(REQUEST_CHECKOUT)) {
            return handleCheckout(goPluginApiRequest);
        }
        return renderJSON(NOT_FOUND_RESPONSE_CODE, null);
    }

    private GoPluginApiResponse handlePluginValidation(GoPluginApiRequest goPluginApiRequest) {
        return renderJSON(SUCCESS_RESPONSE_CODE, Collections.emptyList());
    }

    @Override
    public GoPluginIdentifier pluginIdentifier() {
        return new GoPluginIdentifier(EXTENSION_NAME, goSupportedVersions);
    }

    void setProvider(Provider provider) {
        this.provider = provider;
    }

    private GoPluginApiResponse handlePluginView() throws IOException {
        return getPluginView(provider, provider.getGeneralConfigurationView());
    }

    private GoPluginApiResponse handlePluginConfiguration() {
        return getPluginConfiguration(provider.getGeneralConfigurationView());
    }

    private GoPluginApiResponse handleSCMView() throws IOException {
        return getPluginView(provider, provider.getScmConfigurationView());
    }

    private GoPluginApiResponse handleSCMConfiguration() {
        return getPluginConfiguration(provider.getScmConfigurationView());
    }

    private GoPluginApiResponse getPluginView(Provider provider, PluginConfigurationView view) throws IOException {
        if (view.hasConfigurationView()) {
            Map<String, Object> response = new HashMap<String, Object>();
            response.put("displayValue", provider.getName());
            response.put("template", getFileContents(view.templateName()));
            return renderJSON(SUCCESS_RESPONSE_CODE, response);
        } else {
            return renderJSON(NOT_FOUND_RESPONSE_CODE, null);
        }
    }

    private GoPluginApiResponse getPluginConfiguration(PluginConfigurationView view) {
        Map<String, Object> response = view.fields();
        return renderJSON(SUCCESS_RESPONSE_CODE, response);
    }

    private GoPluginApiResponse handleSCMValidation(GoPluginApiRequest goPluginApiRequest) {
        Map<String, Object> requestBodyMap = (Map<String, Object>) fromJSON(goPluginApiRequest.requestBody());
        final Map<String, String> configuration = keyValuePairs(requestBodyMap, "scm-configuration");
        final GitConfig gitConfig = getGitConfig(configuration);

        List<Map<String, Object>> response = new ArrayList<Map<String, Object>>();
        validate(response, new FieldValidator() {
            @Override
            public void validate(Map<String, Object> fieldValidation) {
                validateUrl(gitConfig, fieldValidation);
            }
        });
        return renderJSON(SUCCESS_RESPONSE_CODE, response);
    }

    private GoPluginApiResponse handleSCMCheckConnection(GoPluginApiRequest goPluginApiRequest) {
        Map<String, Object> requestBodyMap = (Map<String, Object>) fromJSON(goPluginApiRequest.requestBody());
        Map<String, String> configuration = keyValuePairs(requestBodyMap, "scm-configuration");
        GitConfig gitConfig = getGitConfig(configuration);

        Map<String, Object> response = new HashMap<String, Object>();
        List<String> messages = new ArrayList<String>();

        checkConnection(gitConfig, response, messages);

        if (response.get("status") == null) {
            response.put("status", "success");
            messages.add("Could connect to URL successfully");
        }
        response.put("messages", messages);
        return renderJSON(SUCCESS_RESPONSE_CODE, response);
    }

    GoPluginApiResponse handleGetLatestRevision(GoPluginApiRequest goPluginApiRequest) {
        Map<String, Object> requestBodyMap = (Map<String, Object>) fromJSON(goPluginApiRequest.requestBody());
        Map<String, String> configuration = keyValuePairs(requestBodyMap, "scm-configuration");
        GitConfig gitConfig = getGitConfig(configuration);
        String flyweightFolder = (String) requestBodyMap.get("flyweight-folder");
        LOGGER.info(String.format("Flyweight: %s", flyweightFolder));

        try {
            GitHelper git = gitFactory.create(gitConfig, gitFolderFactory.create(flyweightFolder));
            git.cloneOrFetch(provider.getRefSpec());
            Map<String, String> branchToRevisionMap = git.getBranchToRevisionMap(provider.getRefPattern());
            Revision revision = git.getLatestRevision();
            git.submoduleUpdate();

            Map<String, Object> response = new HashMap<String, Object>();
            String defaultBranch = (StringUtils.isEmpty(gitConfig.getBranch())) ? "master" : gitConfig.getBranch();
            Map<String, Object> revisionMap = getRevisionMap(gitConfig, defaultBranch, revision);
            response.put("revision", revisionMap);
            Map<String, String> scmDataMap = new HashMap<String, String>();
            scmDataMap.put(BRANCH_TO_REVISION_MAP, JSONUtils.toJSON(branchToRevisionMap));
            response.put("scm-data", scmDataMap);
            LOGGER.info(String.format("Triggered build for " + defaultBranch + " with head at %s", revision.getRevision()));
            return renderJSON(SUCCESS_RESPONSE_CODE, response);
        } catch (Throwable t) {
            LOGGER.warn("get latest revision: ", t);
            return renderJSON(INTERNAL_ERROR_RESPONSE_CODE, removeUsernameAndPassword(t.getMessage(), gitConfig));
        }
    }

    private String removeUsernameAndPassword(String message, GitConfig gitConfig) {
        String messageForDisplay = message;
        String password = gitConfig.getPassword();
        if (StringUtils.isNotBlank(password)) {
            messageForDisplay = message.replaceAll(password, "****");
        }
        String username = gitConfig.getUsername();
        if (StringUtils.isNotBlank(username)) {
            messageForDisplay = messageForDisplay.replaceAll(username, "****");
        }
        return messageForDisplay;
    }

    GoPluginApiResponse handleLatestRevisionSince(GoPluginApiRequest goPluginApiRequest) {
        Map<String, Object> requestBodyMap = (Map<String, Object>) fromJSON(goPluginApiRequest.requestBody());
        Map<String, String> configuration = keyValuePairs(requestBodyMap, "scm-configuration");
        final GitConfig gitConfig = getGitConfig(configuration);
        Map<String, String> scmData = (Map<String, String>) requestBodyMap.get("scm-data");
        Map<String, String> oldBranchToRevisionMap = (Map<String, String>) fromJSON(scmData.get(BRANCH_TO_REVISION_MAP));
        Map<String, String> lastKnownBranchToRevisionMap = (Map<String, String>) fromJSON(scmData.get(BRANCH_TO_REVISION_MAP));
        String flyweightFolder = (String) requestBodyMap.get("flyweight-folder");
        LOGGER.debug(String.format("Fetching latest for: %s", gitConfig.getUrl()));

        try {
            GitHelper git = gitFactory.create(gitConfig, gitFolderFactory.create(flyweightFolder));
            git.cloneOrFetch(provider.getRefSpec());
            Map<String, String> newBranchToRevisionMap = git.getBranchToRevisionMap(provider.getRefPattern());
            git.submoduleUpdate();

            if (newBranchToRevisionMap.isEmpty()) {
                LOGGER.debug("No active PRs found.");
                Map<String, Object> response = new HashMap<String, Object>();
                Map<String, String> scmDataMap = new HashMap<String, String>();
                scmDataMap.put(BRANCH_TO_REVISION_MAP, JSONUtils.toJSON(newBranchToRevisionMap));
                response.put("scm-data", scmDataMap);
                return renderJSON(SUCCESS_RESPONSE_CODE, response);
            }

            Map<String, String> newerRevisions = new HashMap<String, String>();

            BranchFilter branchFilter = provider
                    .getScmConfigurationView()
                    .getBranchFilter(configuration);

            for (String branch : newBranchToRevisionMap.keySet()) {
                if (branchFilter.isBranchValid(branch)) {
                    if (branchHasNewChange(oldBranchToRevisionMap.get(branch), newBranchToRevisionMap.get(branch))) {
                        // If there are any changes we should return the only one of them.
                        // Otherwise Go.CD skips other changes (revisions) in this call.
                        // You can think about it like if we always return a minimum item
                        // of a set with comparable items.
                        String newValue = newBranchToRevisionMap.get(branch);
                        newerRevisions.put(branch, newValue);
                        oldBranchToRevisionMap.put(branch, newValue);
                        break;
                    }
                } else {
                    LOGGER.debug(String.format("Branch %s is filtered by branch matcher", branch));
                }
            }

            if (newerRevisions.isEmpty()) {
                LOGGER.debug(String.format("No updated PRs found. Old: %s New: %s", oldBranchToRevisionMap, newBranchToRevisionMap));

                Map<String, Object> response = new HashMap<String, Object>();
                Map<String, String> scmDataMap = new HashMap<String, String>();
                scmDataMap.put(BRANCH_TO_REVISION_MAP, JSONUtils.toJSON(newBranchToRevisionMap));
                response.put("scm-data", scmDataMap);
                return renderJSON(SUCCESS_RESPONSE_CODE, response);
            } else {
                LOGGER.info(String.format("new commits: %d", newerRevisions.size()));

                List<Map> revisions = new ArrayList<Map>();
                for (final String branch : newerRevisions.keySet()) {
                    String lastKnownSHA = lastKnownBranchToRevisionMap.get(branch);
                    String latestSHA = newerRevisions.get(branch);
                    if(StringUtils.isNotEmpty(lastKnownSHA)) {
                        git.resetHard(latestSHA);
                        List<Revision> allRevisionsSince;
                        try {
                            allRevisionsSince = git.getRevisionsSince(lastKnownSHA);
                        } catch (Exception e) {
                            allRevisionsSince = Collections.singletonList(git.getLatestRevision());
                        }
                        List<Map<String, Object>> changesSinceLastCommit = Lists.map(allRevisionsSince, new Function<Revision, Map<String, Object>>() {
                            @Override
                            public Map<String, Object> apply(Revision revision) {
                                return getRevisionMap(gitConfig, branch, revision);
                            }
                        });
                        revisions.addAll(changesSinceLastCommit);
                    } else {
                        Revision revision = git.getDetailsForRevision(latestSHA);
                        Map<String, Object> revisionMap = getRevisionMapForSHA(gitConfig, branch, revision);
                        revisions.add(revisionMap);
                    }
                }
                Map<String, Object> response = new HashMap<String, Object>();
                response.put("revisions", revisions);
                Map<String, String> scmDataMap = new HashMap<String, String>();
                // We shouldn't return any new branches from newBranchToRevisionMap.
                // Instead of that, we can always return the previously modified map
                // (with a newly added or with changed and existing branch), because
                // it will be the same as there are no any changes
                // (see if (newerRevisions.isEmpty()) { ... } clause)
                scmDataMap.put(BRANCH_TO_REVISION_MAP, JSONUtils.toJSON(oldBranchToRevisionMap));
                response.put("scm-data", scmDataMap);
                return renderJSON(SUCCESS_RESPONSE_CODE, response);
            }
        } catch (Throwable t) {
            LOGGER.warn("get latest revisions since: ", t);
            return renderJSON(INTERNAL_ERROR_RESPONSE_CODE, removeUsernameAndPassword(t.getMessage(), gitConfig));
        }
    }

    private Map<String, Object> getRevisionMapForSHA(GitConfig gitConfig, String branch, Revision revision) {
        // patch for building merge commits
        if (revision.isMergeCommit() && ListUtil.isEmpty(revision.getModifiedFiles())) {
            revision.setModifiedFiles(Lists.of(new ModifiedFile("/dev/null", "deleted")));
        }

        return getRevisionMap(gitConfig, branch, revision);
    }

    private boolean branchHasNewChange(String previousSHA, String latestSHA) {
        return previousSHA == null || !previousSHA.equals(latestSHA);
    }

    private GoPluginApiResponse handleCheckout(GoPluginApiRequest goPluginApiRequest) {
        Map<String, Object> requestBodyMap = (Map<String, Object>) fromJSON(goPluginApiRequest.requestBody());
        Map<String, String> configuration = keyValuePairs(requestBodyMap, "scm-configuration");
        GitConfig gitConfig = getGitConfig(configuration);
        String destinationFolder = (String) requestBodyMap.get("destination-folder");
        Map<String, Object> revisionMap = (Map<String, Object>) requestBodyMap.get("revision");
        Map<String, String> customDataBag = (Map<String, String>) revisionMap.getOrDefault("data", Collections.emptyMap());
        String revision = (String) revisionMap.get("revision");
        LOGGER.info(String.format("destination: %s. commit: %s", destinationFolder, revision));

        try {
            File workDir = gitFolderFactory.create(destinationFolder);
            GitHelper git = gitFactory.create(gitConfig, workDir);
            git.cloneOrFetch(provider.getRefSpec());

            String branch = customDataBag.getOrDefault("PR_CHECKOUT_BRANCH", "gocd-pr");
            // TODO: this should be moved into the git-cmd library, or just provide an easy way to execute
            // arbitrary git commands (without all the plumbing needed here).
            CommandLine gitCheckout = Console.createCommand(new String[]{"checkout", "-B", branch});
            ProcessOutputStreamConsumer stdOut = new ProcessOutputStreamConsumer(new InMemoryConsumer());
            ProcessOutputStreamConsumer stdErr = new ProcessOutputStreamConsumer(new InMemoryConsumer());
            Console.runOrBomb(gitCheckout, workDir, stdOut, stdErr);

            git.resetHard(revision);
            git.submoduleUpdate();

            Map<String, Object> response = new HashMap<String, Object>();
            response.put("status", "success");
            response.put("messages", Arrays.asList(String.format("Checked out to revision %s", revision)));

            return renderJSON(SUCCESS_RESPONSE_CODE, response);
        } catch (Throwable t) {
            LOGGER.warn("checkout: ", t);
            return renderJSON(INTERNAL_ERROR_RESPONSE_CODE, t.getMessage());
        }
    }

    GitConfig getGitConfig(Map<String, String> configuration) {
        GitConfig gitConfig = new GitConfig(
                configuration.get("url"),
                configuration.get("username"),
                configuration.get("password"),
                StringUtils.trimToNull(configuration.get("defaultBranch")),
                true,
                Boolean.parseBoolean(configuration.get("shallowClone")));
        provider.addConfigData(gitConfig);
        return gitConfig;
    }

    private void validate(List<Map<String, Object>> response, FieldValidator fieldValidator) {
        Map<String, Object> fieldValidation = new HashMap<String, Object>();
        fieldValidator.validate(fieldValidation);
        if (!fieldValidation.isEmpty()) {
            response.add(fieldValidation);
        }
    }

    Map<String, Object> getRevisionMap(GitConfig gitConfig, String branch, Revision revision) {
        Map<String, Object> response = new HashMap<String, Object>();
        response.put("revision", revision.getRevision());
        response.put("user", revision.getUser());
        response.put("timestamp", new SimpleDateFormat(DATE_PATTERN).format(revision.getTimestamp()));
        response.put("revisionComment", revision.getComment());
        List<Map> modifiedFilesMapList = new ArrayList<Map>();
        if (!ListUtil.isEmpty(revision.getModifiedFiles())) {
            for (ModifiedFile modifiedFile : revision.getModifiedFiles()) {
                Map<String, String> modifiedFileMap = new HashMap<String, String>();
                modifiedFileMap.put("fileName", modifiedFile.getFileName());
                modifiedFileMap.put("action", modifiedFile.getAction());
                modifiedFilesMapList.add(modifiedFileMap);
            }
        }
        response.put("modifiedFiles", modifiedFilesMapList);
        Map<String, String> customDataBag = new HashMap<String, String>();
        provider.populateRevisionData(gitConfig, branch, revision.getRevision(), customDataBag);

        // Don't use "pr" because at least for Bitbucke there are already pr/ git refs.
        // TODO: for providers that return the source branch, it should use that.
        String checkoutBranch = "gocd-pr";
        if (customDataBag.containsKey("PR_ID")) {
            checkoutBranch += "/" + customDataBag.get("PR_ID");
        }
        customDataBag.put("PR_CHECKOUT_BRANCH", checkoutBranch);

        response.put("data", customDataBag);
        return response;
    }

    private Map<String, String> keyValuePairs(Map<String, Object> requestBodyMap, String mainKey) {
        Map<String, String> keyValuePairs = new HashMap<String, String>();
        Map<String, Object> fieldsMap = (Map<String, Object>) requestBodyMap.get(mainKey);
        for (String field : fieldsMap.keySet()) {
            Map<String, Object> fieldProperties = (Map<String, Object>) fieldsMap.get(field);
            String value = (String) fieldProperties.get("value");
            keyValuePairs.put(field, value);
        }
        return keyValuePairs;
    }

    public void validateUrl(GitConfig gitConfig, Map<String, Object> fieldMap) {
        if (StringUtil.isEmpty(gitConfig.getUrl())) {
            fieldMap.put("key", "url");
            fieldMap.put("message", "URL is a required field");
        } else if (!provider.isValidURL(gitConfig.getUrl())) {
            fieldMap.put("key", "url");
            fieldMap.put("message", "Invalid URL");
        }
    }

    public void checkConnection(GitConfig gitConfig, Map<String, Object> response, List<String> messages) {
        if (StringUtil.isEmpty(gitConfig.getUrl())) {
            response.put("status", "failure");
            messages.add("URL is empty");
        } else if (!provider.isValidURL(gitConfig.getUrl())) {
            response.put("status", "failure");
            messages.add("Invalid URL");
        } else {
            try {
                provider.checkConnection(gitConfig);
            } catch (Exception e) {
                response.put("status", "failure");
                messages.add(e.getMessage());
            }
        }
    }

    private String getFileContents(String filePath) throws IOException {
        return IOUtils.toString(getClass().getResourceAsStream(filePath), "UTF-8");
    }

    GoPluginApiResponse renderJSON(final int responseCode, Object response) {
        final String json = response == null ? null : JSONUtils.toJSON(response);
        return new GoPluginApiResponse() {
            @Override
            public int responseCode() {
                return responseCode;
            }

            @Override
            public Map<String, String> responseHeaders() {
                return null;
            }

            @Override
            public String responseBody() {
                return json;
            }
        };
    }

}
