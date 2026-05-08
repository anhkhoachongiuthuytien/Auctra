package bank.system;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a bank customer.
 */
public class Customer {
    private long idNumber;
    private String fullName;
    private List<Account> accountList;

    /**
     * Creates an empty customer.
     */
    public Customer() {
        this(0L, "");
    }

    /**
     * Creates a customer.
     *
     * @param idNumber customer identity number
     * @param fullName customer full name
     */
    public Customer(long idNumber, String fullName) {
        this.idNumber = idNumber;
        this.fullName = fullName;
        this.accountList = new ArrayList<>();
    }

    /**
     * Returns the identity number.
     *
     * @return customer identity number
     */
    public long getIdNumber() {
        return idNumber;
    }

    /**
     * Updates the identity number.
     *
     * @param idNumber customer identity number
     */
    public void setIdNumber(long idNumber) {
        this.idNumber = idNumber;
    }

    /**
     * Returns the customer full name.
     *
     * @return customer full name
     */
    public String getFullName() {
        return fullName;
    }

    /**
     * Updates the customer full name.
     *
     * @param fullName customer full name
     */
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    /**
     * Returns the customer accounts.
     *
     * @return list of accounts
     */
    public List<Account> getAccountList() {
        return accountList;
    }

    /**
     * Replaces the customer accounts.
     *
     * @param accountList new account list
     */
    public void setAccountList(List<Account> accountList) {
        if (accountList == null) {
            this.accountList = new ArrayList<>();
            return;
        }
        this.accountList = new ArrayList<>(accountList);
    }

    /**
     * Adds a new account if it is not already present.
     *
     * @param account account to add
     */
    public void addAccount(Account account) {
        if (account == null) {
            return;
        }
        if (!accountList.contains(account)) {
            accountList.add(account);
        }
    }

    /**
     * Removes an account if present.
     *
     * @param account account to remove
     */
    public void removeAccount(Account account) {
        if (account != null) {
            accountList.remove(account);
        }
    }

    /**
     * Returns the customer information text.
     *
     * @return formatted customer text
     */
    public String getCustomerInfo() {
        return "Số CMND: " + idNumber + ". Họ tên: " + fullName + ".";
    }
}
