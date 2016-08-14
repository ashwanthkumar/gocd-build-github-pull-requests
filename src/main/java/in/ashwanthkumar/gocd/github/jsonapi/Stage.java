package in.ashwanthkumar.gocd.github.jsonapi;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Stage {


    @SerializedName("can_run")
    private boolean canRun;

    @SerializedName("jobs")
    private List<Job> jobs;

    public Stage() {
    }

    public Stage(boolean canRun, List<Job> jobs) {
        this.canRun = canRun;
        this.jobs = jobs;
    }


    public boolean isRunningOrScheduled() {
        if (!canRun) {
            return true;
        }

        for (Job job : jobs) {
            if (job.isRunningOrScheduled()) {
                return true;
            }
        }
        return false;
    }
}
