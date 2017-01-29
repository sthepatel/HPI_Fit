package info.smitpatel.hpifit.models;

import java.io.Serializable;

/**
 * An object that represents all user data
 */
public class UserProfile implements Serializable{
    private String username;
    private String password;
    private String firstName;
    private String lastName;
    private String profilePic;
    private String milestonesCount;

    public UserProfile(){}

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getProfilePic() {
        if(profilePic == null) {
            return "";
        }
        return profilePic;
    }

    public int getMilestonesCount() {
        return Integer.valueOf(milestonesCount);
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setProfilePic(String profilePic) {
        if(profilePic == null) {
            this.profilePic = "";
            return;
        }
        this.profilePic = profilePic;
    }

    public void setMilestonesCount(String milestonesCount) {
        this.milestonesCount = milestonesCount;
    }

}
