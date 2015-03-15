package in.ashwanthkumar.gocd.github;

import com.thoughtworks.go.plugin.api.GoApplicationAccessor;
import com.thoughtworks.go.plugin.api.GoPlugin;
import com.thoughtworks.go.plugin.api.GoPluginIdentifier;
import com.thoughtworks.go.plugin.api.annotation.Extension;
import com.thoughtworks.go.plugin.api.logging.Logger;
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import com.tw.go.plugin.GitHelper;
import com.tw.go.plugin.HelperFactory;
import com.tw.go.plugin.model.GitConfig;
import com.tw.go.plugin.model.ModifiedFile;
import com.tw.go.plugin.model.Revision;
import com.tw.go.plugin.util.ListUtil;
import com.tw.go.plugin.util.StringUtil;
import in.ashwanthkumar.gocd.github.provider.Provider;
import in.ashwanthkumar.gocd.github.provider.github.GitHubProvider;
import in.ashwanthkumar.gocd.github.util.JSONUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

import static java.util.Arrays.asList;

@Extension
public class GitHubPRBuildPlugin implements GoPlugin {
    public static final String EXTENSION_NAME = "scm";
    public static final String REQUEST_SCM_CONFIGURATION = "scm-configuration";
    public static final String REQUEST_SCM_VIEW = "scm-view";
    public static final String REQUEST_VALIDATE_SCM_CONFIGURATION = "validate-scm-configuration";
    public static final String REQUEST_CHECK_SCM_CONNECTION = "check-scm-connection";
    public static final String REQUEST_LATEST_REVISION = "latest-revision";
    public static final String REQUEST_LATEST_REVISIONS_SINCE = "latest-revisions-since";
    public static final String REQUEST_CHECKOUT = "checkout";
    public static final int SUCCESS_RESPONSE_CODE = 200;
    public static final int INTERNAL_ERROR_RESPONSE_CODE = 500;
    private static final List<String> goSupportedVersions = asList("1.0");
    private static final String DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    private static Logger LOGGER = Logger.getLoggerFor(GitHubPRBuildPlugin.class);

    private Provider provider;

    public GitHubPRBuildPlugin() {
        provider = new GitHubProvider();
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
                return renderJSON(500, message);
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
        return null;
    }

    @Override
    public GoPluginIdentifier pluginIdentifier() {
        return new GoPluginIdentifier(EXTENSION_NAME, goSupportedVersions);
    }

    private GoPluginApiResponse handleSCMConfiguration() {
        Map<String, Object> response = new HashMap<String, Object>();
        response.put("url", createField("URL", null, true, true, false, "0"));
        response.put("username", createField("Username", null, false, false, false, "1"));
        response.put("password", createField("Password", null, false, false, true, "2"));
        return renderJSON(SUCCESS_RESPONSE_CODE, response);
    }

    private GoPluginApiResponse handleSCMView() throws IOException {
        Map<String, Object> response = new HashMap<String, Object>();
        response.put("displayValue", provider.getName());
        response.put("template", IOUtils.toString(getClass().getResourceAsStream("/views/scm.template.html"), "UTF-8"));
        return renderJSON(SUCCESS_RESPONSE_CODE, response);
    }

    private GoPluginApiResponse handleSCMValidation(GoPluginApiRequest goPluginApiRequest) {
        final Map<String, String> configuration = keyValuePairs(goPluginApiRequest, "scm-configuration");
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
        Map<String, String> configuration = keyValuePairs(goPluginApiRequest, "scm-configuration");
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
        Map<String, String> configuration = keyValuePairs(goPluginApiRequest, "scm-configuration");
        GitConfig gitConfig = getGitConfig(configuration);
        String flyweightFolder = (String) getValueFor(goPluginApiRequest, "flyweight-folder");
        LOGGER.debug("flyweight: " + flyweightFolder);

        try {
            GitHelper git = HelperFactory.git(gitConfig, new File(flyweightFolder));
            git.cloneOrFetch(provider.getRefSpec());
            Map<String, String> prToRevisionMap = git.getBranchToRevisionMap(provider.getRefPattern());
            Revision revision = git.getLatestRevision();

            Map<String, Object> revisionMap = getRevisionMap(gitConfig, "master", revision, prToRevisionMap);
            LOGGER.info("Triggered build for master with head at " + revision.getRevision());
            return renderJSON(SUCCESS_RESPONSE_CODE, revisionMap);
        } catch (Throwable t) {
            LOGGER.warn("get latest revision: ", t);
            return renderJSON(INTERNAL_ERROR_RESPONSE_CODE, t.getMessage());
        }
    }

    GoPluginApiResponse handleLatestRevisionSince(GoPluginApiRequest goPluginApiRequest) {
        Map<String, String> configuration = keyValuePairs(goPluginApiRequest, "scm-configuration");
        GitConfig gitConfig = getGitConfig(configuration);
        String flyweightFolder = (String) getValueFor(goPluginApiRequest, "flyweight-folder");
        Map<String, Object> previousRevisionMap = getMapFor(goPluginApiRequest, "previous-revision");
        Map<String, String> prStatuses = (Map<String, String>) JSONUtils.fromJSON((String) ((Map<String, Object>) previousRevisionMap.get("data")).get("ACTIVE_PULL_REQUESTS"));
        LOGGER.debug("handleLatestRevisionSince# - Cloning / Fetching the latest for " + gitConfig.getUrl());

        try {
            GitHelper git = HelperFactory.git(gitConfig, new File(flyweightFolder));
            git.cloneOrFetch(provider.getRefSpec());
            Map<String, String> prToRevisionMap = git.getBranchToRevisionMap(provider.getRefPattern());

            if (prToRevisionMap.isEmpty()) {
                LOGGER.debug("handleLatestRevisionSince# - No active PRs found. We're good here.");
                return renderJSON(SUCCESS_RESPONSE_CODE, null);
            }

            Map<String, String> newerRevisions = new HashMap<String, String>();
            for (String prId : prToRevisionMap.keySet()) {
                if (prHasNewChange(prStatuses.get(prId), prToRevisionMap.get(prId))) {
                    newerRevisions.put(prId, prToRevisionMap.get(prId));
                }
            }

            if (newerRevisions.isEmpty()) {
                return renderJSON(SUCCESS_RESPONSE_CODE, null);
            } else {
                LOGGER.info("new commits: " + newerRevisions.size());

                Map<String, Object> response = new HashMap<String, Object>();
                List<Map> revisions = new ArrayList<Map>();
                for (String prId : newerRevisions.keySet()) {
                    String latestSHA = newerRevisions.get(prId);
                    Revision revision = git.getDetailsForRevision(latestSHA);

                    Map<String, Object> revisionMap = getRevisionMap(gitConfig, prId, revision, prToRevisionMap);
                    revisions.add(revisionMap);
                }
                response.put("revisions", revisions);
                return renderJSON(SUCCESS_RESPONSE_CODE, response);
            }
        } catch (Throwable t) {
            LOGGER.warn("get latest revisions since: ", t);
            return renderJSON(INTERNAL_ERROR_RESPONSE_CODE, t.getMessage());
        }
    }

    private boolean prHasNewChange(String previousSHA, String latestSHA) {
        return previousSHA == null || !previousSHA.equals(latestSHA);
    }

    private GoPluginApiResponse handleCheckout(GoPluginApiRequest goPluginApiRequest) {
        Map<String, String> configuration = keyValuePairs(goPluginApiRequest, "scm-configuration");
        GitConfig gitConfig = getGitConfig(configuration);
        String destinationFolder = (String) getValueFor(goPluginApiRequest, "destination-folder");
        Map<String, Object> revisionMap = getMapFor(goPluginApiRequest, "revision");
        String revision = (String) revisionMap.get("revision");
        LOGGER.warn("destination: " + destinationFolder + ". commit: " + revision);

        try {
            GitHelper git = HelperFactory.git(gitConfig, new File(destinationFolder));
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

    Map<String, Object> getRevisionMap(GitConfig gitConfig, String prId, Revision revision, Map<String, String> prStatuses) {
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
        customDataBag.put("ACTIVE_PULL_REQUESTS", JSONUtils.toJSON(prStatuses));
        provider.populateRevisionData(gitConfig, prId, revision.getRevision(), customDataBag);
        response.put("data", customDataBag);
        return response;
    }

    private Map<String, Object> getMapFor(GoPluginApiRequest goPluginApiRequest, String field) {
        Map<String, Object> map = (Map<String, Object>) JSONUtils.fromJSON(goPluginApiRequest.requestBody());
        Map<String, Object> fieldProperties = (Map<String, Object>) map.get(field);
        return fieldProperties;
    }

    private Object getValueFor(GoPluginApiRequest goPluginApiRequest, String field) {
        Map<String, Object> map = (Map<String, Object>) JSONUtils.fromJSON(goPluginApiRequest.requestBody());
        return map.get(field);
    }

    private Map<String, String> keyValuePairs(GoPluginApiRequest goPluginApiRequest, String mainKey) {
        Map<String, String> keyValuePairs = new HashMap<String, String>();
        Map<String, Object> map = (Map<String, Object>) JSONUtils.fromJSON(goPluginApiRequest.requestBody());
        Map<String, Object> fieldsMap = (Map<String, Object>) map.get(mainKey);
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
