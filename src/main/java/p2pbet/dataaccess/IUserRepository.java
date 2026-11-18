package p2pbet.dataaccess;
import p2pbet.core.entities.User;
import java.util.Optional;
public interface IUserRepository extends IRepository<User, Integer> {
    Optional<User> findByUsername(String username);
}
