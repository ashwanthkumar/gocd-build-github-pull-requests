package in.ashwanthkumar.gocd.github.model;

import com.google.gson.annotations.SerializedName;

/**
 * Messages used while SCM Configuration
 * Ref - http://www.go.cd/documentation/developer/writing_go_plugins/scm_material/version_1_0/scm_configuration.html
 */
public class SCMConfig {

    private String name;
    private Properties props = new Properties();

    public static class Properties {
        @SerializedName("display-name")
        public String displayName = "";
        @SerializedName("display-order")
        public String displayOrder = "";
        @SerializedName("required")
        public boolean required = false;
        @SerializedName("part-of-identity")
        public boolean partOfIdentity = false;
        @SerializedName("secure")
        public boolean secure = false;
        @SerializedName("default-value")
        public String defaultValue;
    }

    public Properties props() {
        return props;
    }

    public String getName() {
        return name;
    }

    public SCMConfig name(String name) {
        this.name = name;
        return this;
    }

    public SCMConfig displayName(String displayName) {
        this.props.displayName = displayName;
        return this;
    }

    public SCMConfig displayOrder(String displayOrder) {
        this.props.displayOrder = displayOrder;
        return this;
    }

    public SCMConfig required(boolean required) {
        this.props.required = required;
        return this;
    }

    public SCMConfig partOfIdentity(boolean partOfIdentity) {
        this.props.partOfIdentity = partOfIdentity;
        return this;
    }

    public SCMConfig secure(boolean secure) {
        this.props.secure = secure;
        return this;
    }

    public SCMConfig defaultValue(String defaultValue) {
        this.props.defaultValue = defaultValue;
        return this;
    }

}
