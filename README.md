[![Build Status](https://snap-ci.com/ashwanthkumar/gocd-build-github-pull-requests/branch/master/build_image)](https://snap-ci.com/ashwanthkumar/gocd-build-github-pull-requests/branch/master)

# GoCD - Build PR from Github
This GoCD's SCM plugin polls the Github repository for Pull Requests (new PR/update to existing PRs) and triggers the build pipeline. [Discussion Thread](https://groups.google.com/d/topic/go-cd-dev/Rt_Y5G2VkOc/discussion)

## Requirements
This needs GoCD >= v15.x which is due release as of writing.

## Get Started
**Installation:**
- Download the latest plugin jar from [Releases](https://github.com/ashwanthkumar/gocd-build-github-pull-requests/releases) section. Place it in `<go-server-location>/plugins/external` & restart Go Server.

**Usage:**
- Assuming you already have a pipeline "ProjectA" for one of your Github repos, 'Extract Template' from the pipeline (if its not templatized already)
- Create new pipeline say "ProjectA-PullRequests" off of the extracted template. You can clone "ProjectA" pipeline to achieve this.
- Select `Github` in Admin -> 'Pipeline' Configuration (ProjectA-PullRequests) -> 'Materials' Configuration -> 'Material' listing drop-down

**Authentication:**
- You can choose to provide `username` & `password` through Go Config XML
- (or) Create a file `~/.github` with the following contents: (Note: `~/.github` needs to be available on Go Server and on all Go Agent machines)
```
login=johndoe
password=thisaintapassword
```

**Github Enterprise:**
- If you intend to use this plugin with 'Github Enterprise' then add the following content in `~/.github` (Note: `~/.github` needs to be available on Go Server and on all Go Agent machines)
```
# for enterprise installations - Make sure to add /api/v3 to the hostname
# ignore this field or have the value to https://api.github.com
endpoint=http://code.yourcompany.com/api/v3
```

## Behavior
- First run of the new pipeline will be off of 'master' branch. This is prepare a setup base PR-Revision map. It also serves as sanity check for newly created pipeline.
- From then on, any new change (new PR create / new commits to existing PR) will trigger the new pipeline. Only the top commit in the PR will show up in build cause.
- PR details (id, author etc.) will be available as environement variable for tasks to consume.

## To Dos
- Clean up the code esp. the JSON SerDe part
- Add proper tests around the plugin
- Add support for [Github's commit status](https://developer.github.com/v3/repos/statuses/) API to push build status to Github. May be a separate task/notification plugin?

## FAQs

