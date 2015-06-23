package in.ashwanthkumar.gocd.github.util;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class URLUtilsTest {
    private URLUtils urlUtils;

    @Before
    public void setUp() throws Exception {
        urlUtils = new URLUtils();
    }

    @Test
    public void shouldValidateURL() throws Exception {
        assertTrue(urlUtils.isValidHTTPUrl("http://www.google.com"));
        assertTrue(urlUtils.isValidHTTPUrl("https://www.google.com"));
    }

    @Test
    public void shouldTestForValidSSHUrl() {
        assertTrue(urlUtils.isValidSSHUrl("git@code.corp.yourcompany.com:username/repo"));
        assertTrue(urlUtils.isValidSSHUrl("git@code.corp.yourcompany.com:username/repo.git"));
        assertTrue(urlUtils.isValidSSHUrl("git@code.corp.yourcompany.com:username/repo/"));
        assertFalse(urlUtils.isValidSSHUrl("git@code.corp.yourcompany.com:username/repo/foo"));
        assertFalse(urlUtils.isValidSSHUrl("git@code.corp.yourcompany.com:/username/repo"));
    }
}
