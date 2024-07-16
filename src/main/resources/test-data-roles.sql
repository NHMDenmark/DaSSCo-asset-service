CREATE EXTENSION IF NOT EXISTS age;
LOAD 'age';
SET search_path = ag_catalog, "$user", public;

SELECT * FROM cypher('dassco', $$
    MERGE (i:Institution { name: "role-institution-1"})
    MERGE (c:Collection {name: "role-collection-1"})
    MERGE(p:Pipeline {name: "ri1_p1"})
    MERGE (w:Workstation {name: "ri1_w1", status: "IN_SERVICE"})
    MERGE (u:User { name: "test-role-user", user_id: "test-user" })
    MERGE (a:Asset { asset_pid: "test-role-asset-1", asset_guid: "test-role-asset-1", name: "test-role-asset-1", tags:{}, asset_locked: false, status: "WORKING_COPY", funding: "hundredetusindvis af dollars", subject: "folder", file_formats: ["TIF"], payload_type: "ct scan", internal_status: "METADATA_RECEIVED", asset_taken_date: 0, restricted_access: ["USER"] })
    MERGE (e:Event { name: "CREATE_ASSET_METADATA", event: "CREATE_ASSET_METADATA", timestamp: 1683702805000})
    MERGE (r:Role {name: "test-role-1"})
    MERGE (e)-[:INITIATED_BY]->(u)
    MERGE (i)-[:RESTRICTED_TO]->(r)
    MERGE (w)-[:STATIONED_AT]->(i)
    MERGE (i)<-[:USED_BY]-(c)
    MERGE (p)-[:USED_BY]->(i)
    MERGE (a)-[:CHANGED_BY]->(e)
    MERGE (a)-[:BELONGS_TO]->(i)
    MERGE (a)-[:IS_PART_OF]->(c)
    MERGE (e)-[:USED]->(w)
	MERGE (e)-[:USED]->(p)
    RETURN i
$$) as (name agtype);

SELECT * FROM cypher('dassco', $$
    MERGE (i:Institution { name: "role-institution-2"})
    MERGE (c:Collection {name: "role-collection-2"})
    MERGE(p:Pipeline {name: "ri2_p1"})
    MERGE (w:Workstation {name: "ri2_w1", status: "IN_SERVICE"})
    MERGE (u:User { name: "test-role-user", user_id: "test-user" })
    MERGE (a:Asset { asset_pid: "test-role-asset-2", asset_guid: "test-role-asset-2", name: "test-role-asset-2", tags:{}, asset_locked: false, status: "WORKING_COPY", funding: "hundredetusindvis af dollars", subject: "folder", file_formats: ["TIF"], payload_type: "ct scan", internal_status: "METADATA_RECEIVED", asset_taken_date: 0, restricted_access: ["USER"] })
    MERGE (e:Event { name: "CREATE_ASSET_METADATA", event: "CREATE_ASSET_METADATA", timestamp: 1683702806000})
    MERGE (r:Role {name: "test-role-2"})
    MERGE (e)-[:INITIATED_BY]->(u)
    MERGE (c)-[:RESTRICTED_TO]->(r)
    MERGE (w)-[:STATIONED_AT]->(i)
    MERGE (i)<-[:USED_BY]-(c)
    MERGE (p)-[:USED_BY]->(i)
    MERGE (a)-[:CHANGED_BY]->(e)
    MERGE (a)-[:BELONGS_TO]->(i)
    MERGE (a)-[:IS_PART_OF]->(c)
    MERGE (e)-[:USED]->(w)
	MERGE (e)-[:USED]->(p)
    RETURN i
$$) as (name agtype);

SELECT * FROM cypher('dassco', $$
    MERGE (i:Institution { name: "role-institution-3"})
    MERGE (c:Collection {name: "role-collection-3"})
    MERGE(p:Pipeline {name: "ri3_p1"})
    MERGE (w:Workstation {name: "ri3_w1", status: "IN_SERVICE"})
    MERGE (u:User { name: "test-role-user", user_id: "test-user" })
    MERGE (a:Asset { asset_pid: "test-role-asset-3", asset_guid: "test-role-asset-3", name: "test-role-asset-3", tags:{}, asset_locked: false, status: "WORKING_COPY", funding: "hundredetusindvis af dollars", subject: "folder", file_formats: ["TIF"], payload_type: "ct scan", internal_status: "METADATA_RECEIVED", asset_taken_date: 0, restricted_access: ["USER"] })
    MERGE (e:Event { name: "CREATE_ASSET_METADATA", event: "CREATE_ASSET_METADATA", timestamp: 1683702809000})
    MERGE (e)-[:INITIATED_BY]->(u)
    MERGE (w)-[:STATIONED_AT]->(i)
    MERGE (i)<-[:USED_BY]-(c)
    MERGE (p)-[:USED_BY]->(i)
    MERGE (a)-[:CHANGED_BY]->(e)
    MERGE (a)-[:BELONGS_TO]->(i)
    MERGE (a)-[:IS_PART_OF]->(c)
    MERGE (e)-[:USED]->(w)
	MERGE (e)-[:USED]->(p)
    RETURN i
$$) as (name agtype);

SELECT * FROM cypher('dassco', $$
    MERGE (ag:Asset_Group {name: "ag1"})
$$) as (name agtype);

SELECT * FROM cypher('dassco', $$
    MERGE (ag:Asset_Group {name: "ag2"})
$$) as (name agtype);

SELECT * FROM cypher('dassco', $$
    MERGE (ag:Asset_Group {name: "ag3"})
$$) as (name agtype);

SELECT * FROM ag_catalog.cypher('dassco', $$
            MATCH (ag:Asset_Group{name:"ag1"})
            MATCH (a:Asset)
            WHERE a.asset_guid IN ['test-role-asset-1', 'test-role-asset-3']
            MERGE (ag)-[:CONTAINS]->(a)
$$) as (a agtype);

SELECT * FROM ag_catalog.cypher('dassco', $$
    MATCH (ag:Asset_Group{name:"ag2"})
    MATCH (a:Asset)
    WHERE a.asset_guid IN ['test-role-asset-2', 'test-role-asset-3']
    MERGE (ag)-[:CONTAINS]->(a)
$$) as (a agtype);

SELECT * FROM ag_catalog.cypher('dassco', $$
    MATCH (ag:Asset_Group{name:"ag3"})
    MATCH (a:Asset)
    WHERE a.asset_guid IN ['test-role-asset-1', 'test-role-asset-2', 'test-role-asset-3']
    MERGE (ag)-[:CONTAINS]->(a)
$$) as (a agtype)

