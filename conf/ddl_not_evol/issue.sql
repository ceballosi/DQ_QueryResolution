
-- Table: "issue"

-- DROP TABLE "issue";


CREATE TABLE issue
(
  id BIGSERIAL,
  issue_id VARCHAR(50) NOT NULL UNIQUE,
  status VARCHAR(50) NOT NULL,
  date_logged timestamp without time zone NOT NULL,
  participant_id integer NOT NULL,
  data_source VARCHAR(50) NOT NULL, -- origin
  priority integer NOT NULL,
  data_item VARCHAR(50) NOT NULL,
  short_desc VARCHAR(50) NOT NULL,
  description VARCHAR(500) NOT NULL,
  gmc VARCHAR(50) NOT NULL,
  lsid VARCHAR(50),
  area VARCHAR(50),                 -- RD / Cancer
  family_id VARCHAR(50),
  notes VARCHAR(500),
  CONSTRAINT "issue_pkey1" PRIMARY KEY (id)
)
WITH (
OIDS=FALSE
);
ALTER TABLE issue
OWNER TO postgres;
