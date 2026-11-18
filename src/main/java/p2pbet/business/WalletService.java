package p2pbet.business;
import p2pbet.core.entities.Wallet;
import p2pbet.dataaccess.IWalletRepository;
import org.springframework.stereotype.Service;
@Service
public class WalletService {
    private final IWalletRepository walletRepository;
    public WalletService(IWalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }
    public synchronized Wallet createWallet(int userId, double initialBalance) {
        Wallet wallet = new Wallet(userId, initialBalance, 0.0);
        walletRepository.save(wallet);
        return wallet;
    }
    public synchronized void deposit(int userId, double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Deposit amount must be positive.");
        Wallet wallet = walletRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found for user: " + userId));
        wallet.addAvailable(amount);
        walletRepository.save(wallet);
    }
    public synchronized void withdraw(int userId, double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Withdraw amount must be positive.");
        Wallet wallet = walletRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found for user: " + userId));
        if (wallet.getAvailableBalance() < amount) {
            throw new IllegalArgumentException("Insufficient available funds for withdrawal.");
        }
        wallet.deductAvailable(amount);
        walletRepository.save(wallet);
    }
    public double getBalance(int userId) {
        Wallet wallet = getWallet(userId);
        return wallet.getAvailableBalance();
    }
    public Wallet getWallet(int userId) {
        return walletRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found for user: " + userId));
    }
    public synchronized void reserveFunds(int userId, double amount) {
        Wallet wallet = walletRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found for user: " + userId));
        wallet.lockFunds(amount);
        walletRepository.save(wallet);
        System.out.println("Reserved " + amount + " for user " + userId + ". New available: " + wallet.getAvailableBalance() + ", Locked: " + wallet.getLockedBalance());
    }
    public synchronized void refundFunds(int userId, double amount) {
        Wallet wallet = walletRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found for user: " + userId));
        wallet.releaseLocked(amount);
        walletRepository.save(wallet);
        System.out.println("Refunded " + amount + " to user " + userId + ". New available: " + wallet.getAvailableBalance() + ", Locked: " + wallet.getLockedBalance());
    }
    public synchronized void releaseAndTransfer(int fromUserId, int toUserId, double amount) {
        Wallet fromWallet = walletRepository.findById(fromUserId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found for user: " + fromUserId));
        Wallet toWallet = walletRepository.findById(toUserId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found for user: " + toUserId));
        fromWallet.deductLocked(amount);
        toWallet.addAvailable(amount);
        walletRepository.save(fromWallet);
        walletRepository.save(toWallet);
    }
    public synchronized void adminUpdateBalance(int adminId, int userId, double newBalance) {
        if (newBalance < 0) throw new IllegalArgumentException("Balance cannot be negative.");
        double current = getBalance(userId);
        double diff = newBalance - current;
        if (diff > 0) {
            deposit(userId, diff);
        } else if (diff < 0) {
            withdraw(userId, -diff);
        }
        System.out.println("Admin " + adminId + " updated balance of user " + userId + " to " + newBalance);
    }
}
