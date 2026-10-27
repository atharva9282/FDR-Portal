-- Existing installations may have an older Hibernate-generated status check
-- that omits RENEWED. Repair that check before the application handles requests.
-- Fresh databases do not have the table yet; Hibernate will create it from the
-- current InstrumentStatus enum during application startup.
DO $migration$
BEGIN
    IF to_regclass('public.fdr_instruments') IS NOT NULL THEN
        ALTER TABLE public.fdr_instruments
            DROP CONSTRAINT IF EXISTS fdr_instruments_status_check;

        ALTER TABLE public.fdr_instruments
            ADD CONSTRAINT fdr_instruments_status_check
            CHECK (status IN (
                'SUBMITTED',
                'UNDER_REVIEW',
                'ACTION_REQUIRED',
                'ACTIVE',
                'RENEWAL_PENDING',
                'RELEASE_REQUESTED',
                'RENEWED',
                'RELEASED',
                'REJECTED'
            ));
    END IF;
END
$migration$;
