# GoCD - Build PR from Github

This GoCD's SCM plugin polls the Github repository for any new Pull Requests and triggers the build pipeline. This plugin was built for one thing and that is to build PRs. No other fancy features. [This](https://groups.google.com/d/msg/go-cd-dev/Rt_Y5G2VkOc/ymIyeEds8swJ) discussion was the motivation behind building this plugin.

## Workflow
- Templatize your project's build pipeline stages
- Create another pipeline that would be specifically used for building PRs in the repository


## License
http://www.apache.org/licenses/LICENSE-2.0
