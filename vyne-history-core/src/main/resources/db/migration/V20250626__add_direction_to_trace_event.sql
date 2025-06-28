-- Step 1: Add the new column, allowing NULLs temporarily
ALTER TABLE TRACE_EVENT
ADD COLUMN event_direction VARCHAR(50);

-- Step 2: Populate existing rows with default value 'NONE'
UPDATE TRACE_EVENT
SET event_direction = 'NONE';

-- Step 3: Make the column NOT NULL now that it's fully populated
ALTER TABLE TRACE_EVENT
ALTER COLUMN event_direction SET NOT NULL;
