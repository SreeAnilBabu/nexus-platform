
-- Sprint 5: Employee lifecycle and joining date
-- Preserve all existing employee records.

ALTER TABLE employees
    ADD COLUMN joining_date DATE;

ALTER TABLE employees
    ADD COLUMN employment_status VARCHAR(20);

-- Existing employees have no recorded lifecycle status.
-- Backfill them as ACTIVE without inventing joining dates.
UPDATE employees
SET employment_status = 'ACTIVE'
WHERE employment_status IS NULL;

-- All future employee records require a valid status.
ALTER TABLE employees
    ALTER COLUMN employment_status SET DEFAULT 'ACTIVE';

ALTER TABLE employees
    ALTER COLUMN employment_status SET NOT NULL;

ALTER TABLE employees
    ADD CONSTRAINT chk_employees_employment_status
        CHECK (employment_status IN ('ACTIVE', 'RESIGNED', 'TERMINATED'));
