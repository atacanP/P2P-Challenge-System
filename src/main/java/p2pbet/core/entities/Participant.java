package p2pbet.core.entities;
import java.util.Objects;
public class Participant {
    private int userId;
    private String choice;
    public Participant(int userId, String choice) {
        this.userId = userId;
        this.choice = choice;
    }
    public int getUserId() {
        return userId;
    }
    public String getChoice() {
        return choice;
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Participant that = (Participant) o;
        return userId == that.userId &&
               Objects.equals(choice, that.choice);
    }
    @Override
    public int hashCode() {
        return Objects.hash(userId, choice);
    }
    @Override
    public String toString() {
        return "Participant{" +
                "userId=" + userId +
                ", choice='" + choice + '\'' +
                '}';
    }
}
