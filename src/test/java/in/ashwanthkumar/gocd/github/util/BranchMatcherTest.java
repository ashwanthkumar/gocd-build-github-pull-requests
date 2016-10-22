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

    @Parameterized.Parameters(name = "\"{0}\"")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                // Null and empty
                {branches(null), valid(), invalid("", "branch", "master"), BranchMatcher.Mode.FAIL_EMPTY},
                {branches(null), valid("", "branch", "master"), invalid(), BranchMatcher.Mode.PASS_EMPTY},
                {branches(""), valid(), invalid("", "branch", "master"), BranchMatcher.Mode.FAIL_EMPTY},
                {branches(""), valid("", "branch", "master"), invalid(), BranchMatcher.Mode.PASS_EMPTY},
                {branches("     "), valid(), invalid("", "branch", "master"), BranchMatcher.Mode.FAIL_EMPTY},
                {branches("  \t "), valid("", "branch", "master"), invalid(), BranchMatcher.Mode.PASS_EMPTY},

                // Basic values
                {branches("branch"),
                        valid("branch"),
                        invalid("master"),
                        BranchMatcher.Mode.FAIL_EMPTY},
                {branches("branch1,master,branch2"),
                        valid("branch2", "master", "branch1"),
                        invalid("branch3"),
                        BranchMatcher.Mode.FAIL_EMPTY},

                // Whitespace
                {branches("  branch  \t"),
                        valid("branch"),
                        invalid("master"),
                        BranchMatcher.Mode.FAIL_EMPTY},
                {branches("  branch1  \t,\t  branch2  "),
                        valid("branch1", "branch2"),
                        invalid("master"),
                        BranchMatcher.Mode.FAIL_EMPTY},

                // Wildcards
                {branches("branch*"),
                        valid("branch", "branch1", "branchfoo"),
                        invalid("master", "foobranch", "branch/foo"),
                        BranchMatcher.Mode.FAIL_EMPTY},
                {branches("branch/*"),
                        valid("branch/foo"),
                        invalid("master", "foobranch", "branch/foo/bar"),
                        BranchMatcher.Mode.FAIL_EMPTY},
                {branches("branch**"),
                        valid("branch", "branch/foo/bar", "branch1", "branchfoo"),
                        invalid("master", "foobranch"),
                        BranchMatcher.Mode.FAIL_EMPTY},
                {branches("branch?"),
                        valid("branch1", "brancha"),
                        invalid("master", "foobranch", "branch"),
                        BranchMatcher.Mode.FAIL_EMPTY},

                // Braces
                {branches("branch{foo,bar}"),
                        valid("branchfoo", "branchbar"),
                        invalid("master", "branch", "branchzoo"),
                        BranchMatcher.Mode.FAIL_EMPTY},
                {branches("{fizz,buzz}branch{foo,bar}"),
                        valid("fizzbranchfoo", "buzzbranchbar", "fizzbranchbar"),
                        invalid("master", "branch", "branchzoo", "branchbar", "fizzbranch"),
                        BranchMatcher.Mode.FAIL_EMPTY},
                {branches("{fizz,buzz}b{foo,bar},c{1,2},{3,4}d{5,6}"),
                        valid("fizzbfoo", "buzzbbar", "c1", "c2", "3d5", "4d6"),
                        invalid("master", "branch", "d5", "d56", "3d56"),
                        BranchMatcher.Mode.FAIL_EMPTY},
                {branches("{fizz,buzz}b{foo,bar},c{,1,2},{3,4}d{5,6}"),
                        valid("fizzbfoo", "buzzbbar", "c", "c2", "3d5", "4d6"),
                        invalid("master", "branch", "d5", "d56", "3d56"),
                        BranchMatcher.Mode.FAIL_EMPTY},

                // Character matches
                {branches("[a-d]fizz,buzz[a-d]"),
                        valid("afizz", "dfizz", "buzza", "buzzd", "buzzb"),
                        invalid("fizz", "buzz", "fizze", "buzze"),
                        BranchMatcher.Mode.FAIL_EMPTY},
                {branches("[ad]fizz,buzz[ad]"),
                        valid("afizz", "dfizz", "buzza", "buzzd"),
                        invalid("fizz", "buzz", "fizze", "buzze", "fizzb", "buzzb"),
                        BranchMatcher.Mode.FAIL_EMPTY},
                {branches("[!ad]fizz,buzz[!ad]"),
                        invalid("efizz", "buzze", "cfizz", "buzzb"),
                        valid("fizz", "buzz", "afizz", "dfizz", "buzza", "buzzd"),
                        BranchMatcher.Mode.FAIL_EMPTY},
        });
    }

    private String branches;
    private String[] validBranches;
    private String[] invalidBranches;
    private BranchMatcher.Mode mode;

    public BranchMatcherTest(String branches, String[] validBranches, String[] invalidBranches, BranchMatcher.Mode mode) {
        this.branches = branches;
        this.validBranches = validBranches;
        this.invalidBranches = invalidBranches;
        this.mode = mode;
    }

    @Test
    public void validValues() {
        BranchMatcher branchMatcher = new BranchMatcher(branches, mode);

        for (String value : validBranches) {
            assertTrue(value, branchMatcher.matches(value));
        }
    }

    @Test
    public void invalidValues() {
        BranchMatcher branchMatcher = new BranchMatcher(branches, mode);

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

    private static String branches(String branches) {
        return branches;
    }


}