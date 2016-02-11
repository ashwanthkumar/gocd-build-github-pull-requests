package in.ashwanthkumar.gocd.github.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class BranchMatcherTest {

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                {null, valid(), invalid("", "branch", "master")},
                {"", valid(), invalid("", "branch", "master")},
                {"     ", valid(), invalid("", "branch", "master")},

                {"branch", valid("branch"), invalid("master")},
                {"branch1,master,branch2", valid("branch2", "master", "branch1"), invalid("branch3")},

                {"  branch  \t", valid("branch"), invalid("master")},
                {"  branch1  \t,\t  branch2  ", valid("branch1", "branch2"), invalid("master")},

                {"branch*", valid("branch", "branch1", "branchfoo"), invalid("master", "foobranch", "branch/foo")},
                {"branch/*", valid("branch/foo"), invalid("master", "foobranch", "branch/foo/bar")},
                {"branch**", valid("branch", "branch/foo/bar", "branch1", "branchfoo"), invalid("master", "foobranch")},
                {"branch?", valid("branch1", "brancha"), invalid("master", "foobranch", "branch")},

                {"branch{foo,bar}", valid("branchfoo", "branchbar"), invalid("master", "branch", "branchzoo")},
                {"{fizz,buzz}branch{foo,bar}",
                        valid("fizzbranchfoo", "buzzbranchbar", "fizzbranchbar"),
                        invalid("master", "branch", "branchzoo", "branchbar", "fizzbranch")},
                {"{fizz,buzz}b{foo,bar},c{1,2},{3,4}d{5,6}",
                        valid("fizzbfoo", "buzzbbar", "c1", "c2", "3d5", "4d6"),
                        invalid("master", "branch", "d5", "d56", "3d56")},

                {"[a-d]fizz,buzz[a-d]",
                        valid("afizz", "dfizz", "buzza", "buzzd", "buzzb"),
                        invalid("fizz", "buzz", "fizze", "buzze")},
                {"[ad]fizz,buzz[ad]",
                        valid("afizz", "dfizz", "buzza", "buzzd"),
                        invalid("fizz", "buzz", "fizze", "buzze", "fizzb", "buzzb")},
                {"[!ad]fizz,buzz[!ad]",
                        invalid("efizz", "buzze", "cfizz", "buzzb"),
                        valid("fizz", "buzz", "afizz", "dfizz", "buzza", "buzzd")},
        });
    }

    private String branches;
    private String[] validBranches;
    private String[] invalidBranches;

    public BranchMatcherTest(String branches, String[] validBranches, String[] invalidBranches) {
        this.branches = branches;
        this.validBranches = validBranches;
        this.invalidBranches = invalidBranches;
    }

    @Test
    public void validValues() {
        BranchMatcher branchMatcher = new BranchMatcher(branches);

        for (String value : validBranches) {
            assertTrue(value, branchMatcher.matches(value));
        }
    }

    @Test
    public void invalidValues() {
        BranchMatcher branchMatcher = new BranchMatcher(branches);

        for (String value : invalidBranches) {
            assertFalse(value, branchMatcher.matches(value));
        }
    }

    private static String[] valid(String... values) {
        return values;
    }

    private static String[] invalid(String... values) {
        return values;
    }


}