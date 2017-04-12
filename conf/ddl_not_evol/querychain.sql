CREATE TABLE querychain
(
  id bigserial,
  issue_id VARCHAR(50) NOT NULL references issue(issue_id),
  status VARCHAR(50) NOT NULL,
  comment VARCHAR(1000),
  date timestamp without time zone NOT NULL,
  username VARCHAR(50) NOT NULL,
  party_id integer,         -- GEL=0 , GMC=1
  CONSTRAINT "querychain_pkey1" PRIMARY KEY (id)
)
WITH (
OIDS=FALSE
);
ALTER TABLE querychain
OWNER TO postgres;

----------------TEMP
-- CREATE TABLE querychain
-- (
--   id bigserial,
--   issue_id VARCHAR(50) NOT NULL references loggedIssue(issue_id),
--   comment VARCHAR(1000),
--   date timestamp without time zone NOT NULL,
--   username VARCHAR(50) NOT NULL,
--   party_id integer,         -- GEL=0 , GMC=1
--   CONSTRAINT "querychain_pkey1" PRIMARY KEY (id)
-- )
-- WITH (
-- OIDS=FALSE
-- );
-- ALTER TABLE querychain
-- OWNER TO postgres;

-- 0;"RYJ-Orp-00001";"This is a test comment";"2017-04-04 15:05:07.184275";"rick";0;"Open"
-- 2;"RYJ-Orp-00001";"new test response";"2017-04-04 15:10:49.654177";"user1";1;"Responded"
-- 3;"RYJ-Orp-00001";"confirmed so closing issue";"2017-04-06 09:30:55.358257";"gel-user";0;"Closed"
