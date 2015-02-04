# GoCD - Build PR from Github

This GoCD's SCM plugin polls the Github repository for any new Pull Requests and triggers the build pipeline. This plugin was built for one thing and that is to build PRs. No other fancy features. [This](https://groups.google.com/d/msg/go-cd-dev/Rt_Y5G2VkOc/ymIyeEds8swJ) discussion was the motivation behind building this plugin.

## Workflow
- Templatize your project's build pipeline stages
- Create another pipeline that would be specifically used for building PRs in the repository

## Under the hood
Under the hood, we exploit the fact that custom "data" as part of the [latestRevision](http://www.go.cd/documentation/developer/writing_go_plugins/scm_material/version_1_0/latest_revision.html) message is available during [latestRevisionSince](http://www.go.cd/documentation/developer/writing_go_plugins/scm_material/version_1_0/latest_revisions_since.html) message as well.
First time when we run, we store all the open PRs in the data bag as
```
{
    "data": {
        "PR_PENDING_$id": true
        "PR_REVISON_$id": "Commit SHA for the PR"
        ...
    }
}
```
We store all the open PRs in this fashion. With this we can find if a PR has new commit and trigger a build accordingly. As soon as we schedule a build against a PR, we remove the `"PR_PENDING_$id"` from the data bag. When a PR is closed / no longer available we remove the `"PR_REVISON_$id"` key.
We batch all the PRs and schedule them sequentially one after the other. After every build we look for any new changes across the PRs.

### Questions
- What happens when we trigger a version in the plugin history, but the corresponding PR is not available?
- I know, using `data` bag as a state between triggers, is a huge hack. I'm not sure how will this affect in terms of DB Storage and query processing, for repos with large number of active PRs.
