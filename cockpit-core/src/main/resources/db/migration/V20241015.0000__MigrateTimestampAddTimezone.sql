-- Step 1: Add temporary columns for the start_time and end_time with TIMESTAMP WITH TIME ZONE
ALTER TABLE query_summary
   ADD COLUMN start_time_utc TIMESTAMP WITH TIME ZONE,
   ADD COLUMN end_time_utc   TIMESTAMP WITH TIME ZONE;

-- Step 2: Update the new columns with existing values interpreted as UTC
UPDATE query_summary
SET start_time_utc = start_time AT TIME ZONE 'UTC',
    end_time_utc   = end_time AT TIME ZONE 'UTC';

-- Step 3: Drop the old columns
ALTER TABLE query_summary
   DROP COLUMN start_time,
   DROP COLUMN end_time;

-- Step 4: Rename the temporary columns to the original column names
ALTER TABLE query_summary
RENAME COLUMN start_time_utc TO start_time;

ALTER TABLE query_summary
RENAME COLUMN end_time_utc TO end_time;



-- Step 1: Do the same for remote call response:
ALTER TABLE remote_call_response
   ADD COLUMN start_time_utc TIMESTAMP WITH TIME ZONE;

-- Step 2: Update the new column with existing values interpreted as UTC
UPDATE remote_call_response
SET start_time_utc = start_time AT TIME ZONE 'UTC';

-- Step 3: Drop the old start_time column
ALTER TABLE remote_call_response
   DROP COLUMN start_time;

-- Step 4: Rename the temporary column to the original column name
ALTER TABLE remote_call_response
   RENAME COLUMN start_time_utc TO start_time;
