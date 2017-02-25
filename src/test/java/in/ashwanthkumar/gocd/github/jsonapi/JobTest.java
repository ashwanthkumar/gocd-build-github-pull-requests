package in.ashwanthkumar.gocd.github.jsonapi;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(Parameterized.class)
public class JobTest {

    @Parameterized.Parameters(name = "Job state {0}. Job running or scheduled expected {1}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                {"assigned", true},
                {"building", true},
                {"scheduled", true},
                {"completed", false},
        });
    }

    private final String state;
    private final boolean result;

    public JobTest(String state, boolean result) {
        this.state = state;
        this.result = result;
    }

    @Test
    public void shouldReturnCorrectBuildingStatus() {
        assertThat(new Job(state).isRunningOrScheduled(), is(result));
    }

}