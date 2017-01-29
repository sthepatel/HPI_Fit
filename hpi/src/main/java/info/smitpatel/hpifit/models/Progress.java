package info.smitpatel.hpifit.models;

/**
 * Progress object that represents one specific day's progress for one user
 */
public class Progress {
    private String username;
    private String date;
    private String steps;
    private String milestones_day;

    public Progress() {
    }

    public Progress(String username, String date, int steps, int milestones_day) {
        this.username = username;
        this.date = date;
        this.steps = String.valueOf(steps);
        this.milestones_day = String.valueOf(milestones_day);
    }

    public String getUsername() {
        return username;
    }

    public String getDate() {
        return date;
    }

    public String getSteps() {
        return steps;
    }

    public String getMilestones_day() {
        return milestones_day;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public void setSteps(String steps) {
        this.steps = steps;
    }

    public void setMilestones_day(String milestones_day) {
        this.milestones_day = milestones_day;
    }
}


