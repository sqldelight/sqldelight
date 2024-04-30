CREATE TABLE animals (
    id BIGINT AUTO_INCREMENT,
    name VARCHAR(30) NOT NULL,
    species VARCHAR(30) NOT NULL,
    UNIQUE KEY unq_name (name),
    KEY idx_species (species)
);

ALTER TABLE animals
  RENAME INDEX `unq_name` TO `unq_animals_name`,
  RENAME KEY `idx_species` TO `idx_animals_species`;

ALTER TABLE animals
  DROP INDEX `unq_animals_name`;
