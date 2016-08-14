package in.ashwanthkumar.gocd.github.jsonapi;

import com.google.gson.annotations.SerializedName;

public class Job {

    @SerializedName("state")
    private String state;

    public Job() {
    }

    public Job(String state) {
        this.state = state;
    }

    public String getState() {
        return state;
    }

    public boolean isRunningOrScheduled() {
        return "assigned".equalsIgnoreCase(state)
                || "scheduled".equalsIgnoreCase(state)
                || "building".equalsIgnoreCase(state);

    }

    @Override
    public String toString() {
        return "Job{" +
                "state='" + state + '\'' +
                '}';
    }
}
