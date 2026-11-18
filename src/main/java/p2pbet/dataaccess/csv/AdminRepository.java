package p2pbet.dataaccess.csv;
import p2pbet.core.entities.Admin;
import p2pbet.dataaccess.IAdminRepository;
import p2pbet.dataaccess.IDataStorage;
import java.util.Optional;
import org.springframework.stereotype.Repository;
@Repository
public class AdminRepository extends AbstractCsvRepository<Admin, Integer> implements IAdminRepository {
    public AdminRepository(IDataStorage dataStorage) {
        super(dataStorage, "admins.csv", "id,username,password");
    }
    @Override
    protected Admin mapFromString(String line) {
        String[] parts = line.split(",");
        if (parts.length == 3) {
            return new Admin(Integer.parseInt(parts[0]), parts[1], parts[2]);
        }
        return null;
    }
    @Override
    protected String mapToString(Admin admin) {
        return admin.getId() + "," + admin.getUsername() + "," + admin.getPasswordHash();
    }
    @Override
    protected Integer getId(Admin admin) {
        return admin.getId();
    }
    @Override
    public Optional<Admin> findByUsername(String username) {
        return cache.values().stream()
                .filter(a -> a.getUsername().equals(username))
                .findFirst();
    }
}
