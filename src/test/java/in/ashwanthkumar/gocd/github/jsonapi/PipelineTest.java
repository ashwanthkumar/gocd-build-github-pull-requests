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
public class PipelineTest {

    private enum PreparingToSchedule {
        YES, NO
    }

    private static List<Stage> stagesRunningOrScheduled(boolean... states) {
        List<Stage> stages = new ArrayList<>(states.length);
        for (boolean state : states) {
            Stage stage = mock(Stage.class);
            when(stage.isRunningOrScheduled()).thenReturn(state);
            when(stage.toString()).thenReturn("Stage.isRunningOrScheduled=" + state);
            stages.add(stage);
        }
        return stages;
    }

    @Parameterized.Parameters(name = "Pipeline can run: {0}, preparing to schedule: {1}, stages: {2}. Expected {3}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                {PreparingToSchedule.YES, CanRun.YES, stagesRunningOrScheduled(true), true},
                {PreparingToSchedule.YES, CanRun.NO, stagesRunningOrScheduled(true), true},
                {PreparingToSchedule.NO, CanRun.YES, stagesRunningOrScheduled(true), true},
                {PreparingToSchedule.NO, CanRun.NO, stagesRunningOrScheduled(true), true},

                {PreparingToSchedule.YES, CanRun.YES, stagesRunningOrScheduled(false), true},
                {PreparingToSchedule.YES, CanRun.NO, stagesRunningOrScheduled(false), true},
                {PreparingToSchedule.NO, CanRun.YES, stagesRunningOrScheduled(false), false},
                {PreparingToSchedule.NO, CanRun.NO, stagesRunningOrScheduled(false), true},
        });
    }

    private final boolean preparingToSchedule;
    private final boolean canRun;
    private final List<Stage> stages;
    private final boolean expected;

    public PipelineTest(PreparingToSchedule preparingToSchedule, CanRun canRun, List<Stage> stages, boolean expected) {
        this.preparingToSchedule = preparingToSchedule.equals(PreparingToSchedule.YES);
        this.canRun = canRun.equals(CanRun.YES);
        this.stages = stages;
        this.expected = expected;
    }

    @Test
    public void shouldReturnCorrectStatus() {
        assertThat(new Pipeline(preparingToSchedule, canRun, stages)
                .isRunningOrScheduled(), is(expected));
    }

}