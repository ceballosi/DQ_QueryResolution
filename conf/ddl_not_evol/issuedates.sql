
-- Table: "issuedates"

-- DROP TABLE "issuedates";


CREATE TABLE issuedates
(
  id BIGSERIAL,
  issue_id VARCHAR(50) REFERENCES issue (issue_id),       -- foreign key constraint
  open_date timestamp without time zone,
  responded_date timestamp without time zone,
  resolution_date timestamp without time zone,
  escalation timestamp without time zone,
  open_who VARCHAR(50),
  responded_who VARCHAR(50),
  resolution_who VARCHAR(50),
  CONSTRAINT "issuedates_pkey1" PRIMARY KEY (id)
)
WITH (
OIDS=FALSE
);
ALTER TABLE issuedates
OWNER TO postgres;
