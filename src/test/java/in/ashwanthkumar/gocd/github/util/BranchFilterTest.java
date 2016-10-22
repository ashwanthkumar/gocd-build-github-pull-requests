package in.ashwanthkumar.gocd.github.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class BranchFilterTest {

    private enum Expect {
        PASS, FAIL;

        @Override
        public String toString() {
            return super.toString().toLowerCase();
        }
    }

    private static String blacklist(String value) {
        return value;
    }

    private static String whitelist(String value) {
        return value;
    }

    private static String branch(String value) {
        return value;
    }

    @Parameterized.Parameters(name = "whitelist: \"{0}\" and blacklist: \"{1}\"; Expect branch \"{2}\" to {3}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                {blacklist(null), whitelist(null), branch("master"), Expect.PASS},
                {blacklist(null), whitelist(null), branch("feature"), Expect.PASS},

                {blacklist("master"), whitelist("feature"), branch("feature"), Expect.PASS},
                {blacklist("feature"), whitelist("feature"), branch("feature"), Expect.FAIL},
                {blacklist("master"), whitelist("feature"), branch("master"), Expect.FAIL},

                {blacklist(null), whitelist("feature"), branch("feature"), Expect.PASS},
                {blacklist("master"), whitelist(null), branch("master"), Expect.FAIL},
                {blacklist("master"), whitelist(null), branch("feature"), Expect.PASS},
                {blacklist("master"), whitelist(""), branch("master"), Expect.FAIL},
                {blacklist("master"), whitelist(""), branch("feature"), Expect.PASS},

                {blacklist("master1"), whitelist("master*"), branch("master"), Expect.PASS},
                {blacklist("master1"), whitelist("master*"), branch("master2"), Expect.PASS},
                {blacklist("master1"), whitelist("master*"), branch("master1"), Expect.FAIL},
                {blacklist("master*"), whitelist("master1"), branch("master1"), Expect.FAIL},
                {blacklist("master*"), whitelist("master1"), branch("master2"), Expect.FAIL},

                {blacklist("master[a-d]"), whitelist("master[cde]"), branch("masterc"), Expect.FAIL},
                {blacklist("master[a-d]"), whitelist("master[cde]"), branch("mastere"), Expect.PASS},
        });
    }

    private String blacklist;
    private String whitelist;
    private String branch;
    private Expect expect;

    public BranchFilterTest(String blacklist, String whitelist, String branch, Expect expect) {
        this.blacklist = blacklist;
        this.whitelist = whitelist;
        this.branch = branch;
        this.expect = expect;
    }

    @Test
    public void shouldResolveBranchValidity() {
        BranchFilter filter = new BranchFilter(blacklist, whitelist);

        if (expect == Expect.PASS) {
            assertTrue("PASS", filter.isBranchValid(branch));
        } else {
            assertFalse("FAIL", filter.isBranchValid(branch));
        }
    }

}