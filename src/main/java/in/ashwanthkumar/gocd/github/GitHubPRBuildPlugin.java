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
import in.ashwanthkumar.gocd.github.model.ModifiedFile;
import in.ashwanthkumar.gocd.github.model.PullRequestStatus;
import in.ashwanthkumar.gocd.github.model.PullRequests;
import in.ashwanthkumar.gocd.github.model.Revision;
import in.ashwanthkumar.utils.collections.Lists;
import in.ashwanthkumar.utils.collections.Maps;
import in.ashwanthkumar.utils.func.Function;
import in.ashwanthkumar.utils.lang.option.Option;
import org.apache.commons.io.IOUtils;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        LOGGER.warn("flyweight: " + flyweightFolder);

        try {
            LOGGER.info(String.format("Connecting to Github for %s", url));
            GHRepository repository = GitHub.connect().getRepository(GHUtils.parseGithubUrl(url));
            LOGGER.info("Github Connection succesful!");
            LOGGER.info("Fetching PR information from " + repository.getFullName());
            List<PullRequestStatus> prStatuses = getPullRequestStatuses(repository);
            LOGGER.info("Fetch successful.");
            PullRequestStatus currentPR = null;
            if (prStatuses.isEmpty()) {
                LOGGER.info("handleGetLatestRevision# - No active PRs found. We're good");
                return renderJSON(SUCCESS_RESPONSE_CODE, null);
            } else {
                currentPR = prStatuses.get(0);
            }

            JGitHelper jGit = new JGitHelper();
            jGit.cloneOrFetch(repository.getSshUrl(), flyweightFolder);
            jGit.fetchRepository(repository.getSshUrl(), flyweightFolder, currentPR.getRef());
            jGit.checkoutToRevision(flyweightFolder, currentPR.getLastHead());
            Revision revision = jGit.getLatestRevision(flyweightFolder);

            if (revision == null) {
                LOGGER.info("handleGetLatestRevision# - No latest revision found");
                return renderJSON(SUCCESS_RESPONSE_CODE, null);
            } else {
                currentPR.scheduled();
                Map<String, Object> revisionMap = getRevisionMap(revision, prStatuses, currentPR);
                LOGGER.info("Triggered build for PR#" + currentPR.getId() + " with head as " + currentPR.getLastHead());
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
        PullRequests activePullRequests = JSONUtils.fromJson(((Map<String, Object>) previousRevisionMap.get("data")).get("activePullRequests").toString(), PullRequests.class);

        try {
            LOGGER.info(String.format("Connecting to Github for %s", url));
            GHRepository repository = GitHub.connect().getRepository(GHUtils.parseGithubUrl(url));
            LOGGER.info("Github Connection succesful!");
            LOGGER.info("Fetching PR information from " + repository.getFullName());
            List<PullRequestStatus> openPRs = getPullRequestStatuses(repository);
            LOGGER.info("Fetch successful.");
            PullRequests newActivePullRequests = activePullRequests.mergeWith(openPRs);
            Option<PullRequestStatus> notProcessed = newActivePullRequests.nextNotProcessed();
            if (notProcessed.isEmpty()) {
                LOGGER.info("#handleLatestRevisionSince - No Active PRs / all PRs already built up-to-date.");
                return renderJSON(SUCCESS_RESPONSE_CODE, null);
            }
            PullRequestStatus currentPR = notProcessed.get();

            LOGGER.info("We're going to process Pull Request #" + currentPR.getId());

            JGitHelper jGit = new JGitHelper();
            jGit.cloneOrFetch(url, flyweightFolder);
            jGit.fetchRepository(url, flyweightFolder, currentPR.getRef());
            jGit.checkoutToRevision(flyweightFolder, currentPR.getLastHead());
            List<Revision> newerRevisions;
            if (activePullRequests.hasId(currentPR.getId()) && activePullRequests.get(currentPR.getId()).hasChanged(currentPR.getLastHead())) {
                newerRevisions = jGit.getNewerRevisions(flyweightFolder, activePullRequests.get(currentPR.getId()).getLastHead());
            } else {
                newerRevisions = Lists.of(jGit.getLatestRevision(flyweightFolder));
            }

            if (newerRevisions == null || newerRevisions.isEmpty()) {
                LOGGER.warn("We did not find any new revisions on PR-" + currentPR.getId());
                LOGGER.warn("currentPR-" + currentPR);
                return renderJSON(SUCCESS_RESPONSE_CODE, null);
            } else {
                LOGGER.warn("new commits: " + newerRevisions.size());

                Map<String, Object> response = new HashMap<String, Object>();
                List<Map> revisions = new ArrayList<Map>();
                newActivePullRequests.schedule(currentPR.getId());
                for (Revision revisionObj : newerRevisions) {
                    Map<String, Object> revisionMap = getRevisionMap(revisionObj, newActivePullRequests.getPullRequestStatuses(), currentPR);
                    revisions.add(revisionMap);
                }
                response.put("revisions", revisions);
                LOGGER.info("Triggered build for PR#" + currentPR.getId() + " with head as " + currentPR.getLastHead());
                return renderJSON(SUCCESS_RESPONSE_CODE, response);
            }
        } catch (Throwable t) {
            LOGGER.warn("get latest revisions since: ", t);
            return renderJSON(INTERNAL_ERROR_RESPONSE_CODE, null);
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
            jGit.checkoutToRevision(destinationFolder, revision);

            Map<String, Object> response = new HashMap<String, Object>();
            ArrayList<String> messages = new ArrayList<String>();
            response.put("status", "success");
            messages.add("Checked out to revision " + revision);
            response.put("messages", messages);

            return renderJSON(SUCCESS_RESPONSE_CODE, response);
        } catch (Throwable t) {
            LOGGER.warn("checkout: ", t);
            return renderJSON(INTERNAL_ERROR_RESPONSE_CODE, null);
        }
    }

    private List<PullRequestStatus> getPullRequestStatuses(GHRepository repository) throws IOException {
        List<GHPullRequest> activePRs = repository.getPullRequests(GHIssueState.OPEN);
        return Lists.map(activePRs, new Function<GHPullRequest, PullRequestStatus>() {
            @Override
            public PullRequestStatus apply(GHPullRequest input) {
                int prID = GHUtils.prIdFrom(input.getDiffUrl().toString());
                return new PullRequestStatus(prID, input.getHead().getSha());
            }
        });
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
                .put("activePullRequests", JSONUtils.toJson(prStatuses))
                .put("PR_ID", String.valueOf(currentPR.getId()))
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
