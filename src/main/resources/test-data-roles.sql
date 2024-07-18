CREATE EXTENSION IF NOT EXISTS age;
LOAD 'age';
SET search_path = ag_catalog, "$user", public;

-- Add Institution 3
SELECT * FROM cypher('dassco', $$
    MERGE (i:Institution { name: "institution_3"})
    MERGE (c:Collection {name: "i3_c1"})
    MERGE (p:Pipeline {name: "i3_p1"})
    MERGE (w:Workstation {name: "i3_w1", status: "IN_SERVICE"})
    MERGE (w)-[:STATIONED_AT]->(i)
    MERGE (i)<-[:USED_BY]-(c)
    MERGE (p)-[:USED_BY]->(i)
    RETURN i
$$) as (name agtype);

SELECT * FROM cypher('dassco', $$
    MERGE (i:Institution { name: "institution_4"})
    MERGE (c:Collection {name: "i4_c1"})
    MERGE (p:Pipeline {name: "i4_p1"})
    MERGE (w:Workstation {name: "i4_w1", status: "IN_SERVICE"})
    MERGE (w)-[:STATIONED_AT]->(i)
    MERGE (i)<-[:USED_BY]-(c)
    MERGE (p)-[:USED_BY]->(i)
    RETURN i
$$) as (name agtype);

SELECT * FROM cypher('dassco', $$
    MERGE (i:Institution { name: "institution_5"})
    MERGE (c:Collection {name: "i5_c1"})
    MERGE (p:Pipeline {name: "i5_p1"})
    MERGE (w:Workstation {name: "i5_w1", status: "IN_SERVICE"})
    MERGE (w)-[:STATIONED_AT]->(i)
    MERGE (i)<-[:USED_BY]-(c)
    MERGE (p)-[:USED_BY]->(i)
    RETURN i
$$) as (name agtype);

-- Add Roles:
SELECT * from cypher('dassco', $$
    MATCH(i: Institution { name: "institution_3" })
    MATCH(c: Collection { name: "i4_c1"})
    MERGE(r1: Role {name: "role-1"})
    MERGE(r2: Role {name: "role-2"})
    MERGE(i)-[:RESTRICTED_TO]->(r1)
    MERGE(c)-[:RESTRICTED_TO]->(r2)
    RETURN i
$$) as (name agtype);

-- Add Users:
SELECT * from cypher('dassco', $$
    MERGE (u1:User {name: "role-1-user", user_id: "role-1-user"})
    MERGE (u2:User {name: "role-2-user", user_id: "role-2-user"})
    MERGE (u3:User {name: "service-user", user_id: "service-user"})
    RETURN u1
$$) as (name agtype);

-- Add Assets:
SELECT * from cypher('dassco', $$
    MATCH (u:User { name: "service-user"})
    MATCH(i:Institution {name: "institution_3"})
    MATCH(c:Collection {name: "i3_c1"})
    MATCH(w:Workstation {name: "i3_w1"})
    MATCH(p:Pipeline {name: "i3_p1"})
    MERGE (a:Asset { asset_pid: "asset-1", asset_guid: "asset-1", name: "asset-1", tags:{}, asset_locked: false, status: "WORKING_COPY", funding: "hundredetusindvis af dollars", subject: "folder", file_formats: ["TIF"], payload_type: "ct scan", internal_status: "METADATA_RECEIVED", asset_taken_date: 0, restricted_access: ["USER"] })
    MERGE (e:Event { name: "CREATE_ASSET_METADATA", event: "CREATE_ASSET_METADATA", timestamp: 1683703806000})
    MERGE (e)-[:INITIATED_BY]->(u)
    MERGE (a)-[:CHANGED_BY]->(e)
    MERGE (a)-[:BELONGS_TO]->(i)
    MERGE (a)-[:IS_PART_OF]->(c)
    MERGE (e)-[:USED]->(w)
    MERGE (e)-[:USED]->(p)
$$) as (name agtype);

SELECT * from cypher('dassco', $$
    MATCH (u:User { name: "service-user"})
    MATCH(i:Institution {name: "institution_3"})
    MATCH(c:Collection {name: "i3_c1"})
    MATCH(w:Workstation {name: "i3_w1"})
    MATCH(p:Pipeline {name: "i3_p1"})
    MERGE (a:Asset { asset_pid: "asset-2", asset_guid: "asset-2", name: "asset-2", tags:{}, asset_locked: false, status: "WORKING_COPY", funding: "hundredetusindvis af dollars", subject: "folder", file_formats: ["TIF"], payload_type: "ct scan", internal_status: "METADATA_RECEIVED", asset_taken_date: 0, restricted_access: ["USER"] })
    MERGE (e:Event { name: "CREATE_ASSET_METADATA", event: "CREATE_ASSET_METADATA", timestamp: 1683703706000})
    MERGE (e)-[:INITIATED_BY]->(u)
    MERGE (a)-[:CHANGED_BY]->(e)
    MERGE (a)-[:BELONGS_TO]->(i)
    MERGE (a)-[:IS_PART_OF]->(c)
    MERGE (e)-[:USED]->(w)
    MERGE (e)-[:USED]->(p)
$$) as (name agtype);

SELECT * from cypher('dassco', $$
    MATCH (u:User { name: "service-user"})
    MATCH(i:Institution {name: "institution_4"})
    MATCH(c:Collection {name: "i4_c1"})
    MATCH(w:Workstation {name: "i4_w1"})
    MATCH(p:Pipeline {name: "i4_p1"})
    MERGE (a:Asset { asset_pid: "asset-3", asset_guid: "asset-3", name: "asset-3", tags:{}, asset_locked: false, status: "WORKING_COPY", funding: "hundredetusindvis af dollars", subject: "folder", file_formats: ["TIF"], payload_type: "ct scan", internal_status: "METADATA_RECEIVED", asset_taken_date: 0, restricted_access: ["USER"] })
    MERGE (e:Event { name: "CREATE_ASSET_METADATA", event: "CREATE_ASSET_METADATA", timestamp: 1683703606000})
    MERGE (e)-[:INITIATED_BY]->(u)
    MERGE (a)-[:CHANGED_BY]->(e)
    MERGE (a)-[:BELONGS_TO]->(i)
    MERGE (a)-[:IS_PART_OF]->(c)
    MERGE (e)-[:USED]->(w)
    MERGE (e)-[:USED]->(p)
$$) as (name agtype);

SELECT * from cypher('dassco', $$
    MATCH (u:User { name: "service-user"})
    MATCH(i:Institution {name: "institution_4"})
    MATCH(c:Collection {name: "i4_c1"})
    MATCH(w:Workstation {name: "i4_w1"})
    MATCH(p:Pipeline {name: "i4_p1"})
    MERGE (a:Asset { asset_pid: "asset-4", asset_guid: "asset-4", name: "asset-4", tags:{}, asset_locked: false, status: "WORKING_COPY", funding: "hundredetusindvis af dollars", subject: "folder", file_formats: ["TIF"], payload_type: "ct scan", internal_status: "METADATA_RECEIVED", asset_taken_date: 0, restricted_access: ["USER"] })
    MERGE (e:Event { name: "CREATE_ASSET_METADATA", event: "CREATE_ASSET_METADATA", timestamp: 1683703506000})
    MERGE (e)-[:INITIATED_BY]->(u)
    MERGE (a)-[:CHANGED_BY]->(e)
    MERGE (a)-[:BELONGS_TO]->(i)
    MERGE (a)-[:IS_PART_OF]->(c)
    MERGE (e)-[:USED]->(w)
    MERGE (e)-[:USED]->(p)
$$) as (name agtype);

SELECT * from cypher('dassco', $$
    MATCH (u:User { name: "service-user"})
    MATCH(i:Institution {name: "institution_5"})
    MATCH(c:Collection {name: "i5_c1"})
    MATCH(w:Workstation {name: "i5_w1"})
    MATCH(p:Pipeline {name: "i5_p1"})
    MERGE (a:Asset { asset_pid: "asset-5", asset_guid: "asset-5", name: "asset-5", tags:{}, asset_locked: false, status: "WORKING_COPY", funding: "hundredetusindvis af dollars", subject: "folder", file_formats: ["TIF"], payload_type: "ct scan", internal_status: "METADATA_RECEIVED", asset_taken_date: 0, restricted_access: ["USER"] })
    MERGE (e:Event { name: "CREATE_ASSET_METADATA", event: "CREATE_ASSET_METADATA", timestamp: 1683703406000})
    MERGE (e)-[:INITIATED_BY]->(u)
    MERGE (a)-[:CHANGED_BY]->(e)
    MERGE (a)-[:BELONGS_TO]->(i)
    MERGE (a)-[:IS_PART_OF]->(c)
    MERGE (e)-[:USED]->(w)
    MERGE (e)-[:USED]->(p)
$$) as (name agtype);

-- Add Asset Groups:
SELECT * from cypher('dassco', $$
    MATCH(a1:Asset {name: "asset-1"})
    MATCH(a2:Asset {name: "asset-2"})
    MATCH(a3:Asset {name: "asset-5"})
    MATCH(u:User {name: "role-1-user"})
    MERGE (ag:Asset_Group {name: "ag-1"})
    MERGE (ag)-[:CONTAINS]-(a1)
    MERGE (ag)-[:CONTAINS]-(a2)
    MERGE (ag)-[:CONTAINS]-(a3)
    MERGE (ag)-[:HAS_ACCESS]-(u)
    MERGE (ag)-[:MADE_BY]-(u)
$$) as (name agtype);

SELECT * from cypher('dassco', $$
    MATCH(a1:Asset {name: "asset-3"})
    MATCH(a2:Asset {name: "asset-4"})
    MATCH(a3:Asset {name: "asset-5"})
    MATCH(u:User {name: "role-2-user"})
    MERGE (ag:Asset_Group {name: "ag-2"})
    MERGE (ag)-[:CONTAINS]-(a1)
    MERGE (ag)-[:CONTAINS]-(a2)
    MERGE (ag)-[:CONTAINS]-(a3)
    MERGE (ag)-[:HAS_ACCESS]-(u)
    MERGE (ag)-[:MADE_BY]-(u)
$$) as (name agtype);

SELECT * from cypher('dassco', $$
    MATCH(a1:Asset {name: "asset-1"})
    MATCH(a2:Asset {name: "asset-3"})
    MATCH(a3:Asset {name: "asset-5"})
    MATCH(u:User {name: "service-user"})
    MERGE (ag:Asset_Group {name: "ag-3"})
    MERGE (ag)-[:CONTAINS]-(a1)
    MERGE (ag)-[:CONTAINS]-(a2)
    MERGE (ag)-[:CONTAINS]-(a3)
    MERGE (ag)-[:HAS_ACCESS]-(u)
    MERGE (ag)-[:MADE_BY]-(u)
$$) as (name agtype);