CREATE EXTENSION IF NOT EXISTS age;
LOAD 'age';
SET search_path = ag_catalog, "$user", public;

SELECT * FROM cypher('dassco', $$
    MERGE (i:Institution { name: "role-institution-1"})
    MERGE (r:Role {name: "test-role-1"})
    MERGE (i)-[:RESTRICTED_TO]->(r)
    MERGE (c:Collection {name: "role-collection-1"})
    MERGE (i)<-[:USED_BY]-(c)
    MERGE(p:Pipeline {name: "ri1_p1"})
    MERGE (p)-[:USED_BY]->(i)
    MERGE (w:Workstation {name: "ri1_w1", status: "IN_SERVICE"})
    MERGE (w)-[:STATIONED_AT]->(i)
    RETURN i
$$) as (name agtype);

SELECT * FROM ag_catalog.cypher('dassco'
    , $$
    MERGE (i:Institution {name: "role-institution-2"})
    MERGE (c:Collection {name: "role-collection-2"})
    MERGE (i)<-[:USED_BY]-(c)
    MERGE (r:Role {name: "test-role-2"})
    MERGE (c)-[:RESTRICTED_TO]->(r)
    MERGE(p:Pipeline {name: "ri2_p1"})
    MERGE (p)-[:USED_BY]->(i)
    MERGE (w:Workstation {name: "ri2_w1", status: "IN_SERVICE"})
    MERGE (w)-[:STATIONED_AT]->(i)
    RETURN i
$$) as (name agtype);
