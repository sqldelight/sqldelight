CREATE TABLE animals (
    id BIGINT AUTO_INCREMENT,
    name VARCHAR(30) NOT NULL,
    species VARCHAR(30) NOT NULL,
    UNIQUE KEY unq_name (name),
    KEY idx_species (species)
);

CREATE TABLE animals2 (
    id BIGINT AUTO_INCREMENT,
    name VARCHAR(30) NOT NULL,
    species VARCHAR(30) NOT NULL,
    UNIQUE INDEX unq_name (name),
    INDEX idx_species (species)
);
