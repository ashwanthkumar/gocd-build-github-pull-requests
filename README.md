[![Build Status](https://snap-ci.com/ashwanthkumar/gocd-build-github-pull-requests/branch/master/build_image)](https://snap-ci.com/ashwanthkumar/gocd-build-github-pull-requests/branch/master)

# GoCD - Build PR from Github

This GoCD's SCM plugin polls the Github repository for any new Pull Requests and triggers the build pipeline. This plugin was built for one thing and that is to build PRs. No other fancy features. [This](https://groups.google.com/d/msg/go-cd-dev/Rt_Y5G2VkOc/ymIyeEds8swJ) discussion was the motivation behind building this plugin.

## How Pull Requests are Built
We pool periodically for any new PRs on the given repository. We build a pull request when it's first opened and when commits are added to the pull request throughout its lifetime.
Rather than test the commits from the branches the pull request is sent from, we test the merge between the origin and the upstream branch.

## Pull Request isn't being built
If a pull request isn't built, that usually means that it can't be merged. We rely on the merge commit that GitHub transparently creates between the changes in the source branch and the upstream branch the pull request is sent against.
So when you create or update a pull request, and Go doesn't create a build for it, make sure the pull request is mergeable. If it isn't, rebase it against the upstream branch and resolve any merge conflicts.

## Requirements
This needs GoCD >= v15.x which is due release as of writing.

## Get Started
- Download the latest plugin jar from [Releases](https://github.com/ashwanthkumar/gocd-build-github-pull-requests/releases) section.
- Create a file `~/.github` on Go Server and on all Go Agent machines, with the following contents. More information on http://github-api.kohsuke.org/. In the next release of the plugin we'll remove this dependency. 
```
login=johndoe
password=thisaintapassword
# for enterprise installations - Make sure to add /api/v3 to the hostname
# ignore this field or have the value to https://api.github.com
endpoint=http://code.yourcompany.com/api/v3
```
- Lets assume you already have a pipeline "ProjectA" for one of your Github repos
- Do a 'Extract Template' from the pipeline (if not present already)
- Now clone your "ProjectA" pipeline to "ProjectA-PullRequests"
- Since SCM notification end point feature still doesn't have UI support for adding custom SCM Materials, you need to make some changes in the config.xml
- Go to "Admin" -> "Config XML"
- Add the following line in your xml
```
<?xml version="1.0" encoding="utf-8"?>
<cruise ....>
  <scms>
    <!-- You might have to manually edit the id part here for every new material -->
    <scm id="34293182-33e4-4459-a686-276d70387dfb" name="test">
      <pluginConfiguration id="github.pr" version="0.1" />
      <configuration>
        <property>
          <key>url</key>
          <value>git@github.com:user-name/project-a.git</value>
        </property>
      </configuration>
    </scm>
  </scms>

  <piplines>
    <pipeline name="Project-A-PullRequests" template="Project-A-Build">
      <materials>
        <!-- Make sure the ref="..." value matches the one specified above -->
        <scm ref="34293182-33e4-4459-a686-276d70387dfb">
        </scm>
      </materials>
    </pipeline>
  </pipelines>
</cruise>
```

## Under the hood
Under the hood, we exploit the fact that custom "data" as part of the [latestRevision](http://www.go.cd/documentation/developer/writing_go_plugins/scm_material/version_1_0/latest_revision.html) message is available during [latestRevisionSince](http://www.go.cd/documentation/developer/writing_go_plugins/scm_material/version_1_0/latest_revisions_since.html) message as well.
First time when we run, we store all the open PRs in the data bag as
```

{
  "data": {
    "activePullRequests": "[ {
        "id": 1,
        "ref": "refs/pull/1/head",
        "lastHead": "ff59f906669db51d558723b4d4f1ef3871f7ec1c",
        "alreadyScheduled": false
      },
      {
        "id": 3,
        "ref": "refs/pull/3/head",
        "lastHead": "12a57841805c9b2b0db7ec8b3c96b867fdb541ec",
        "alreadyScheduled": true
      } ]"
  }
}
```
We store all the open PRs in this fashion. With this we can find if a PR has new commit and trigger a build accordingly. As soon as we schedule a build against a PR, we set the `"alreadyScheduled"` flag for the PR to true. When a PR is closed / no longer available we remove that PR from the list completely. We batch all the PRs and schedule them sequentially one after the other. After every build we look for any new changes across all the PRs.

## To Dos
- Clean up the code esp. the JSON SerDe part
- Add proper tests around the plugin
- Add support for [Github's commit status](https://developer.github.com/v3/repos/statuses/) API to push build status to Github. May be a separate task plugin?
