[![Build Status](https://snap-ci.com/ashwanthkumar/gocd-build-github-pull-requests/branch/master/build_image)](https://snap-ci.com/ashwanthkumar/gocd-build-github-pull-requests/branch/master)
[![Build Status](https://travis-ci.org/ashwanthkumar/gocd-github-pull-request.svg?branch=master)](https://travis-ci.org/ashwanthkumar/gocd-github-pull-request)

# GoCD - Git feature branch support
This is a GoCD SCM plugin for Git feature branch support. [Discussion Thread](https://groups.google.com/d/topic/go-cd-dev/Rt_Y5G2VkOc/discussion)

Supported (as seperate plugins):
* Git repository for branches
* Github repository for Pull Requests
* Stash repository for Pull Requests
* Gerrit repository for Change Sets

## Migration from Git Feature Branch plugin 1.2.x to 1.3

Addition of the white and blacklist feature to Git Feature Branch plugin changes the identity of 
the SCM material. This means that it may trigger automatically triggered pipelines that use 
Git Feature Branch material. When moving from version 1.2.x to 1.3.x be sure to pause all 
pipelines which shouldn't be triggered or be ready to cancel them in case they are triggered.

## Requirements
These plugins require GoCD version v15.x or above.

## Get Started
**Installation:**
- Download the latest plugin jar from the [Releases](https://github.com/ashwanthkumar/gocd-build-github-pull-requests/releases) section. Place it in `<go-server-location>/plugins/external` & restart Go Server. You can find the location of the Go Server installation [here](http://www.go.cd/documentation/user/current/installation/installing_go_server.html#location-of-files-after-installation-of-go-server).

**Usage:**

* Make sure plugins are loaded. Note: You can use [GoCD build status notifier](https://github.com/srinivasupadhya/gocd-build-status-notifier) to update status of Pull Requests with build status.
![Plugins listing page][1]

* Assuming you already have a pipeline "ProjectA" for one of your repos
![Original pipeline][2]
![Original pipeline material listing page][3]

* 'Extract Template' from the pipeline, if its not templatized already (this is optional step) 
![Pipelines listing page][4]
![Extract template pop-up][5]

* Create new pipeline say "ProjectA-FeatureBranch" off of the extracted template. You can clone "ProjectA" pipeline to achieve this.
![Pipelines listing page after extract template][6]
![Clone pipeline pop-up][7]

* In the materials configuration for your newly created pipeline, you will see that there is a new material for each of the plugins you have installed (Git Feature Branch, Github, Stash or Gerrit). Select one of these new materials, fill in the details and the plugin will build the pull requests from the given material.
![Select GitHub drop-down][8]
![Add GitHub drop-down][9]
![New pipeline material listing page][10]

* You can delete the old material that is left over from cloning your pipeline.
![Delete old material pop-up][11]

## Behavior
- First run of the new pipeline will be off of 'master' branch. This creates base PR-Revision map. It also serves as sanity check for newly created pipeline.

- From then on, any new change (new PR create / new commits to existing PR) will trigger the new pipeline. Only the top commit in the PR will show up in build cause.
![New pipeine schedule][12]

- PR details (id, author etc.) will be available as environement variable for tasks to consume.

- Build status notifier plugin will update Pull Request with build status
![On successful run of new pipeline][13]

- In order to force GoCD to build every commit the pipeline name has be defined for the material.

### Github

**Authentication:**
- You can create a file `~/.github` with the following contents: (Note: `~/.github` needs to be available on Go Server)
```
login=johndoe
password=thisaintapassword
```

- You can also generate & use oauth token. To do so create a file `~/.github` with the following contents: (Note: `~/.github` needs to be available on Go Server)
```
login=johndoe
oauth=thisaintatoken
```

**Github Enterprise:**
- If you intend to use this plugin with 'Github Enterprise' then add the following content in `~/.github` (Note: `~/.github` needs to be available on Go Server)
```
# for enterprise installations - Make sure to add /api/v3 to the hostname
# ignore this field or have the value to https://api.github.com
endpoint=http://code.yourcompany.com/api/v3
```

### Stash
**Authentication**

If authentication is required, place a file named `.netrc` under the Go user's home directory. The file needs to be created in both the server and any number of agents that will build this material. You can find the home directory for Go server [here](http://www.go.cd/documentation/user/current/installation/installing_go_server.html#location-of-files-after-installation-of-go-server) and for Go Agent
[here](http://www.go.cd/documentation/user/current/installation/installing_go_agent.html#location-of-files-after-installing-go-agent).

The `.netrc` file takes the following format:

```
machine stash.vm.com 
login myusername
password mypassword
```

### Git

#### Branch filtering

Git feature branches support filtering the branches with whitelist and blacklist.
Both lists support _glob_ syntax (`*`, `?`, `[...]`, `{...}`) and multiple branch patterns
can be given as a comma separated list. The glob syntax is same as
defined in Java's [FileSystem.getPathMatcher()](https://docs.oracle.com/javase/7/docs/api/java/nio/file/FileSystem.html#getPathMatcher%28java.lang.String%29)
method. If neither blacklist or whitelist pattern is defined, all branches will be built.

The blacklist takes precedence over whitelist. I.e. a branch will not be built if the
blacklist pattern matches to the branch name.

## To Dos
- Clean up the code esp. the JSON SerDe part
- Add proper tests around the plugin

## FAQs

### Pull Request isn't being built
- If more than 1 PR gets updated (create/update) Go bunches them together for the next pipeline run & uses the top change in the "build-cause" to build. You can force trigger pipeline with other revisions until this get fixed ([thread](https://github.com/gocd/gocd/issues/938)).

[1]: images/list-plugin.png  "List Plugin"
[2]: images/original-pipeline.png  "Original Pipeline"
[3]: images/original-pipeline-material.png  "Original Pipeline Material"
[4]: images/list-pipeline.png  "List Pipeline"
[5]: images/extract-template.png  "Extract Template"
[6]: images/list-pipeline-after-extract-template.png  "List Pipeline After Extract Template"
[7]: images/clone-pipeline.png  "Clone Pipeline"
[8]: images/select-github-material.png  "Select GitHub Material"
[9]: images/add-github-material.png  "Add GitHub Material"
[10]: images/new-pipeline-material.png  "New Pipeline Material"
[11]: images/delete-old-material.png  "Delete Old Material"
[12]: images/pipeline-schedule.png  "Pipeline Schedule"
[13]: images/on-successful-pipeline-run.png  "On Successful Run"
