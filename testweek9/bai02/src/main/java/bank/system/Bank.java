package bank.system;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bank aggregate that manages customers and imports data.
 */
public class Bank {
    private static final int TOKEN_START_OFFSET = 1;
    private static final int ACCOUNT_NUMBER_INDEX = 0;
    private static final int ACCOUNT_TYPE_INDEX = 1;
    private static final int ACCOUNT_BALANCE_INDEX = 2;
    private static final Logger LOGGER = LoggerFactory.getLogger(Bank.class);
    private static final Pattern CUSTOMER_ID_PATTERN = Pattern.compile("\\d{9}");
    private static final int CUSTOMER_LINE_PARTS = 3;

    private List<Customer> customerList;

    /**
     * Creates an empty bank.
     */
    public Bank() {
        this.customerList = new ArrayList<>();
    }

    /**
     * Returns the customer list.
     *
     * @return list of customers
     */
    public List<Customer> getCustomerList() {
        return customerList;
    }

    /**
     * Replaces the customer list.
     *
     * @param customerList new customer list
     */
    public void setCustomerList(List<Customer> customerList) {
        if (customerList == null) {
            this.customerList = new ArrayList<>();
            return;
        }
        this.customerList = new ArrayList<>(customerList);
    }

    /**
     * Reads customers and accounts from a text stream.
     *
     * @param inputStream source stream
     */
    public void readCustomerList(InputStream inputStream) {
        if (inputStream == null) {
            LOGGER.warn("Customer import skipped because inputStream is null");
            return;
        }

        LOGGER.info("Starting customer import");
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            Customer currentCustomer = null;
            String line;
            while ((line = reader.readLine()) != null) {
                currentCustomer = processInputLine(line, currentCustomer);
            }
            LOGGER.info("Customer import completed customerCount={}", customerList.size());
        } catch (IOException | NumberFormatException exception) {
            LOGGER.error("Unable to import customer data", exception);
        }
    }

    /**
     * Returns customer info sorted by identity number.
     *
     * @return customer information sorted by id
     */
    public String getCustomersInfoByIdOrder() {
        List<Customer> sortedCustomers = new ArrayList<>(customerList);
        sortedCustomers.sort(Comparator.comparingLong(Customer::getIdNumber));
        return buildCustomerInfo(sortedCustomers);
    }

    /**
     * Returns customer info sorted by full name and then identity number.
     *
     * @return customer information sorted by name
     */
    public String getCustomersInfoByNameOrder() {
        List<Customer> sortedCustomers = new ArrayList<>(customerList);
        sortedCustomers.sort(Comparator
                .comparing(Customer::getFullName)
                .thenComparingLong(Customer::getIdNumber));
        return buildCustomerInfo(sortedCustomers);
    }

    /**
     * Processes one input line and returns the active customer context.
     *
     * @param rawLine current input line
     * @param currentCustomer active customer context
     * @return updated customer context
     */
    private Customer processInputLine(String rawLine, Customer currentCustomer) {
        String line = rawLine.trim();
        if (line.isEmpty()) {
            return currentCustomer;
        }

        if (isCustomerLine(line)) {
            Customer customer = createCustomer(line);
            customerList.add(customer);
            LOGGER.debug(
                    "Imported customer idNumber={} fullName={}",
                    customer.getIdNumber(),
                    customer.getFullName());
            return customer;
        }

        if (currentCustomer == null) {
            LOGGER.warn("Ignored account line without customer context line={}", line);
            return null;
        }

        addAccountLine(currentCustomer, line);
        return currentCustomer;
    }

    /**
     * Checks whether a line defines a customer.
     *
     * @param line trimmed input line
     * @return true when the line ends with a 9-digit identity number
     */
    private boolean isCustomerLine(String line) {
        int lastWhitespace = line.lastIndexOf(' ');
        if (lastWhitespace < 0) {
            return false;
        }
        int tokenStartIndex = lastWhitespace + TOKEN_START_OFFSET;
        String token = line.substring(tokenStartIndex).trim();
        return CUSTOMER_ID_PATTERN.matcher(token).matches();
    }

    /**
     * Creates a customer from a customer line.
     *
     * @param line customer line
     * @return parsed customer
     */
    private Customer createCustomer(String line) {
        int lastWhitespace = line.lastIndexOf(' ');
        int tokenStartIndex = lastWhitespace + TOKEN_START_OFFSET;
        String fullName = line.substring(0, lastWhitespace).trim();
        String idToken = line.substring(tokenStartIndex).trim();
        return new Customer(Long.parseLong(idToken), fullName);
    }

    /**
     * Parses and attaches an account line to a customer.
     *
     * @param customer owner of the account
     * @param line account line
     */
    private void addAccountLine(Customer customer, String line) {
        String[] parts = line.split("\\s+");
        if (parts.length < CUSTOMER_LINE_PARTS) {
            LOGGER.warn("Ignored malformed account line line={}", line);
            return;
        }

        long accountNumber = Long.parseLong(parts[ACCOUNT_NUMBER_INDEX]);
        String accountType = parts[ACCOUNT_TYPE_INDEX].toUpperCase(Locale.ROOT);
        double balance = Double.parseDouble(parts[ACCOUNT_BALANCE_INDEX]);
        Account account = createAccount(accountNumber, accountType, balance);
        if (account == null) {
            LOGGER.warn(
                    "Ignored unknown account type customerId={} accountType={}",
                    customer.getIdNumber(),
                    accountType);
            return;
        }

        customer.addAccount(account);
        LOGGER.debug(
                "Imported account customerId={} accountNumber={} accountType={} balance={}",
                customer.getIdNumber(),
                accountNumber,
                accountType,
                balance);
    }

    /**
     * Creates an account from parsed data.
     *
     * @param accountNumber account number
     * @param accountType account type label
     * @param balance initial balance
     * @return account instance or null for unknown types
     */
    private Account createAccount(long accountNumber, String accountType, double balance) {
        return switch (accountType) {
            case Account.CHECKING_TYPE -> new CheckingAccount(accountNumber, balance);
            case Account.SAVINGS_TYPE -> new SavingsAccount(accountNumber, balance);
            default -> null;
        };
    }

    /**
     * Builds the report text for a list of customers.
     *
     * @param customers customers to render
     * @return formatted text report
     */
    private String buildCustomerInfo(List<Customer> customers) {
        StringJoiner joiner = new StringJoiner(System.lineSeparator());
        for (Customer customer : customers) {
            joiner.add(customer.getCustomerInfo());
        }
        return joiner.toString();
    }
}
