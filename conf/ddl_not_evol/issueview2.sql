-- View: issueview

-- DROP VIEW issueview;

CREATE OR REPLACE VIEW issueview2 AS
  SELECT iss.id,
    iss.issue_id,
    iss.status,
    iss.date_logged,
    iss.participant_id,
    iss.data_source,
    iss.priority,
    iss.data_item,
    iss.short_desc,
    iss.description,
    iss.gmc,
    iss.lsid,
    iss.area,
    iss.family_id,
    issd.open_date,
    trunc(date_part('day'::text, now()::timestamp without time zone - issd.open_date) / 7::double precision) AS weeks_open,
    issd.resolution_date,
    issd.escalation,
    iss.notes
  FROM issue as iss
LEFT OUTER JOIN issuedates as issd
ON iss.issue_id = issd.issue_id;

ALTER TABLE issueview2
OWNER TO postgres;