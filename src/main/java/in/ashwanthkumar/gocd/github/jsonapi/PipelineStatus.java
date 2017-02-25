package in.ashwanthkumar.gocd.github.jsonapi;

import com.google.gson.annotations.SerializedName;

public class PipelineStatus {

    @SerializedName("schedulable")
    public boolean schedulable;

    public PipelineStatus() {
    }

    public PipelineStatus(boolean schedulable) {
        this.schedulable = schedulable;
    }
}
