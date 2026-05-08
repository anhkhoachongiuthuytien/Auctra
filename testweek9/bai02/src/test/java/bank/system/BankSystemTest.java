package bank.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class BankSystemTest {
    @Test
    void shouldReadCustomersAndSortThemByDifferentOrders() {
        Bank bank = new Bank();
        String input = String.join(
                System.lineSeparator(),
                "Tran B 987654321",
                "100001 CHECKING 3500",
                "Le A 123456789",
                "200001 SAVINGS 9000");

        bank.readCustomerList(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));

        assertEquals(
                String.join(
                        System.lineSeparator(),
                        "Số CMND: 123456789. Họ tên: Le A.",
                        "Số CMND: 987654321. Họ tên: Tran B."),
                bank.getCustomersInfoByIdOrder());
        assertEquals(
                String.join(
                        System.lineSeparator(),
                        "Số CMND: 123456789. Họ tên: Le A.",
                        "Số CMND: 987654321. Họ tên: Tran B."),
                bank.getCustomersInfoByNameOrder());
    }

    @Test
    void shouldApplySavingsWithdrawalRules() {
        SavingsAccount account = new SavingsAccount(101L, 7000.0);

        account.withdraw(1500.0);
        assertEquals(7000.0, account.getBalance());
        assertTrue(account.getTransactionList().isEmpty());

        account.withdraw(1000.0);
        assertEquals(6000.0, account.getBalance());
        assertEquals(1, account.getTransactionList().size());
    }

    @Test
    void shouldFormatTransactionSummary() {
        Transaction transaction = new Transaction(
                Transaction.TYPE_DEPOSIT_CHECKING,
                250.0,
                1000.0,
                1250.0);

        assertEquals(
                "- Kiểu giao dịch: Nạp tiền vãng lai. Số dư ban đầu: $1000.00. "
                        + "Số tiền: $250.00. Số dư cuối: $1250.00.",
                transaction.getTransactionSummary());
    }
}
