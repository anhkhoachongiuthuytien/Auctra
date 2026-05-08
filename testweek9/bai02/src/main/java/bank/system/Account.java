package bank.system;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base abstraction for a bank account.
 */
public abstract class Account {
    /**
     * Account type label for checking accounts.
     */
    public static final String CHECKING_TYPE = "CHECKING";

    /**
     * Account type label for savings accounts.
     */
    public static final String SAVINGS_TYPE = "SAVINGS";

    private static final Logger LOGGER = LoggerFactory.getLogger(Account.class);

    private long accountNumber;
    private double balance;
    protected List<Transaction> transactions;

    /**
     * Creates a new account.
     *
     * @param accountNumber account number
     * @param balance starting balance
     */
    public Account(long accountNumber, double balance) {
        this.accountNumber = accountNumber;
        this.balance = balance;
        this.transactions = new ArrayList<>();
    }

    /**
     * Returns the account number.
     *
     * @return account number
     */
    public long getAccountNumber() {
        return accountNumber;
    }

    /**
     * Updates the account number.
     *
     * @param accountNumber account number
     */
    public void setAccountNumber(long accountNumber) {
        this.accountNumber = accountNumber;
    }

    /**
     * Returns the current balance.
     *
     * @return current balance
     */
    public double getBalance() {
        return balance;
    }

    /**
     * Returns the account transaction list.
     *
     * @return list of transactions
     */
    public List<Transaction> getTransactionList() {
        return transactions;
    }

    /**
     * Replaces the transaction list.
     *
     * @param transactionList new transaction list
     */
    public void setTransactionList(List<Transaction> transactionList) {
        if (transactionList == null) {
            this.transactions = new ArrayList<>();
            return;
        }
        this.transactions = new ArrayList<>(transactionList);
    }

    /**
     * Deposits an amount into the account.
     *
     * @param amount deposit amount
     */
    public abstract void deposit(double amount);

    /**
     * Withdraws an amount from the account.
     *
     * @param amount withdrawal amount
     */
    public abstract void withdraw(double amount);

    /**
     * Adds a transaction to the history.
     *
     * @param transaction transaction to append
     */
    public void addTransaction(Transaction transaction) {
        if (transaction != null) {
            transactions.add(transaction);
        }
    }

    /**
     * Returns the textual transaction history for the account.
     *
     * @return formatted transaction history
     */
    public String getTransactionHistory() {
        StringJoiner historyJoiner = new StringJoiner(System.lineSeparator());
        for (Transaction transaction : transactions) {
            historyJoiner.add(transaction.getTransactionSummary());
        }

        LOGGER.debug(
                "Generated transaction history for accountNumber={} transactionCount={}",
                accountNumber,
                transactions.size());
        return new StringBuilder()
                .append("Lịch sử giao dịch của tài khoản ")
                .append(accountNumber)
                .append(":\n")
                .append(historyJoiner)
                .toString();
    }

    /**
     * Validates and performs a deposit.
     *
     * @param amount deposit amount
     * @throws InvalidFundingAmountException when amount is not positive
     */
    protected void doDepositing(double amount) throws InvalidFundingAmountException {
        if (amount <= 0) {
            throw new InvalidFundingAmountException(amount);
        }
        balance += amount;
    }

    /**
     * Validates and performs a withdrawal.
     *
     * @param amount withdrawal amount
     * @throws BankException when amount is invalid or balance is insufficient
     */
    protected void doWithdrawing(double amount) throws BankException {
        if (amount <= 0) {
            throw new InvalidFundingAmountException(amount);
        }
        if (amount > balance) {
            throw new InsufficientFundsException(amount);
        }
        balance -= amount;
    }

    /**
     * Updates the balance.
     *
     * @param balance updated balance
     */
    protected void setBalance(double balance) {
        this.balance = balance;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof Account other)) {
            return false;
        }
        return accountNumber == other.accountNumber;
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountNumber);
    }
}
