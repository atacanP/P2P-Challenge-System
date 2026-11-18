package p2pbet.core.entities;
public class Wallet {
    private int userId;
    private double availableBalance;
    private double lockedBalance;
    public Wallet(int userId, double availableBalance, double lockedBalance) {
        this.userId = userId;
        this.availableBalance = availableBalance;
        this.lockedBalance = lockedBalance;
    }
    public int getUserId() {
        return userId;
    }
    public double getAvailableBalance() {
        return availableBalance;
    }
    public double getLockedBalance() {
        return lockedBalance;
    }
    public void addAvailable(double amount) {
        this.availableBalance += amount;
    }
    public void deductAvailable(double amount) {
        if (this.availableBalance < amount) {
            throw new p2pbet.core.entities.exception.InsufficientFundsException("Insufficient available balance.");
        }
        this.availableBalance -= amount;
    }
    public void lockFunds(double amount) {
        deductAvailable(amount);
        this.lockedBalance += amount;
    }
    public void releaseLocked(double amount) {
        if (this.lockedBalance < amount) {
            throw new IllegalArgumentException("Insufficient locked balance.");
        }
        this.lockedBalance -= amount;
        this.availableBalance += amount;
    }
    public void deductLocked(double amount) {
        if (this.lockedBalance < amount) {
            throw new IllegalArgumentException("Insufficient locked balance.");
        }
        this.lockedBalance -= amount;
    }
    @Override
    public String toString() {
        return "Wallet{" +
                "userId=" + userId +
                ", availableBalance=" + availableBalance +
                ", lockedBalance=" + lockedBalance +
                '}';
    }
}
