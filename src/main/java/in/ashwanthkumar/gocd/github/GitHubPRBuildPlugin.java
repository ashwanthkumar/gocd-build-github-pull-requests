package in.ashwanthkumar.gocd.github;

import com.thoughtworks.go.plugin.api.GoApplicationAccessor;
import com.thoughtworks.go.plugin.api.GoPlugin;
import com.thoughtworks.go.plugin.api.GoPluginIdentifier;
import com.thoughtworks.go.plugin.api.annotation.Extension;
import com.thoughtworks.go.plugin.api.logging.Logger;
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import com.tw.go.plugin.GitHelper;
import com.tw.go.plugin.model.GitConfig;
import com.tw.go.plugin.model.ModifiedFile;
import com.tw.go.plugin.model.Revision;
import com.tw.go.plugin.util.ListUtil;
import com.tw.go.plugin.util.StringUtil;
import in.ashwanthkumar.gocd.github.provider.Provider;
import in.ashwanthkumar.gocd.github.util.*;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.text.SimpleDateFormat;
import java.util.*;

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

    public GitHubPRBuildPlugin(Provider provider, GitFactory gitFactory, GitFolderFactory gitFolderFactory) {
        this.provider = provider;
        this.gitFactory = gitFactory;
        this.gitFolderFactory = gitFolderFactory;
    }

    @Override
    public void initializeGoApplicationAccessor(GoApplicationAccessor goApplicationAccessor) {
        // ignore
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
        } else if (goPluginApiRequest.requestName().equals(REQUEST_VALIDATE_SCM_CONFIGURATION)) {
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

    @Override
    public GoPluginIdentifier pluginIdentifier() {
        return new GoPluginIdentifier(EXTENSION_NAME, goSupportedVersions);
    }

    void setProvider(Provider provider) {
        this.provider = provider;
    }

    private GoPluginApiResponse handleSCMConfiguration() {
        Map<String, Object> response = new HashMap<String, Object>();
        response.put("url", createField("URL", null, true, true, false, "0"));
        response.put("username", createField("Username", null, false, false, false, "1"));
        response.put("password", createField("Password", null, false, false, true, "2"));
        response.put("branchwhitelist", createField("Whitelisted branches", "", false, false, false, "3"));
        response.put("branchblacklist", createField("Blacklisted branches", "", false, false, false, "4"));
        return renderJSON(SUCCESS_RESPONSE_CODE, response);
    }

    private GoPluginApiResponse handleSCMView() throws IOException {
        Map<String, Object> response = new HashMap<String, Object>();
        response.put("displayValue", provider.getName());
        response.put("template", getFileContents("/scm.template.html"));
        return renderJSON(SUCCESS_RESPONSE_CODE, response);
    }

    private GoPluginApiResponse handleSCMValidation(GoPluginApiRequest goPluginApiRequest) {
        Map<String, Object> requestBodyMap = (Map<String, Object>) JSONUtils.fromJSON(goPluginApiRequest.requestBody());
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
        Map<String, Object> requestBodyMap = (Map<String, Object>) JSONUtils.fromJSON(goPluginApiRequest.requestBody());
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
        Map<String, Object> requestBodyMap = (Map<String, Object>) JSONUtils.fromJSON(goPluginApiRequest.requestBody());
        Map<String, String> configuration = keyValuePairs(requestBodyMap, "scm-configuration");
        GitConfig gitConfig = getGitConfig(configuration);
        String flyweightFolder = (String) requestBodyMap.get("flyweight-folder");
        LOGGER.info(String.format("Flyweight: %s", flyweightFolder));

        try {
            GitHelper git = gitFactory.create(gitConfig, gitFolderFactory.create(flyweightFolder));
            git.cloneOrFetch(provider.getRefSpec());
            Map<String, String> branchToRevisionMap = git.getBranchToRevisionMap(provider.getRefPattern());
            Revision revision = git.getLatestRevision();

            Map<String, Object> response = new HashMap<String, Object>();
            Map<String, Object> revisionMap = getRevisionMap(gitConfig, "master", revision);
            response.put("revision", revisionMap);
            Map<String, String> scmDataMap = new HashMap<String, String>();
            scmDataMap.put(BRANCH_TO_REVISION_MAP, JSONUtils.toJSON(branchToRevisionMap));
            response.put("scm-data", scmDataMap);
            LOGGER.info(String.format("Triggered build for master with head at %s", revision.getRevision()));
            return renderJSON(SUCCESS_RESPONSE_CODE, response);
        } catch (Throwable t) {
            LOGGER.warn("get latest revision: ", t);
            return renderJSON(INTERNAL_ERROR_RESPONSE_CODE, t.getMessage());
        }
    }

    GoPluginApiResponse handleLatestRevisionSince(GoPluginApiRequest goPluginApiRequest) {
        Map<String, Object> requestBodyMap = (Map<String, Object>) JSONUtils.fromJSON(goPluginApiRequest.requestBody());
        Map<String, String> configuration = keyValuePairs(requestBodyMap, "scm-configuration");
        GitConfig gitConfig = getGitConfig(configuration);
        Map<String, String> scmData = (Map<String, String>) requestBodyMap.get("scm-data");
        Map<String, String> oldBranchToRevisionMap = (Map<String, String>) JSONUtils.fromJSON(scmData.get(BRANCH_TO_REVISION_MAP));
        String flyweightFolder = (String) requestBodyMap.get("flyweight-folder");
        LOGGER.debug(String.format("Fetching latest for: %s", gitConfig.getUrl()));

        try {
            GitHelper git = gitFactory.create(gitConfig, gitFolderFactory.create(flyweightFolder));
            git.cloneOrFetch(provider.getRefSpec());
            Map<String, String> newBranchToRevisionMap = git.getBranchToRevisionMap(provider.getRefPattern());

            if (newBranchToRevisionMap.isEmpty()) {
                LOGGER.debug("No active PRs found.");
                Map<String, Object> response = new HashMap<String, Object>();
                Map<String, String> scmDataMap = new HashMap<String, String>();
                scmDataMap.put(BRANCH_TO_REVISION_MAP, JSONUtils.toJSON(newBranchToRevisionMap));
                response.put("scm-data", scmDataMap);
                return renderJSON(SUCCESS_RESPONSE_CODE, response);
            }

            Map<String, String> newerRevisions = new HashMap<String, String>();

            BranchFilter branchFilter = new BranchFilter(configuration.get("branchblacklist"), configuration.get("branchwhitelist"));

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
                    LOGGER.debug(String.format("Branch %s is blacklisted", branch));
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
                for (String branch : newerRevisions.keySet()) {
                    String latestSHA = newerRevisions.get(branch);
                    Revision revision = git.getDetailsForRevision(latestSHA);

                    Map<String, Object> revisionMap = getRevisionMap(gitConfig, branch, revision);
                    revisions.add(revisionMap);
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
            return renderJSON(INTERNAL_ERROR_RESPONSE_CODE, t.getMessage());
        }
    }

    private boolean branchHasNewChange(String previousSHA, String latestSHA) {
        return previousSHA == null || !previousSHA.equals(latestSHA);
    }

    private GoPluginApiResponse handleCheckout(GoPluginApiRequest goPluginApiRequest) {
        Map<String, Object> requestBodyMap = (Map<String, Object>) JSONUtils.fromJSON(goPluginApiRequest.requestBody());
        Map<String, String> configuration = keyValuePairs(requestBodyMap, "scm-configuration");
        GitConfig gitConfig = getGitConfig(configuration);
        String destinationFolder = (String) requestBodyMap.get("destination-folder");
        Map<String, Object> revisionMap = (Map<String, Object>) requestBodyMap.get("revision");
        String revision = (String) revisionMap.get("revision");
        LOGGER.info(String.format("destination: %s. commit: %s", destinationFolder, revision));

        try {
            GitHelper git = gitFactory.create(gitConfig, gitFolderFactory.create(destinationFolder));
            git.cloneOrFetch(provider.getRefSpec());
            git.resetHard(revision);

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
        GitConfig gitConfig = new GitConfig(configuration.get("url"), configuration.get("username"), configuration.get("password"), null);
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

    private Map<String, Object> createField(String displayName, String defaultValue, boolean isPartOfIdentity, boolean isRequired, boolean isSecure, String displayOrder) {
        Map<String, Object> fieldProperties = new HashMap<String, Object>();
        fieldProperties.put("display-name", displayName);
        fieldProperties.put("default-value", defaultValue);
        fieldProperties.put("part-of-identity", isPartOfIdentity);
        fieldProperties.put("required", isRequired);
        fieldProperties.put("secure", isSecure);
        fieldProperties.put("display-order", displayOrder);
        return fieldProperties;
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
