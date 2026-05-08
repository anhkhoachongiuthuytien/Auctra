package bank.system;

/**
 * Base exception for the banking domain.
 */
public class BankException extends Exception {

    /**
     * Creates a new domain exception.
     *
     * @param message human-readable message for the failure
     */
    public BankException(String message) {
        super(message);
    }
}
