DROP MATERIALIZED VIEW IF EXISTS ref_data_mappings;

CREATE MATERIALIZED VIEW ref_data_mappings (
    ref_data,
    value,
    ext_id
)
AS
SELECT ref_data, value, ext_id
FROM (
    SELECT 'employer_status' as ref_data, value, id as ext_id FROM (
        SELECT 'KEY_PARTNER' AS value,1 as id
        UNION ALL SELECT 'GOLD',2
        UNION ALL SELECT 'SILVER',3
    ) employer_statuses
    UNION SELECT 'employer_sector', value, id FROM (
        SELECT 'ADMIN_SUPPORT' AS value, 14 as id
        UNION ALL SELECT 'AGRICULTURE',1
        UNION ALL SELECT 'ARTS_ENTERTAINMENT', 18
        UNION ALL SELECT 'CONSTRUCTION',	6
        UNION ALL SELECT 'EDUCATION',	16
        UNION ALL SELECT 'ENERGY', 4
        UNION ALL SELECT 'FINANCE',11
        UNION ALL SELECT 'HEALTH_SOCIAL',17
        UNION ALL SELECT 'HOSPITALITY_CATERING',9
        UNION ALL SELECT 'LOGISTICS',8
        UNION ALL SELECT 'MANUFACTURING',3
        UNION ALL SELECT 'MINING',2
        UNION ALL SELECT 'OTHER',19
        UNION ALL SELECT 'PROFESSIONALS_SCIENTISTS_TECHNICIANS',13
        UNION ALL SELECT 'PROPERTY',12
        UNION ALL SELECT 'PUBLIC_ADMIN_DEFENCE',15
        UNION ALL SELECT 'WASTE_MANAGEMENT',5
        UNION ALL SELECT 'RETAIL',7
        UNION ALL SELECT 'TECHNOLOGY',10
    ) employer_sectors
    UNION SELECT 'job_source' as ref_date, value, id as ext_id FROM (
        SELECT 'DWP' as value, 4 as id
        UNION ALL SELECT 'EAB', 14
        UNION ALL SELECT 'EDUCATION', 15
        UNION ALL SELECT 'IAG', 8
        UNION ALL SELECT 'NFN', 1
        UNION ALL SELECT 'PRISON', 16
        UNION ALL SELECT 'THIRD_SECTOR', 10
        UNION ALL SELECT 'PEL', 2
        UNION ALL SELECT 'OTHER', 11
    ) job_sources
    UNION SELECT 'salary_period' as ref_date, value, id as ext_id FROM (
        SELECT 'PER_DAY' as value, 2 as id
        UNION ALL SELECT 'PER_FORTNIGHT', 4
        UNION ALL SELECT 'PER_HOUR', 1
        UNION ALL SELECT 'PER_MONTH', 5
        UNION ALL SELECT 'PER_WEEK', 3
        UNION ALL SELECT 'PER_YEAR', 6
        UNION ALL SELECT 'PER_YEAR_PRO_RATA', 7
    ) salary_periods
    UNION SELECT 'work_pattern' as ref_date, value, id as ext_id FROM (
        SELECT 'ANNUALISED_HOURS' as value, 1 as id
        UNION ALL SELECT 'COMPRESSED_HOURS', 2
        UNION ALL SELECT 'FLEXI_TIME', 3
        UNION ALL SELECT 'FLEXIBLE_SHIFTS', 4
        UNION ALL SELECT 'JOB_SHARE', 5
        UNION ALL SELECT 'STAGGERED_HOURS', 6
        UNION ALL SELECT 'TERM_TIME_HOURS', 7
        UNION ALL SELECT 'UNSOCIABLE_HOURS', 8
    ) work_patterns
    UNION SELECT 'contract_type' as ref_date, value, id as ext_id FROM (
        SELECT 'FIXED_TERM_CONTRACT' as value, 4 as id
        UNION ALL SELECT 'PERMANENT', 1
        UNION ALL SELECT 'SELF_EMPLOYMENT', 3
        UNION ALL SELECT 'TEMPORARY', 2
    ) contract_types
    UNION SELECT 'hours_per_week' as ref_date, value, id as ext_id FROM (
        SELECT 'FULL_TIME' as value, 2 as id
        UNION ALL SELECT 'FULL_TIME_40_PLUS', 1
        UNION ALL SELECT 'PART_TIME', 3
        UNION ALL SELECT 'ZERO_HOURS', 4
    ) hours_per_week
    UNION SELECT 'base_location' as ref_date, value, id as ext_id FROM (
        SELECT 'REMOTE' as value, 1 as id
        UNION ALL SELECT 'HYBRID', 3
        UNION ALL SELECT 'WORKPLACE', 2
    ) base_locations
    UNION SELECT 'offence_exclusion' as ref_date, value, id as ext_id FROM (
        SELECT 'NONE' as value, 1 as id
        UNION ALL SELECT 'CASE_BY_CASE', 15
        UNION ALL SELECT 'ARSON', 16
        UNION ALL SELECT 'DRIVING', 17
        UNION ALL SELECT 'MURDER', 18
        UNION ALL SELECT 'SEXUAL', 3
        UNION ALL SELECT 'TERRORISM', 19
        UNION ALL SELECT 'OTHER', 14
    ) offence_exclusions
) ref_data_mappings
ORDER BY 1, 2
WITH DATA;

CREATE UNIQUE INDEX ref_data_mappings_data_value_key ON ref_data_mappings(ref_data, value);
CREATE UNIQUE INDEX ref_data_mappings_data_ext_id_key ON ref_data_mappings(ref_data, ext_id);

REFRESH MATERIALIZED VIEW ref_data_mappings;
