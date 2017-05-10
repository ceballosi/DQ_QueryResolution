-- View: issueview

-- DROP VIEW issueview;

CREATE OR REPLACE VIEW issueview AS
  SELECT
    iss.id,
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
    issdt.open_date,
    CASE
    WHEN iss.status='Open'
      THEN trunc(date_part('day' :: TEXT, now() :: TIMESTAMP WITHOUT TIME ZONE - issdt.open_date) / 7 :: DOUBLE PRECISION)
    END AS weeks_open,
    issdt.resolution_date,
    issdt.escalation,
    iss.notes
  FROM issue AS iss
    LEFT OUTER JOIN issuedates AS issdt
      ON iss.issue_id = issdt.issue_id;

ALTER TABLE issueview
OWNER TO postgres;