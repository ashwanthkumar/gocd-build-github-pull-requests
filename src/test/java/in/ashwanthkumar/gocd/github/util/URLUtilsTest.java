package in.ashwanthkumar.gocd.github.util;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class URLUtilsTest {
    private URLUtils urlUtils;

    @Before
    public void setUp() throws Exception {
        urlUtils = new URLUtils();
    }

    @Test
    public void shouldValidateURL() throws Exception {
        assertThat(urlUtils.isValidURL("http://www.google.com"), is(true));
        assertThat(urlUtils.isValidURL("https://www.google.com"), is(true));
    }
}
