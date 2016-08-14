package in.ashwanthkumar.gocd.github.jsonapi;

import org.junit.Test;

import java.util.List;

import static java.util.Arrays.asList;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class PipelineHistoryTest {

    @Test
    public void shouldShowPipelineRunnableWhenJobsCompleted() {

        final boolean canRunPipeline = true;
        final boolean preparingToSchedule = false;
        final boolean canRunStage = true;
        final String jobStage = "Completed";

        List<Pipeline> pipelines = asList(
                new Pipeline(
                        preparingToSchedule,
                        canRunPipeline,
                        asList(
                            new Stage(
                                        canRunStage,
                                        asList(new Job(jobStage))
                                )
                        )
                )
        );

        PipelineHistory history = new PipelineHistory(pipelines);

        assertThat(history.isPipelineRunningOrScheduled(), is(false));
    }

    @Test
    public void shouldShowPipelineNotRunnableWhenJobsAssigned() {

        final boolean canRunPipeline = true;
        final boolean preparingToSchedule = false;
        final boolean canRunStage = true;
        final String jobStage = "Assigned";

        List<Pipeline> pipelines = asList(
                new Pipeline(
                        preparingToSchedule,
                        canRunPipeline,
                        asList(
                            new Stage(
                                        canRunStage,
                                        asList(new Job(jobStage))
                                )
                        )
                )
        );

        PipelineHistory history = new PipelineHistory(pipelines);

        assertThat(history.isPipelineRunningOrScheduled(), is(true));
    }

    @Test
    public void shouldShowPipelineNotRunnableWhenJobsBuilding() {

        final boolean canRunPipeline = true;
        final boolean preparingToSchedule = false;
        final boolean canRunStage = true;
        final String jobStage = "Building";

        List<Pipeline> pipelines = asList(
                new Pipeline(
                        preparingToSchedule,
                        canRunPipeline,
                        asList(
                            new Stage(
                                        canRunStage,
                                        asList(new Job(jobStage))
                                )
                        )
                )
        );

        PipelineHistory history = new PipelineHistory(pipelines);

        assertThat(history.isPipelineRunningOrScheduled(), is(true));
    }

}