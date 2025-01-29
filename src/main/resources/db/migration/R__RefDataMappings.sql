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
    UNION
    SELECT 'employer_sector', value, id FROM (
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
) ref_data_mappings
ORDER BY 1, 2
WITH DATA;

CREATE UNIQUE INDEX ref_data_mappings_data_value_key ON ref_data_mappings(ref_data, value);
CREATE UNIQUE INDEX ref_data_mappings_data_ext_id_key ON ref_data_mappings(ref_data, ext_id);

REFRESH MATERIALIZED VIEW ref_data_mappings;
