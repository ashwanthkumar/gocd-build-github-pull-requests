package in.ashwanthkumar.gocd.github.jsonapi;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class PipelineHistory {

    @SerializedName("pipelines")
    private List<Pipeline> pipelines;

    public PipelineHistory() {
    }

    public PipelineHistory(List<Pipeline> pipelines) {
        this.pipelines = pipelines;
    }

    public List<Pipeline> getPipelines() {
        return pipelines;
    }

    boolean isPipelineRunningOrScheduled() {
        for (Pipeline pipeline : pipelines) {
            if (pipeline.isRunningOrScheduled()) {
                return true;
            }
        }
        return false;
    }
}
