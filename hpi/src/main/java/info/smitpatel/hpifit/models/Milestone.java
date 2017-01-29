package info.smitpatel.hpifit.models;

/**
 * An object that represent a single individual Milestone on a day for a user
 */
public class Milestone {

    public interface Type {
        String FEET_1000 = "FEET_1000";
    }

    private String username;
    private String date;
    private String type;

    public Milestone(String username, String date, String type) {
        this.username = username;
        this.date = date;
        this.type = type;
    }

    public String getUsername() {
        return username;
    }

    public String getDate() {
        return date;
    }

    public String getType() {
        return type;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public void setType(String type) {
        this.type = type;
    }
}
