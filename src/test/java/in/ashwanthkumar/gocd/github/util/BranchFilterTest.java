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

    private static class Value {
        private String value;

        public Value(String value) {
            this.value = value;
        }

        public String get() { return value; }

        @Override
        public String toString() {
            return value;
        }
    }

    private static class Blacklist extends Value {
        public Blacklist(String value) {
            super(value);
        }
    }
    private static class Whitelist extends Value {
        public Whitelist(String value) {
            super(value);
        }
    }
    private static class Branch extends Value {
        public Branch(String value) {
            super(value);
        }
    }

    @Parameterized.Parameters(name = "whitelist: \"{0}\" and blacklist: \"{1}\"; Expect branch \"{2}\" to {3}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                {new Blacklist(null),      new Whitelist(null),     new Branch("master"),     Expect.PASS},
                {new Blacklist(null),      new Whitelist(null),     new Branch("feature"),     Expect.PASS},

                {new Blacklist("master"), new Whitelist("feature"), new Branch("feature"), Expect.PASS},

                {new Blacklist(null), new Whitelist("feature"), new Branch("feature"), Expect.PASS},
                {new Blacklist("master"), new Whitelist(null), new Branch("master"), Expect.FAIL},
                {new Blacklist("master"), new Whitelist(null), new Branch("feature"), Expect.PASS},
                {new Blacklist("master"), new Whitelist(""), new Branch("master"), Expect.FAIL},
                {new Blacklist("master"), new Whitelist(""), new Branch("feature"), Expect.PASS},

                {new Blacklist("feature"), new Whitelist("feature"), new Branch("feature"), Expect.FAIL},
                {new Blacklist("master"), new Whitelist("feature"), new Branch("master"), Expect.FAIL},

                {new Blacklist("master1"), new Whitelist("master*"), new Branch("master"), Expect.PASS},
                {new Blacklist("master1"), new Whitelist("master*"), new Branch("master2"), Expect.PASS},
                {new Blacklist("master1"), new Whitelist("master*"), new Branch("master1"), Expect.FAIL},
                {new Blacklist("master*"), new Whitelist("master1"), new Branch("master1"), Expect.FAIL},
                {new Blacklist("master*"), new Whitelist("master1"), new Branch("master2"), Expect.FAIL},

                {new Blacklist("master[a-d]"), new Whitelist("master[cde]"), new Branch("masterc"), Expect.FAIL},
                {new Blacklist("master[a-d]"), new Whitelist("master[cde]"), new Branch("mastere"), Expect.PASS},
        });
    }

    private Blacklist blacklist;
    private Whitelist whitelist;
    private Branch branch;
    private Expect expect;

    public BranchFilterTest(Blacklist blacklist, Whitelist whitelist, Branch branch, Expect expect) {
        this.blacklist = blacklist;
        this.whitelist = whitelist;
        this.branch = branch;
        this.expect = expect;
    }

    @Test
    public void test() {
        BranchFilter filter = new BranchFilter(blacklist.get(), whitelist.get());

        if (expect == Expect.PASS) {
            assertTrue("PASS", filter.isBranchValid(branch.get()));
        } else {
            assertFalse("FAIL", filter.isBranchValid(branch.get()));
        }
    }

}