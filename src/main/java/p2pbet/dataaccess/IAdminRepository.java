package p2pbet.dataaccess;
import p2pbet.core.entities.Admin;
import java.util.Optional;
public interface IAdminRepository extends IRepository<Admin, Integer> {
    Optional<Admin> findByUsername(String username);
}
