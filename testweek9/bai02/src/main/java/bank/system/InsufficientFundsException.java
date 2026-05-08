package bank.system;

import java.util.Locale;

/**
 * Raised when an account does not have enough balance for a transaction.
 */
public class InsufficientFundsException extends BankException {

    /**
     * Creates a new insufficient funds exception.
     *
     * @param amount attempted transaction amount
     */
    public InsufficientFundsException(double amount) {
        super("Số dư tài khoản không đủ $" + String.format(Locale.US, "%.2f", amount)
                + " để thực hiện giao dịch");
    }
}
