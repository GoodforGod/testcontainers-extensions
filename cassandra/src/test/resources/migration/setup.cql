CREATE KEYSPACE IF NOT EXISTS default WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};

CREATE TABLE IF NOT EXISTS default.users
(
    id INT,
    PRIMARY KEY (id)
);