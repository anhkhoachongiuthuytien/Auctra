-- Bật kiểm tra khóa ngoại trong SQLite để các quan hệ giữa bảng được enforce.
PRAGMA foreign_keys = ON;

-- Bảng users lưu tài khoản và vai trò của người dùng trong hệ thống.
CREATE TABLE IF NOT EXISTS users (
    id TEXT PRIMARY KEY,
    username TEXT NOT NULL,
    email TEXT NOT NULL UNIQUE,
    role TEXT NOT NULL,
    password_hash TEXT NOT NULL DEFAULT '',
    shipping_address TEXT,
    phone_number TEXT,
    store_name TEXT,
    store_description TEXT,
    department TEXT,
    avatar_path TEXT
);

-- Bảng items lưu thông tin vật phẩm được đem ra đấu giá.
CREATE TABLE IF NOT EXISTS items (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT NOT NULL,
    starting_price REAL NOT NULL,
    type TEXT NOT NULL,
    image_path TEXT
);

-- Bảng auctions lưu phiên đấu giá, người bán, người thắng và trạng thái hiện tại.
CREATE TABLE IF NOT EXISTS auctions (
    id TEXT PRIMARY KEY,
    item_id TEXT NOT NULL,
    seller_id TEXT NOT NULL,
    current_price REAL NOT NULL,
    status TEXT NOT NULL,
    winner_id TEXT,
    end_time TEXT,
    FOREIGN KEY (item_id) REFERENCES items(id),
    FOREIGN KEY (seller_id) REFERENCES users(id),
    FOREIGN KEY (winner_id) REFERENCES users(id)
);

-- Bảng bids lưu lịch sử từng lần đặt giá của bidder trong mỗi auction.
CREATE TABLE IF NOT EXISTS bids (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    auction_id TEXT NOT NULL,
    bidder_id TEXT NOT NULL,
    amount REAL NOT NULL,
    bid_time TEXT NOT NULL,
    FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE,
    FOREIGN KEY (bidder_id) REFERENCES users(id)
);

-- Bảng auto_bids lưu cấu hình tự động đặt giá cho từng bidder
CREATE TABLE IF NOT EXISTS auto_bids (
    auction_id TEXT NOT NULL,
    bidder_id TEXT NOT NULL,
    max_price REAL NOT NULL,
    increment REAL NOT NULL DEFAULT 10.0,
    PRIMARY KEY (auction_id, bidder_id),
    FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE,
    FOREIGN KEY (bidder_id) REFERENCES users(id)
);
