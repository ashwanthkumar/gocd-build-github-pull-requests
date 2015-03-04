package in.ashwanthkumar.gocd.github;

import com.google.gson.GsonBuilder;
import com.thoughtworks.go.plugin.api.GoApplicationAccessor;
import com.thoughtworks.go.plugin.api.GoPlugin;
import com.thoughtworks.go.plugin.api.GoPluginIdentifier;
import com.thoughtworks.go.plugin.api.annotation.Extension;
import com.thoughtworks.go.plugin.api.logging.Logger;
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import in.ashwanthkumar.gocd.github.json.JSONUtils;
import in.ashwanthkumar.gocd.github.model.*;
import in.ashwanthkumar.utils.collections.Lists;
import in.ashwanthkumar.utils.collections.Maps;
import in.ashwanthkumar.utils.func.Function;
import in.ashwanthkumar.utils.lang.option.Option;
import org.apache.commons.io.IOUtils;
import org.eclipse.jgit.lib.Ref;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.GitHub;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static in.ashwanthkumar.gocd.github.GHUtils.buildGithubFromPropertyFile;
import static in.ashwanthkumar.gocd.github.GHUtils.pullRequestIdFromRef;
import static in.ashwanthkumar.gocd.github.GitConstants.PR_FETCH_REFSPEC;
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

    private static final String DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    public static final int SUCCESS_RESPONSE_CODE = 200;
    public static final int INTERNAL_ERROR_RESPONSE_CODE = 500;

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

        List<Map<String, Object>> response = new ArrayList<Map<String, Object>>();
        validate(response, new FieldValidator() {
            @Override
            public void validate(Map<String, Object> fieldValidation) {
                String url = configuration.get("url");
                if (url == null || url.trim().isEmpty() || !GHUtils.isValidGHUrl(url)) {
                    fieldValidation.put("key", "url");
                    fieldValidation.put("message", "Either url is empty / invalid.");
                }
            }
        });
        return renderJSON(SUCCESS_RESPONSE_CODE, response);
    }

    private GoPluginApiResponse handleSCMCheckConnection(GoPluginApiRequest goPluginApiRequest) {
        Map<String, String> configuration = keyValuePairs(goPluginApiRequest, "scm-configuration");

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

        response.put("messages", messages);
        return renderJSON(SUCCESS_RESPONSE_CODE, response);
    }

    private GoPluginApiResponse handleGetLatestRevision(GoPluginApiRequest goPluginApiRequest) {
        Map<String, String> configuration = keyValuePairs(goPluginApiRequest, "scm-configuration");
        String url = configuration.get("url");
        String flyweightFolder = (String) getValueFor(goPluginApiRequest, "flyweight-folder");

        LOGGER.debug("flyweight: " + flyweightFolder);

        try {
            JGitHelper jGit = new JGitHelper();
            jGit.cloneOrFetch(url, flyweightFolder);
            Iterable<Ref> refs = jGit.refs(flyweightFolder);
            String refsAvailableInTheRepository = Lists.mkString(refs);
            LOGGER.info("Available refs - " + refsAvailableInTheRepository);
            jGit.fetchRepository(url, flyweightFolder, PR_FETCH_REFSPEC);
            MergeRefs mergeRefs = jGit.findMergeRef(flyweightFolder);
            if (mergeRefs.isEmpty()) {
                LOGGER.debug("handleGetLatestRevision# - No active PRs found. We're good");
                return renderJSON(SUCCESS_RESPONSE_CODE, null);
            }
            Ref currentPullRequestRef = mergeRefs.head();
            int currentPullRequestID = pullRequestIdFromRef(currentPullRequestRef);
            PullRequestStatus currentPR = transformGHPullRequestToPullRequestStatus(currentPullRequestRef.getObjectId().name())
                    .apply(pullRequestFrom(url, currentPullRequestID));
            jGit.checkoutToRevision(flyweightFolder, currentPR.getMergeRef());
            Revision revision = jGit.getLatestRevision(flyweightFolder);

            if (revision == null) {
                LOGGER.debug("handleGetLatestRevision# - No latest revision found");
                return renderJSON(SUCCESS_RESPONSE_CODE, null);
            } else {
                Map<String, Object> revisionMap = getRevisionMap(revision, Lists.of(currentPR), currentPR);
                LOGGER.info("Triggered build for PR#" + currentPR.getId() + " with head as " + currentPR.getMergeSHA());
                return renderJSON(SUCCESS_RESPONSE_CODE, revisionMap);
            }
        } catch (Throwable t) {
            LOGGER.warn("get latest revision: ", t);
            return renderJSON(INTERNAL_ERROR_RESPONSE_CODE, t.getMessage());
        }
    }

    private GoPluginApiResponse handleLatestRevisionSince(GoPluginApiRequest goPluginApiRequest) {
        Map<String, String> configuration = keyValuePairs(goPluginApiRequest, "scm-configuration");
        String url = configuration.get("url");
        String flyweightFolder = (String) getValueFor(goPluginApiRequest, "flyweight-folder");
        Map<String, Object> previousRevisionMap = getMapFor(goPluginApiRequest, "previous-revision");
        PullRequests activePullRequests = JSONUtils.fromJson(((Map<String, Object>) previousRevisionMap.get("data")).get("ACTIVE_PULL_REQUESTS").toString(), PullRequests.class);

        try {
            JGitHelper jGit = new JGitHelper();
            LOGGER.debug("handleLatestRevisionSince# - Cloning / Fetching the latest for " + url);
            jGit.cloneOrFetch(url, flyweightFolder);
            LOGGER.debug("handleLatestRevisionSince# - Fetching all PR merge refs from remote - " + url);
            jGit.fetchRepository(url, flyweightFolder, PR_FETCH_REFSPEC);
            MergeRefs mergeRefs = jGit.findMergeRef(flyweightFolder);
            Option<Ref> newPullRequest = mergeRefs.findNotProcessed(activePullRequests);
            if (mergeRefs.isEmpty() || newPullRequest.isEmpty()) {
                LOGGER.debug("handleLatestRevisionSince# - No active PRs found. We're good here.");
                return renderJSON(SUCCESS_RESPONSE_CODE, null);
            }

            Ref currentPullRequestRef = newPullRequest.get();
            int currentPullRequestID = pullRequestIdFromRef(currentPullRequestRef);
            PullRequestStatus currentPR = transformGHPullRequestToPullRequestStatus(currentPullRequestRef.getObjectId().name())
                    .apply(pullRequestFrom(url, currentPullRequestID));
            jGit.checkoutToRevision(flyweightFolder, currentPR.getMergeRef());

            List<Revision> newerRevisions;
            if (activePullRequests.hasId(currentPR.getId()) && activePullRequests.get(currentPR.getId()).hasChanged(currentPR.getMergeSHA())) {
                newerRevisions = jGit.getNewerRevisions(flyweightFolder, activePullRequests.get(currentPR.getId()).getLastHead());
            } else {
                newerRevisions = Lists.of(jGit.getLatestRevision(flyweightFolder));
            }

            if (newerRevisions == null || newerRevisions.isEmpty()) {
                return renderJSON(SUCCESS_RESPONSE_CODE, null);
            } else {
                Map<String, Object> response = new HashMap<String, Object>();
                List<Map> revisions = new ArrayList<Map>();
                PullRequests pullRequestsToSave = activePullRequests.mergeWith(currentPR, mergeRefs);
                LOGGER.debug("Custom Data bag to save in handleLatestRevisionSince");
                LOGGER.debug(JSONUtils.toJson(pullRequestsToSave));

                for (Revision revisionObj : newerRevisions) {
                    Map<String, Object> revisionMap = getRevisionMap(revisionObj, pullRequestsToSave.getPullRequestStatuses(), currentPR);
                    revisions.add(revisionMap);
                }
                response.put("revisions", revisions);
                LOGGER.info("#handleLatestRevisionSince - Triggered build for PR#" + currentPR.getId() + " at " + currentPR.getMergeSHA());
                return renderJSON(SUCCESS_RESPONSE_CODE, response);
            }
        } catch (Throwable t) {
            LOGGER.warn("get latest revisions since: ", t);
            return renderJSON(INTERNAL_ERROR_RESPONSE_CODE, t.getMessage());
        }
    }

    private GoPluginApiResponse handleCheckout(GoPluginApiRequest goPluginApiRequest) {
        Map<String, String> configuration = keyValuePairs(goPluginApiRequest, "scm-configuration");
        String url = configuration.get("url");
        String destinationFolder = (String) getValueFor(goPluginApiRequest, "destination-folder");
        Map<String, Object> revisionMap = getMapFor(goPluginApiRequest, "revision");
        String revision = (String) revisionMap.get("revision");

        LOGGER.warn("destination: " + destinationFolder + ". commit: " + revision);

        try {
            JGitHelper jGit = new JGitHelper();
            jGit.cloneOrFetch(url, destinationFolder);
            jGit.fetchRepository(url, destinationFolder, PR_FETCH_REFSPEC);
            jGit.checkoutToRevision(destinationFolder, revision);

            Map<String, Object> response = new HashMap<String, Object>();
            ArrayList<String> messages = new ArrayList<String>();
            response.put("status", "success");
            messages.add("Checked out to revision " + revision);
            response.put("messages", messages);

            return renderJSON(SUCCESS_RESPONSE_CODE, response);
        } catch (Throwable t) {
            LOGGER.warn("checkout: ", t);
            return renderJSON(INTERNAL_ERROR_RESPONSE_CODE, t.getMessage());
        }
    }

    private GHPullRequest pullRequestFrom(String url, int currentPullRequestID) throws IOException {
        return buildGithubFromPropertyFile()
                .getRepository(GHUtils.parseGithubUrl(url))
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

    private void validate(List<Map<String, Object>> response, FieldValidator fieldValidator) {
        Map<String, Object> fieldValidation = new HashMap<String, Object>();
        fieldValidator.validate(fieldValidation);
        if (!fieldValidation.isEmpty()) {
            response.add(fieldValidation);
        }
    }

    private Map<String, Object> getRevisionMap(Revision revision, Iterable<PullRequestStatus> prStatuses, PullRequestStatus currentPR) {
        Map<String, Object> response = new HashMap<String, Object>();
        response.put("revision", revision.getRevision());
        response.put("timestamp", new SimpleDateFormat(DATE_PATTERN).format(revision.getTimestamp()));
        response.put("revisionComment", revision.getComment());
        List<Map> modifiedFilesMapList = new ArrayList<Map>();
        for (ModifiedFile modifiedFile : revision.getModifiedFiles()) {
            Map<String, String> modifiedFileMap = new HashMap<String, String>();
            modifiedFileMap.put("fileName", modifiedFile.getFileName());
            modifiedFileMap.put("action", modifiedFile.getAction());
            modifiedFilesMapList.add(modifiedFileMap);
        }
        Map<String, String> customDataBag = Maps.<String, String>builder()
                .put("ACTIVE_PULL_REQUESTS", JSONUtils.toJson(prStatuses))
                .put("PR_ID", String.valueOf(currentPR.getId()))
                .put("PR_BRANCH", String.valueOf(currentPR.getPrBranch()))
                .put("TARGET_BRANCH", String.valueOf(currentPR.getToBranch()))
                .put("PR_URL", String.valueOf(currentPR.getUrl()))
                .put("PR_AUTHOR", currentPR.getAuthor())
                .put("PR_AUTHOR_EMAIL", currentPR.getAuthorEmail())
                .put("PR_DESCRIPTION", currentPR.getDescription())
                .put("PR_TITLE", currentPR.getTitle())
                .value();
        response.put("data", customDataBag);
        response.put("modifiedFiles", modifiedFilesMapList);
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

    private GoPluginApiResponse renderJSON(final int responseCode, Object response) {
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
