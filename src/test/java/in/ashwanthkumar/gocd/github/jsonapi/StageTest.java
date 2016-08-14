package in.ashwanthkumar.gocd.github.jsonapi;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(Parameterized.class)
public class StageTest {

    private static List<Job> jobsRunningOrScheduled(boolean... states) {
        List<Job> jobs = new ArrayList<>(states.length);
        for (boolean state : states) {
            Job stage = mock(Job.class);
            when(stage.isRunningOrScheduled()).thenReturn(state);
            when(stage.toString()).thenReturn("Job.isRunningOrScheduled=" + state);
            jobs.add(stage);
        }
        return jobs;
    }

    @Parameterized.Parameters(name = "Stage can run {0}, job statuses: {1}. Expected {2}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                {CanRun.YES, jobsRunningOrScheduled(false), false},
                {CanRun.NO, jobsRunningOrScheduled(true), true},
                {CanRun.YES, jobsRunningOrScheduled(true), true},
                {CanRun.YES, jobsRunningOrScheduled(false, false, true), true},
        });
    }

    private final boolean canRun;
    private final List<Job> jobs;
    private final boolean expected;

    public StageTest(CanRun canRun, List<Job> jobs, boolean expected) {
        this.canRun = canRun.equals(CanRun.YES);
        this.jobs = jobs;
        this.expected = expected;
    }

    @Test
    public void shouldReturnCorrectStatus() {
        assertThat(new Stage(canRun, jobs).isRunningOrScheduled(), is(expected));
    }
}