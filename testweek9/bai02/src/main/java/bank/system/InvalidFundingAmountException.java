package bank.system;

import java.util.Locale;

/**
 * Raised when the transaction amount is invalid.
 */
public class InvalidFundingAmountException extends BankException {

    /**
     * Creates a new invalid amount exception.
     *
     * @param amount invalid transaction amount
     */
    public InvalidFundingAmountException(double amount) {
        super("Số tiền không hợp lệ: $" + String.format(Locale.US, "%.2f", amount));
    }
}
