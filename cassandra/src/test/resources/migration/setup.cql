CREATE KEYSPACE IF NOT EXISTS cassandra WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};

CREATE TABLE IF NOT EXISTS cassandra.users
(
    id INT,
    PRIMARY KEY (id)
);