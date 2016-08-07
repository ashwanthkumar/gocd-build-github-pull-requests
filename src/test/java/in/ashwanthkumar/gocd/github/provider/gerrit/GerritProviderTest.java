package in.ashwanthkumar.gocd.github.provider.gerrit;

import in.ashwanthkumar.gocd.github.provider.Provider;
import in.ashwanthkumar.gocd.github.settings.scm.PluginConfigurationView;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;


public class GerritProviderTest extends in.ashwanthkumar.gocd.github.provider.AbstractProviderTest {

    @Test
    public void shouldReturnCorrectScmSettingsTemplate() throws Exception {
        PluginConfigurationView scmConfigurationView = getScmView();

        assertThat(scmConfigurationView.templateName(), is("/views/scm.template.html"));;
    }

    @Test
    public void shouldReturnCorrectScmSettingsFields() throws Exception {
        PluginConfigurationView scmConfigurationView = getScmView();

        assertThat(scmConfigurationView.fields().keySet(),
                   hasItems("url", "username", "password", "pipeline_name")
        );
        assertThat(scmConfigurationView.fields().size(), is(4));
    }

    @Test
    public void shouldReturnCorrectGeneralSettingsTemplate() throws Exception {
        PluginConfigurationView generalConfigurationView = getGeneralView();

        assertThat(generalConfigurationView.templateName(), is("/views/gerrit.plugin.template.html"));
        assertThat(generalConfigurationView.hasConfigurationView(), is(true));
    }

    @Test
    public void shouldReturnCorrectGeneralSettingsFields() throws Exception {
        PluginConfigurationView generalConfigurationView = getGeneralView();

        assertThat(generalConfigurationView.fields().keySet(),
                hasItems("go_api_host", "go_api_username", "go_api_password")
        );
        assertThat(generalConfigurationView.fields().size(), is(3));
    }

    @Override
    protected Provider getProvider() {
        return new GerritProvider();
    }

}