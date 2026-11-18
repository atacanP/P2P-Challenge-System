package p2pbet.dataaccess.csv;
import p2pbet.core.entities.User;
import p2pbet.dataaccess.IDataStorage;
import p2pbet.dataaccess.IUserRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;
@Repository
public class UserRepository extends AbstractCsvRepository<User, Integer> implements IUserRepository {
    public UserRepository(IDataStorage dataStorage) {
        super(dataStorage, "users.csv", "uuid,username,hashedPassword,email,resetToken");
    }
    @Override
    protected User mapFromString(String line) {
        String[] parts = line.split(",", -1);
        if (parts.length >= 5) {
            return new User(Integer.parseInt(parts[0]), parts[1], parts[2], parts[3], parts[4]);
        }
        return null;
    }
    @Override
    protected String mapToString(User user) {
        return user.getId() + "," + user.getUsername() + "," + user.getPasswordHash() + "," + user.getEmail() + "," + (user.getResetToken() == null ? "" : user.getResetToken());
    }
    @Override
    protected Integer getId(User user) {
        return user.getId();
    }
    @Override
    public Optional<User> findByUsername(String username) {
        return cache.values().stream()
                .filter(u -> u.getUsername().equals(username))
                .findFirst();
    }
    public Optional<User> findByEmail(String email) {
        return cache.values().stream()
                .filter(u -> u.getEmail() != null && u.getEmail().equals(email))
                .findFirst();
    }
    public Optional<User> findByResetToken(String resetToken) {
        return cache.values().stream()
                .filter(u -> u.getResetToken() != null && !u.getResetToken().isEmpty() && u.getResetToken().equals(resetToken))
                .findFirst();
    }
}
