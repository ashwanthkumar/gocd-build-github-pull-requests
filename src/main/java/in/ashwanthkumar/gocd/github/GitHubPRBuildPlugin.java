package in.ashwanthkumar.gocd.github;

import com.google.gson.GsonBuilder;
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
import com.tw.go.plugin.util.StringUtil;
import in.ashwanthkumar.gocd.github.json.JSONUtils;
import in.ashwanthkumar.gocd.github.model.PullRequestStatus;
import in.ashwanthkumar.utils.func.Function;
import org.apache.commons.io.IOUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.GitHub;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

import static in.ashwanthkumar.gocd.github.GHUtils.buildGithubFromPropertyFile;
import static in.ashwanthkumar.gocd.github.GitConstants.PR_FETCH_REFSPEC;
import static in.ashwanthkumar.gocd.github.GitConstants.PR_MERGE_PREFIX;
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
        return renderJSON(SUCCESS_RESPONSE_CODE, response);
    }

    private GoPluginApiResponse handleSCMView() throws IOException {
        Map<String, Object> response = new HashMap<String, Object>();
        response.put("displayValue", "Github");
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
        ArrayList<String> messages = new ArrayList<String>();
        try {
            GitHub.connect().getRepository(GHUtils.parseGithubUrl(configuration.get("url")));
            response.put("status", "success");
            messages.add("Could connect to URL successfully");
        } catch (IOException e) {
            response.put("status", "failure");
            messages.add("Could not connect to URL");
        } catch (Exception e) {
            response.put("status", "failure");
            messages.add(e.getMessage());
        }

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
            git.cloneOrFetch(PR_FETCH_REFSPEC);
            Revision revision = git.getLatestRevision();
            Map<String, String> prToRevisionMap = git.getBranchToRevisionMap(PR_MERGE_PREFIX);

            Map<String, Object> revisionMap = getRevisionMap(revision, prToRevisionMap, null);
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
        Map<String, String> prStatuses = JSONUtils.fromJson((String) ((Map<String, Object>) previousRevisionMap.get("data")).get("ACTIVE_PULL_REQUESTS"), Map.class);

        try {
            GitHelper git = HelperFactory.git(gitConfig, new File(flyweightFolder));
            LOGGER.debug("handleLatestRevisionSince# - Cloning / Fetching the latest for " + gitConfig.getUrl());
            git.cloneOrFetch(PR_FETCH_REFSPEC);
            Map<String, String> prToRevisionMap = git.getBranchToRevisionMap(PR_MERGE_PREFIX);
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
                LOGGER.warn("new commits: " + newerRevisions.size());

                Map<String, Object> response = new HashMap<String, Object>();
                List<Map> revisions = new ArrayList<Map>();
                for (String prId : newerRevisions.keySet()) {
                    int pullRequestID = Integer.parseInt(prId);
                    String latestSHA = newerRevisions.get(prId);
                    Revision revision = git.getDetailsForRevision(latestSHA);
                    PullRequestStatus currentPR = transformGHPullRequestToPullRequestStatus(latestSHA).apply(pullRequestFrom(gitConfig, pullRequestID));

                    Map<String, Object> revisionMap = getRevisionMap(revision, prToRevisionMap, currentPR);
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
            git.cloneOrFetch(PR_FETCH_REFSPEC);
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

    private GHPullRequest pullRequestFrom(GitConfig gitConfig, int currentPullRequestID) throws IOException {
        return buildGithubFromPropertyFile()
                .getRepository(GHUtils.parseGithubUrl(gitConfig.getUrl()))
                .getPullRequest(currentPullRequestID);
    }

    private Function<GHPullRequest, PullRequestStatus> transformGHPullRequestToPullRequestStatus(final String mergedSHA) {
        return new Function<GHPullRequest, PullRequestStatus>() {
            @Override
            public PullRequestStatus apply(GHPullRequest input) {
                int prID = GHUtils.prIdFrom(input.getDiffUrl().toString());
                try {
                    GHUser user = input.getUser();
                    return new PullRequestStatus(prID, input.getHead().getSha(), mergedSHA, input.getHead().getLabel(),
                            input.getBase().getLabel(), input.getHtmlUrl().toString(), user.getName(),
                            user.getEmail(), input.getBody(), input.getTitle());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    private GitConfig getGitConfig(Map<String, String> configuration) {
        return new GitConfig(configuration.get("url"));
    }

    private void validate(List<Map<String, Object>> response, FieldValidator fieldValidator) {
        Map<String, Object> fieldValidation = new HashMap<String, Object>();
        fieldValidator.validate(fieldValidation);
        if (!fieldValidation.isEmpty()) {
            response.add(fieldValidation);
        }
    }

    Map<String, Object> getRevisionMap(Revision revision, Map<String, String> prStatuses, PullRequestStatus currentPR) {
        Map<String, Object> response = new HashMap<String, Object>();
        response.put("revision", revision.getRevision());
        response.put("user", revision.getUser());
        response.put("timestamp", new SimpleDateFormat(DATE_PATTERN).format(revision.getTimestamp()));
        response.put("revisionComment", revision.getComment());
        List<Map> modifiedFilesMapList = new ArrayList<Map>();
        for (ModifiedFile modifiedFile : revision.getModifiedFiles()) {
            Map<String, String> modifiedFileMap = new HashMap<String, String>();
            modifiedFileMap.put("fileName", modifiedFile.getFileName());
            modifiedFileMap.put("action", modifiedFile.getAction());
            modifiedFilesMapList.add(modifiedFileMap);
        }
        response.put("modifiedFiles", modifiedFilesMapList);
        Map<String, String> customDataBag = new HashMap<String, String>();
        customDataBag.put("ACTIVE_PULL_REQUESTS", JSONUtils.toJson(prStatuses));
        if (currentPR != null) {
            customDataBag.put("PR_ID", String.valueOf(currentPR.getId()));
            customDataBag.put("PR_BRANCH", String.valueOf(currentPR.getPrBranch()));
            customDataBag.put("TARGET_BRANCH", String.valueOf(currentPR.getToBranch()));
            customDataBag.put("PR_URL", String.valueOf(currentPR.getUrl()));
            customDataBag.put("PR_AUTHOR", currentPR.getAuthor());
            customDataBag.put("PR_AUTHOR_EMAIL", currentPR.getAuthorEmail());
            customDataBag.put("PR_DESCRIPTION", currentPR.getDescription());
            customDataBag.put("PR_TITLE", currentPR.getTitle());
        }
        response.put("data", customDataBag);
        return response;
    }

    private Map<String, Object> getMapFor(GoPluginApiRequest goPluginApiRequest, String field) {
        Map<String, Object> map = (Map<String, Object>) new GsonBuilder().create().fromJson(goPluginApiRequest.requestBody(), Object.class);
        Map<String, Object> fieldProperties = (Map<String, Object>) map.get(field);
        return fieldProperties;
    }

    private Object getValueFor(GoPluginApiRequest goPluginApiRequest, String field) {
        Map<String, Object> map = (Map<String, Object>) new GsonBuilder().create().fromJson(goPluginApiRequest.requestBody(), Object.class);
        return map.get(field);
    }

    private Map<String, String> keyValuePairs(GoPluginApiRequest goPluginApiRequest, String mainKey) {
        Map<String, String> keyValuePairs = new HashMap<String, String>();
        Map<String, Object> map = (Map<String, Object>) new GsonBuilder().create().fromJson(goPluginApiRequest.requestBody(), Object.class);
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
        } else if (!isValidURL(gitConfig)) {
            fieldMap.put("key", "url");
            fieldMap.put("message", "Invalid URL");
        }
    }

    public void checkConnection(GitConfig gitConfig, Map<String, Object> response, ArrayList<String> messages) {
        if (StringUtil.isEmpty(gitConfig.getUrl())) {
            response.put("status", "failure");
            messages.add("URL is empty");
        } else if (!isValidURL(gitConfig)) {
            response.put("status", "failure");
            messages.add("Invalid URL");
        } else {
            try {
                GitHub.connect().getRepository(GHUtils.parseGithubUrl(gitConfig.getUrl()));
            } catch (Exception e) {
                response.put("status", "failure");
                messages.add(e.getMessage());
            }
        }
    }

    boolean isValidURL(GitConfig gitConfig) {
        return new UrlValidator(UrlValidator.ALLOW_LOCAL_URLS).isValid(gitConfig.getUrl()) && GHUtils.isValidGHUrl(gitConfig.getUrl());
    }

    GoPluginApiResponse renderJSON(final int responseCode, Object response) {
        final String json = response == null ? null : JSONUtils.toJson(response);
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
