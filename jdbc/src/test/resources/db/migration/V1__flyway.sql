CREATE TABLE IF NOT EXISTS users
(
    id INT PRIMARY KEY,
    email VARCHAR NULL,
    username VARCHAR NULL,
    status VARCHAR NULL
);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_users_email ON users (email);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_users_username ON users (username);