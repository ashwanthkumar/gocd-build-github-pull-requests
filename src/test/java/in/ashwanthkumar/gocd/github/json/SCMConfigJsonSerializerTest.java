package in.ashwanthkumar.gocd.github.json;

import in.ashwanthkumar.gocd.github.model.SCMConfig;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class SCMConfigJsonSerializerTest {
    @Test
    public void shouldSerializeSCMConfigProperly() {
        SCMConfig scmConfig = new SCMConfig().name("URL").displayName("Github");
        String json = JSONUtils.toJson(scmConfig);
        assertThat(json, is("{\"URL\":{\"display-name\":\"Github\",\"display-order\":\"\",\"required\":false,\"part-of-identity\":false,\"secure\":false}}"));
    }

    @Test
    public void shouldDeserializeConfig() {
        String json = "{\"URL\":{\"display-name\":\"\",\"display-order\":\"\",\"required\":false,\"part-of-identity\":false,\"secure\":false}}";
        SCMConfig scmConfig = JSONUtils.fromJson(json, SCMConfig.class);
        assertThat(scmConfig.getName(), is("URL"));
    }
}
