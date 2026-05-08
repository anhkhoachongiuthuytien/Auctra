package bank.system;

import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a single banking transaction.
 */
public class Transaction {
    /**
     * Transaction type for checking deposit.
     */
    public static final int TYPE_DEPOSIT_CHECKING = 1;

    /**
     * Transaction type for checking withdrawal.
     */
    public static final int TYPE_WITHDRAW_CHECKING = 2;

    /**
     * Transaction type for savings deposit.
     */
    public static final int TYPE_DEPOSIT_SAVINGS = 3;

    /**
     * Transaction type for savings withdrawal.
     */
    public static final int TYPE_WITHDRAW_SAVINGS = 4;

    private static final Logger LOGGER = LoggerFactory.getLogger(Transaction.class);
    private static final Locale LOCALE = Locale.US;

    private int type;
    private double amount;
    private double initialBalance;
    private double finalBalance;

    /**
     * Creates a transaction snapshot.
     *
     * @param type transaction type constant
     * @param amount transaction amount
     * @param initialBalance balance before the transaction
     * @param finalBalance balance after the transaction
     */
    public Transaction(
            int type,
            double amount,
            double initialBalance,
            double finalBalance) {
        this.type = type;
        this.amount = amount;
        this.initialBalance = initialBalance;
        this.finalBalance = finalBalance;
    }

    /**
     * Returns the transaction type.
     *
     * @return transaction type constant
     */
    public int getType() {
        return type;
    }

    /**
     * Updates the transaction type.
     *
     * @param type transaction type constant
     */
    public void setType(int type) {
        this.type = type;
    }

    /**
     * Returns the transaction amount.
     *
     * @return transaction amount
     */
    public double getAmount() {
        return amount;
    }

    /**
     * Updates the transaction amount.
     *
     * @param amount transaction amount
     */
    public void setAmount(double amount) {
        this.amount = amount;
    }

    /**
     * Returns the balance before the transaction.
     *
     * @return starting balance
     */
    public double getInitialBalance() {
        return initialBalance;
    }

    /**
     * Updates the starting balance.
     *
     * @param initialBalance starting balance
     */
    public void setInitialBalance(double initialBalance) {
        this.initialBalance = initialBalance;
    }

    /**
     * Returns the balance after the transaction.
     *
     * @return ending balance
     */
    public double getFinalBalance() {
        return finalBalance;
    }

    /**
     * Updates the ending balance.
     *
     * @param finalBalance ending balance
     */
    public void setFinalBalance(double finalBalance) {
        this.finalBalance = finalBalance;
    }

    /**
     * Maps a transaction type to a human-readable label.
     *
     * @param transactionType transaction type constant
     * @return Vietnamese description of the transaction type
     */
    public static String getTypeString(int transactionType) {
        return switch (transactionType) {
            case TYPE_DEPOSIT_CHECKING -> "Nạp tiền vãng lai";
            case TYPE_WITHDRAW_CHECKING -> "Rút tiền vãng lai";
            case TYPE_DEPOSIT_SAVINGS -> "Nạp tiền tiết kiệm";
            case TYPE_WITHDRAW_SAVINGS -> "Rút tiền tiết kiệm";
            default -> "Không rõ";
        };
    }

    /**
     * Returns a formatted summary for the transaction.
     *
     * @return transaction summary text
     */
    public String getTransactionSummary() {
        LOGGER.debug("Generating summary for transactionType={}", type);
        return String.format(
                LOCALE,
                "- Kiểu giao dịch: %s. Số dư ban đầu: $%.2f. Số tiền: $%.2f. "
                        + "Số dư cuối: $%.2f.",
                getTypeString(type),
                initialBalance,
                amount,
                finalBalance);
    }
}
