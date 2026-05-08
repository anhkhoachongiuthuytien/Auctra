package bank.system;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Savings account implementation with stricter withdrawal rules.
 */
public class SavingsAccount extends Account {
    private static final Logger LOGGER = LoggerFactory.getLogger(SavingsAccount.class);
    private static final double MAX_WITHDRAW = 1000.0;
    private static final double MIN_BALANCE = 5000.0;

    /**
     * Creates a savings account.
     *
     * @param accountNumber account number
     * @param balance starting balance
     */
    public SavingsAccount(long accountNumber, double balance) {
        super(accountNumber, balance);
    }

    /**
     * Deposits money into a savings account.
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
                    Transaction.TYPE_DEPOSIT_SAVINGS,
                    amount,
                    initialBalance,
                    finalBalance);
            addTransaction(transaction);
            LOGGER.info(
                    "Savings deposit completed accountNumber={} amount={} finalBalance={}",
                    getAccountNumber(),
                    amount,
                    finalBalance);
        } catch (BankException exception) {
            LOGGER.warn(
                    "Savings deposit rejected accountNumber={} amount={} reason={}",
                    getAccountNumber(),
                    amount,
                    exception.getMessage());
        }
    }

    /**
     * Withdraws money from a savings account.
     *
     * @param amount withdrawal amount
     */
    @Override
    public void withdraw(double amount) {
        double initialBalance = getBalance();
        try {
            validateSavingsWithdrawal(amount, initialBalance);
            doWithdrawing(amount);
            double finalBalance = getBalance();
            Transaction transaction = new Transaction(
                    Transaction.TYPE_WITHDRAW_SAVINGS,
                    amount,
                    initialBalance,
                    finalBalance);
            addTransaction(transaction);
            LOGGER.info(
                    "Savings withdrawal completed accountNumber={} amount={} finalBalance={}",
                    getAccountNumber(),
                    amount,
                    finalBalance);
        } catch (BankException exception) {
            LOGGER.warn(
                    "Savings withdrawal rejected accountNumber={} amount={} reason={}",
                    getAccountNumber(),
                    amount,
                    exception.getMessage());
        }
    }

    /**
     * Validates the savings-specific withdrawal rules.
     *
     * @param amount withdrawal amount
     * @param initialBalance balance before withdrawal
     * @throws BankException when the withdrawal breaks account rules
     */
    private void validateSavingsWithdrawal(double amount, double initialBalance)
            throws BankException {
        if (amount > MAX_WITHDRAW) {
            throw new InvalidFundingAmountException(amount);
        }
        if (initialBalance - amount < MIN_BALANCE) {
            throw new InsufficientFundsException(amount);
        }
    }
}
