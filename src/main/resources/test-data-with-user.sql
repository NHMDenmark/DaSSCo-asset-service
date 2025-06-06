CREATE EXTENSION IF NOT EXISTS age;
LOAD 'age';
SET search_path = ag_catalog, "$user", public;

SELECT * FROM ag_catalog.cypher('dassco', $$
    MERGE (i:Institution {name: "NNAD"})
$$) as (ag agtype);

SELECT * FROM ag_catalog.cypher('dassco', $$
    MATCH (i:Institution {name: "NNAD"})
    MERGE (c:Collection {name: "NNAD Coll"})
    MERGE (i)<-[:USED_BY]-(c)
$$) as (ag agtype);

SELECT * FROM ag_catalog.cypher('dassco', $$
    MATCH (i:Institution {name: "NNAD"})
    MERGE (w:Workstation {name: "ws-01", status: "IN_SERVICE"})
    MERGE (i)<-[:STATIONED_AT]-(w)
$$) as (ag agtype);

SELECT * FROM ag_catalog.cypher('dassco', $$
    MATCH (i:Institution {name: "NNAD"})
    MERGE (p:Pipeline {name: "pl-01"})
    MERGE (i)<-[:USED_BY]-(p)
$$) as (ag agtype);

select * from cypher('dassco', $$
    MATCH (i:Institution { name: "NNAD" })
	MATCH (co:Collection { name: "NNAD Coll" })
	MATCH (w:Workstation { name: "ws-01"})
	MATCH (p:Pipeline { name: "pl-01" })
	MERGE (a:Asset { asset_pid: "pidtime-1234", asset_guid: "asset_1", name: "asset_1", tags:{}, asset_locked: false, status: "WORKING_COPY", funding: "hundredetusindvis af dollars", subject: "folder", file_formats: ["TIF", "JPEG"], payload_type: "ct scan", internal_status: "METADATA_RECEIVED", asset_taken_date: 0, restricted_access: ["USER"] })
	MERGE (u:User { name: "moogie-woogie", user_id: "moogie-woogie" })
	MERGE (s:Specimen { name: "specimen_1", specimen_pid: "specimen_1", specimen_barcode: "specimen_1", preparation_type: "pinned_insect" })
	MERGE (e:Event { name: "CREATE_ASSET_METADATA", event: "CREATE_ASSET_METADATA", timestamp: 1683702804000})
	MERGE (e)-[:INITIATED_BY]->(u)
	MERGE (e)-[:USED]->(w)
	MERGE (e)-[:USED]->(p)
	MERGE (s)-[:USED_BY]->(a)
	MERGE (s)-[:BELONGS_TO]->(a)
	MERGE (s)-[:CREATED_BY]->(a)
	MERGE (a)-[:CHANGED_BY]->(e)
	MERGE (a)-[:BELONGS_TO]->(i)
	MERGE (a)-[:IS_PART_OF]->(co)
    MERGE (p)-[ub:USED_BY]->(i)
    MERGE (w)-[sa:STATIONED_AT]->(i)
$$) as (ag agtype);

SELECT * FROM ag_catalog.cypher('dassco', $$
    MERGE (i:Institution {name: "WOOP"})
$$) as (ag agtype);

SELECT * FROM ag_catalog.cypher('dassco', $$
    MATCH (i:Institution {name: "WOOP"})
    MERGE (c:Collection {name: "WOOP_coll"})
    MERGE (i)<-[:USED_BY]-(c)
$$) as (ag agtype);

SELECT * FROM ag_catalog.cypher('dassco', $$
    MATCH (i:Institution {name: "WOOP"})
    MERGE (c:Collection {name: "WOOP_coll_open"})
    MERGE (i)<-[:USED_BY]-(c)
$$) as (ag agtype);

SELECT * FROM ag_catalog.cypher('dassco', $$
    MATCH (i:Institution {name: "WOOP"})
    MERGE (c:Collection {name: "WOOP_coll_closed"})
    MERGE (i)<-[:USED_BY]-(c)
$$) as (ag agtype);

SELECT * FROM ag_catalog.cypher('dassco', $$
    MATCH (i:Institution {name: "WOOP"})
    MERGE (w:Workstation {name: "woop-01", status: "IN_SERVICE"})
    MERGE (i)<-[:STATIONED_AT]-(w)
$$) as (ag agtype);

SELECT * FROM ag_catalog.cypher('dassco', $$
    MATCH (i:Institution {name: "WOOP"})
    MERGE (p:Pipeline {name: "ploop-01"})
    MERGE (i)<-[:USED_BY]-(p)
$$) as (ag agtype);

select * from cypher('dassco', $$
    MATCH (i:Institution { name: "WOOP" })
	MATCH (co:Collection { name: "WOOP_coll" })
	MATCH (w:Workstation { name: "woop-01"})
	MATCH (p:Pipeline { name: "ploop-01" })
	MERGE (s:Specimen { name: "specimen_2", specimen_pid: "specimen_2", specimen_barcode: "specimen_2", preparation_type: "pinned_insect" })
	MERGE (u:User { name: "moogie-woogie", user_id: "moogie-woogie" })
	MERGE (a:Asset { asset_pid: "pidtime-2", asset_guid: "asset_2", name: "asset_2", tags:{}, asset_locked: false, status: "WORKING_COPY", funding: "hundredetusindvis af dollars", subject: "folder", file_formats: ["TIF"], payload_type: "ct scan", internal_status: "METADATA_RECEIVED", asset_taken_date: 0, restricted_access: ["USER"] })
	MERGE (e:Event { name: "CREATE_ASSET_METADATA", event: "CREATE_ASSET_METADATA", timestamp: 1683702804001})
    MERGE (i)-[:RESTRICTED_TO]->(r:Role {name: "WOOP_USER"})
	MERGE (e)-[:INITIATED_BY]->(u)
	MERGE (e)-[:USED]->(w)
	MERGE (e)-[:USED]->(p)
	MERGE (s)-[:USED_BY]->(a)
	MERGE (s)-[:BELONGS_TO]->(a)
    MERGE (co)-[:RESTRICTED_TO]->(cr:Role {name: "WOOP_coll_USER"})
	MERGE (s)-[:CREATED_BY]->(a)
	MERGE (a)-[:CHANGED_BY]->(e)
	MERGE (a)-[:BELONGS_TO]->(i)
	MERGE (a)-[:IS_PART_OF]->(co)
    MERGE (p)-[ub:USED_BY]->(i)
    MERGE (w)-[sa:STATIONED_AT]->(i)
$$) as (ag agtype);

select * from cypher('dassco', $$
    MATCH (i:Institution { name: "WOOP" })
	MATCH (w:Workstation { name: "woop-01"})
	MATCH (p:Pipeline { name: "ploop-01" })
	MATCH (co:Collection { name: "WOOP_coll_closed" })
	MATCH (s:Specimen { specimen_pid: "specimen_2"})
	MERGE (u:User { name: "moogie-woogie", user_id: "moogie-woogie" })
	MERGE (a:Asset { asset_pid: "pidtime-4", asset_guid: "asset_3", name: "asset_3", tags:{}, asset_locked: false, status: "WORKING_COPY", funding: "hundredetusindvis af dollars", subject: "folder", file_formats: ["TIF"], payload_type: "ct scan", internal_status: "METADATA_RECEIVED", asset_taken_date: 0, restricted_access: ["USER"] })
	MERGE (e:Event { name: "CREATE_ASSET_METADATA", event: "CREATE_ASSET_METADATA", timestamp: 1683702804003})
    MERGE (i)-[:RESTRICTED_TO]->(r:Role {name: "WOOP_USER"})
	MERGE (e)-[:INITIATED_BY]->(u)
	MERGE (e)-[:USED]->(w)
	MERGE (e)-[:USED]->(p)
	MERGE (s)-[:USED_BY]->(a)
    MERGE (co)-[:RESTRICTED_TO]->(cr:Role {name: "WOOP_coll_closed_USER"})
	MERGE (s)-[:BELONGS_TO]->(a)
	MERGE (s)-[:CREATED_BY]->(a)
	MERGE (a)-[:CHANGED_BY]->(e)
	MERGE (a)-[:BELONGS_TO]->(i)
	MERGE (a)-[:IS_PART_OF]->(co)
    MERGE (p)-[ub:USED_BY]->(i)
    MERGE (w)-[sa:STATIONED_AT]->(i)
$$) as (ag agtype);

SELECT * FROM ag_catalog.cypher('dassco', $$
    MATCH (p:Asset {name: "asset_2"})
    MATCH (c:Asset {name: "asset_3"})
    MERGE (c)-[cf:CHILD_OF]->(p)
$$) as (ag agtype);

select * from cypher('dassco', $$
    MATCH (i:Institution { name: "WOOP" })
	MATCH (co:Collection { name: "WOOP_coll_open" })
	MATCH (w:Workstation { name: "woop-01"})
	MATCH (p:Pipeline { name: "ploop-01" })
	MATCH (s:Specimen { specimen_pid: "specimen_2"})
	MERGE (u:User { name: "moogie-woogie", user_id: "moogie-woogie" })
	MERGE (a:Asset { asset_pid: "pidtime-3", asset_guid: "asset_5", name: "asset_5", tags:{}, asset_locked: false, status: "WORKING_COPY", funding: "hundredetusindvis af dollars", subject: "folder", file_formats: ["TIF"], payload_type: "ct scan", internal_status: "METADATA_RECEIVED", asset_taken_date: 0, restricted_access: ["USER"] })
	MERGE (e:Event { name: "CREATE_ASSET_METADATA", event: "CREATE_ASSET_METADATA", timestamp: 1683702804002})
	MERGE (e)-[:INITIATED_BY]->(u)
	MERGE (e)-[:USED]->(w)
    MERGE (i)-[:RESTRICTED_TO]->(r:Role {name: "WOOP_USER"})
	MERGE (e)-[:USED]->(p)
	MERGE (s)-[:USED_BY]->(a)
	MERGE (s)-[:BELONGS_TO]->(a)
	MERGE (s)-[:CREATED_BY]->(a)
	MERGE (a)-[:CHANGED_BY]->(e)
	MERGE (a)-[:BELONGS_TO]->(i)
	MERGE (a)-[:IS_PART_OF]->(co)
    MERGE (p)-[ub:USED_BY]->(i)
    MERGE (w)-[sa:STATIONED_AT]->(i)
$$) as (ag agtype);

SELECT * FROM ag_catalog.cypher('dassco', $$
    MATCH (p:Asset {name: "asset_3"})
    MATCH (c:Asset {name: "asset_5"})
    MERGE (c)-[cf:CHILD_OF]->(p)
$$) as (ag agtype);

SELECT * FROM ag_catalog.cypher('dassco', $$
    MERGE (i:Institution {name: "CLOSED_INST"})
$$) as (ag agtype);

SELECT * FROM ag_catalog.cypher('dassco', $$
    MATCH (i:Institution {name: "CLOSED_INST"})
    MERGE (c:Collection {name: "CLOSED_coll"})
    MERGE (i)<-[:USED_BY]-(c)
$$) as (ag agtype);

SELECT * FROM ag_catalog.cypher('dassco', $$
    MATCH (i:Institution {name: "CLOSED_INST"})
    MERGE (w:Workstation {name: "cls-ws-01", status: "IN_SERVICE"})
    MERGE (i)<-[:STATIONED_AT]-(w)
$$) as (ag agtype);

SELECT * FROM ag_catalog.cypher('dassco', $$
    MATCH (i:Institution {name: "CLOSED_INST"})
    MERGE (p:Pipeline {name: "cls-01"})
    MERGE (i)<-[:USED_BY]-(p)
$$) as (ag agtype);

select * from cypher('dassco', $$
    MATCH (i:Institution { name: "CLOSED_INST" })
	MATCH (co:Collection { name: "CLOSED_coll" })
	MATCH (w:Workstation { name: "cls-ws-01"})
	MATCH (p:Pipeline { name: "cls-01" })
	MERGE (s:Specimen { name: "specimen_3", specimen_pid: "specimen_3", specimen_barcode: "specimen_3", preparation_type: "pinned_insect" })
	MERGE (u:User { name: "moogie-woogie", user_id: "moogie-woogie" })
	MERGE (a:Asset { asset_pid: "pidtime-5", asset_guid: "asset_4", name: "asset_4", tags:{}, asset_locked: false, status: "WORKING_COPY", funding: "hundredetusindvis af dollars", subject: "folder", file_formats: ["TIF"], payload_type: "ct scan", internal_status: "METADATA_RECEIVED", asset_taken_date: 0, restricted_access: ["USER"] })
	MERGE (e:Event { name: "CREATE_ASSET_METADATA", event: "CREATE_ASSET_METADATA", timestamp: 1683702804004})
	MERGE (e)-[:INITIATED_BY]->(u)
	MERGE (w)-[:STATIONED_AT]->(i)
    MERGE (i)-[:RESTRICTED_TO]->(r:Role {name: "CLOSED_INST_USER"})
	MERGE (s)-[:USED_BY]->(a)
	MERGE (co)-[:USED_BY]->(i)
    MERGE (co)-[:RESTRICTED_TO]->(cr:Role {name: "CLOSED_coll_USER"})
	MERGE (p)-[:USED_BY]->(i)
	MERGE (s)-[:CREATED_BY]->(a)
	MERGE (a)-[:CHANGED_BY]->(e)
	MERGE (s)-[:BELONGS_TO]->(a)
	MERGE (a)-[:BELONGS_TO]->(i)
	MERGE (a)-[:IS_PART_OF]->(co)
	MERGE (e)-[:USED]->(w)
	MERGE (e)-[:USED]->(p)
	RETURN i
$$) as (institute agtype);

select * from cypher('dassco', $$
	MERGE (u:User { name: "moogie-auditor", user_id: "moogie-auditor" })
	RETURN u
$$) as (u agtype);
