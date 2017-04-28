-- View: issueview

-- DROP VIEW issueview;

CREATE OR REPLACE VIEW issueview AS
  SELECT issue.id,
    issue.issue_id,
    issue.status,
    issue.date_logged,
    issue.participant_id,
    issue.data_source,
    issue.priority,
    issue.data_item,
    issue.short_desc,
    issue.description,
    issue.gmc,
    issue.lsid,
    issue.area,
    issue.family_id,
    issue.query_date,
    trunc(date_part('day'::text, now()::timestamp without time zone - issue.query_date) / 7::double precision) AS weeks_open,
    issue.resolution_date,
    issue.escalation,
    issue.notes
  FROM issue;

ALTER TABLE issueview
OWNER TO postgres;