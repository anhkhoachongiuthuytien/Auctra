package com.auction.server;

import com.auction.db.DatabaseManager;
import com.auction.util.PasswordHasher;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

/** Offline account recovery, accessible only to an operator with access to the server database. */
public final class RecoveryCodeMain {
    private RecoveryCodeMain() { }

    public static void main(String[] args) {
        if (args.length < 1 || args.length > 2) {
            System.err.println("Usage: java -jar auction-server.jar --recovery-code <email> [database-path]");
            return;
        }
        String databasePath = args.length == 2 ? args[1] : "auction-system.db";
        DatabaseManager database = new DatabaseManager("jdbc:sqlite:" + databasePath);
        database.initializeSchema();
        String code = UUID.randomUUID().toString().replace("-", "");
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement("UPDATE users SET recovery_code_hash = ? WHERE email = ?")) {
            statement.setString(1, PasswordHasher.hash(code));
            statement.setString(2, args[0]);
            if (statement.executeUpdate() != 1) {
                throw new IllegalArgumentException("Không tìm thấy tài khoản");
            }
            System.out.println("Mã khôi phục dùng một lần: " + code);
        } catch (SQLException exception) {
            throw new IllegalStateException("Không thể cấp mã khôi phục", exception);
        }
    }
}
