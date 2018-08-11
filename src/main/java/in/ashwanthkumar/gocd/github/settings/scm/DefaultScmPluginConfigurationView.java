package in.ashwanthkumar.gocd.github.settings.scm;

import in.ashwanthkumar.gocd.github.util.BranchFilter;
import in.ashwanthkumar.gocd.github.util.FieldFactory;

import java.util.HashMap;
import java.util.Map;

public class DefaultScmPluginConfigurationView implements ScmPluginConfigurationView {


    @Override
    public String templateName() {
        return "/views/scm.template.html";
    }

    @Override
    public Map<String, Object> fields() {
        Map<String, Object> response = new HashMap<String, Object>();
        response.put("url", FieldFactory.createForScm("URL", null, true, true, false, "0"));
        response.put("username", FieldFactory.createForScm("Username", null, false, false, false, "1"));
        response.put("password", FieldFactory.createForScm("Password", null, false, false, true, "2"));
        response.put("defaultBranch", FieldFactory.createForScm("Default Branch", "master", false, false, false, "3"));
        response.put("shallowClone", FieldFactory.createForScm("Default Clone Behaviour", "false", false, false, false, "4"));
        return response;
    }

    @Override
    public BranchFilter getBranchFilter(Map<String, String> configuration) {
        return new BranchFilter();
    }

    @Override
    public boolean hasConfigurationView() {
        return true;
    }
}
