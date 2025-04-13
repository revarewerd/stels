-- Table: geozones

-- DROP TABLE geozones;

CREATE TABLE IF NOT EXISTS geozones
(
  id serial NOT NULL,
  username character varying(100),
  name character varying(100),
  color character(10),
  points geography(Polygon,4326),
  instance character varying(100),
  CONSTRAINT geozones_pkey PRIMARY KEY (id )
)
WITH (
  OIDS=FALSE
);



