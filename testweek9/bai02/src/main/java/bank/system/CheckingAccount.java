package bank.system;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Checking account implementation.
 */
public class CheckingAccount extends Account {
    private static final Logger LOGGER = LoggerFactory.getLogger(CheckingAccount.class);

    /**
     * Creates a checking account.
     *
     * @param accountNumber account number
     * @param balance starting balance
     */
    public CheckingAccount(long accountNumber, double balance) {
        super(accountNumber, balance);
    }

    /**
     * Deposits money into a checking account.
     *
     * @param amount deposit amount
     */
    @Override
    public void deposit(double amount) {
        double initialBalance = getBalance();
        try {
            doDepositing(amount);
            double finalBalance = getBalance();
            Transaction transaction = new Transaction(
                    Transaction.TYPE_DEPOSIT_CHECKING,
                    amount,
                    initialBalance,
                    finalBalance);
            addTransaction(transaction);
            LOGGER.info(
                    "Checking deposit completed accountNumber={} amount={} finalBalance={}",
                    getAccountNumber(),
                    amount,
                    finalBalance);
        } catch (BankException exception) {
            LOGGER.warn(
                    "Checking deposit rejected accountNumber={} amount={} reason={}",
                    getAccountNumber(),
                    amount,
                    exception.getMessage());
        }
    }

    /**
     * Withdraws money from a checking account.
     *
     * @param amount withdrawal amount
     */
    @Override
    public void withdraw(double amount) {
        double initialBalance = getBalance();
        try {
            doWithdrawing(amount);
            double finalBalance = getBalance();
            Transaction transaction = new Transaction(
                    Transaction.TYPE_WITHDRAW_CHECKING,
                    amount,
                    initialBalance,
                    finalBalance);
            addTransaction(transaction);
            LOGGER.info(
                    "Checking withdrawal completed accountNumber={} amount={} finalBalance={}",
                    getAccountNumber(),
                    amount,
                    finalBalance);
        } catch (BankException exception) {
            LOGGER.warn(
                    "Checking withdrawal rejected accountNumber={} amount={} reason={}",
                    getAccountNumber(),
                    amount,
                    exception.getMessage());
        }
    }
}
