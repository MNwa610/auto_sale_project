-- Pagination uses id as a stable tie breaker.
CREATE INDEX cars_archive_created_idx ON cars(archived, created_at DESC, id);
CREATE INDEX cars_archive_price_idx ON cars(archived, price, id);
CREATE INDEX cars_archive_year_idx ON cars(archived, year, id);
CREATE INDEX cars_archive_mileage_idx ON cars(archived, mileage, id);
