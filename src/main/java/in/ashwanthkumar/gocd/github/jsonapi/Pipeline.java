package in.ashwanthkumar.gocd.github.jsonapi;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Pipeline {

    @SerializedName("stages")
    private List<Stage> stages;

    @SerializedName("preparing_to_schedule")
    private boolean preparingToSchedule;

    @SerializedName("can_run")
    private boolean canRun;

    public Pipeline() {
    }

    public Pipeline(boolean preparingToSchedule, boolean canRun, List<Stage> stages) {
        this.stages = stages;
        this.preparingToSchedule = preparingToSchedule;
        this.canRun = canRun;
    }

    public boolean isPreparingToSchedule() {
        return preparingToSchedule;
    }

    public List<Stage> getStages() {
        return stages;
    }

    public boolean isCanRun() {
        return canRun;
    }

    public boolean isRunningOrScheduled() {
        if (preparingToSchedule) {
            return true;
        }

        if (!canRun) {
            return true;
        }

        for (Stage stage : stages) {
            if (stage.isRunningOrScheduled()) {
                return true;
            }
        }
        return false;
    }
}
