CREATE EXTENSION IF NOT EXISTS age;
LOAD 'age';
SET search_path = ag_catalog, "$user", public;

--Institutions
SELECT * FROM ag_catalog.cypher('dassco'
                  , $$
                            MERGE (i:Institution {name: "institution_1"})
                            RETURN i
                        $$) as (name agtype);

SELECT * FROM ag_catalog.cypher('dassco'
                  , $$
                            MERGE (i:Institution {name: "institution_2"})
                            RETURN i
                        $$) as (name agtype);

--Collections
SELECT * FROM ag_catalog.cypher('dassco'
                  , $$
                            MERGE (i:Institution {name: "institution_1"})
                            MERGE (c:Collection {name: "i1_c1"})
                            MERGE (i)<-[:USED_BY]-(c)
                            RETURN i.name, c.name
                        $$) as (institution_name agtype, collection_name agtype);

SELECT * FROM ag_catalog.cypher('dassco'
                  , $$
                            MERGE (i:Institution {name: "institution_1"})
                            MERGE (c:Collection {name: "i1_c2"})
                            MERGE (i)<-[:USED_BY]-(c)
                            RETURN i.name, c.name
                        $$) as (institution_name agtype, collection_name agtype);

SELECT * FROM ag_catalog.cypher('dassco'
                  , $$
                            MERGE (i:Institution {name: "institution_2"})
                            MERGE (c:Collection {name: "i2_c1"})
                            MERGE (i)<-[:USED_BY]-(c)
                            RETURN i.name, c.name
                        $$) as (institution_name agtype, collection_name agtype);

-- Workstations
SELECT * FROM ag_catalog.cypher('dassco'
                  , $$
                            MERGE (i:Institution {name: "institution_1"})
                            MERGE (c:Workstation {name: "i1_w1", status: "IN_SERVICE"})
                            MERGE (i)<-[:STATIONED_AT]-(c)
                            RETURN i.name, c.name
                        $$) as (institution_name agtype, workstation_name agtype);

SELECT * FROM ag_catalog.cypher('dassco'
                  , $$
                            MERGE (i:Institution {name: "institution_1"})
                            MERGE (c:Workstation {name: "i1_w2", status: "IN_SERVICE"})
                            MERGE (i)<-[:STATIONED_AT]-(c)
                            RETURN i.name, c.name
                        $$) as (institution_name agtype, workstation_name agtype);

SELECT * FROM ag_catalog.cypher('dassco'
                  , $$
                            MERGE (i:Institution {name: "institution_1"})
                            MERGE (c:Workstation {name: "i1_w3", status: "OUT_OF_SERVICE"})
                            MERGE (i)<-[:STATIONED_AT]-(c)
                            RETURN i.name, c.name
                        $$) as (institution_name agtype, workstation_name agtype);

SELECT * FROM ag_catalog.cypher('dassco'
                  , $$
                            MERGE (i:Institution {name: "institution_2"})
                            MERGE (c:Workstation {name: "i2_w1", status: "IN_SERVICE"})
                            MERGE (i)<-[:STATIONED_AT]-(c)
                            RETURN i.name, c.name
                        $$) as (institution_name agtype, workstation_name agtype);