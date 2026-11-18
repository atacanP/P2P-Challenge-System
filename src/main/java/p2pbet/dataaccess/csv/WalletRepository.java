package p2pbet.dataaccess.csv;
import p2pbet.core.entities.Wallet;
import p2pbet.dataaccess.IDataStorage;
import p2pbet.dataaccess.IWalletRepository;
import org.springframework.stereotype.Repository;
@Repository
public class WalletRepository extends AbstractCsvRepository<Wallet, Integer> implements IWalletRepository {
    public WalletRepository(IDataStorage dataStorage) {
        super(dataStorage, "wallets.csv", "userId,availableBalance,lockedBalance");
    }
    @Override
    protected Wallet mapFromString(String line) {
        String[] parts = line.split(",");
        if (parts.length == 3) {
            return new Wallet(
                Integer.parseInt(parts[0]),
                Double.parseDouble(parts[1]),
                Double.parseDouble(parts[2])
            );
        }
        return null;
    }
    @Override
    protected String mapToString(Wallet wallet) {
        return wallet.getUserId() + "," + wallet.getAvailableBalance() + "," + wallet.getLockedBalance();
    }
    @Override
    protected Integer getId(Wallet wallet) {
        return wallet.getUserId();
    }
}
