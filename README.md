[![Build Status](https://snap-ci.com/ashwanthkumar/gocd-build-github-pull-requests/branch/master/build_image)](https://snap-ci.com/ashwanthkumar/gocd-build-github-pull-requests/branch/master)

# GoCD - Build PR from Github

This GoCD's SCM plugin polls the Github repository for any new Pull Requests and triggers the build pipeline. This plugin was built for one thing and that is to build PRs. No other fancy features. [This](https://groups.google.com/d/msg/go-cd-dev/Rt_Y5G2VkOc/ymIyeEds8swJ) discussion was the motivation behind building this plugin.

## Requirements
This needs GoCD >= v15.x which is due release as of writing.

## Get Started
- Download the latest plugin jar from [Releases](https://github.com/ashwanthkumar/gocd-build-github-pull-requests/releases) section.
- Create a file `~/.github` on Go Server and on all Go Agent machines, with the following contents.
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

## To Dos
- Clean up the code esp. the JSON SerDe part
- Add proper tests around the plugin
- Add support for [Github's commit status](https://developer.github.com/v3/repos/statuses/) API to push build status to Github. May be a separate task plugin?

## FAQs

### How Pull Requests are Built?
We periodically poll for new PRs on the given repository. We build a pull request when it's first opened and when commits are added to the pull request throughout its lifetime.
Rather than test the commits from the branches the pull request is sent from, we test the merge between the origin and the upstream branch.

### Pull Request isn't being built
If a pull request isn't built, that usually means that it can't be merged. We rely on the merge commit that GitHub transparently creates between the changes in the source branch and the upstream branch the pull request is sent against.
So when you create or update a pull request, and Go doesn't create a build for it, make sure the pull request is mergeable. If it isn't, rebase it against the upstream branch and resolve any merge conflicts.
