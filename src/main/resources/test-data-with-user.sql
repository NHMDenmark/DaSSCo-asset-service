CREATE EXTENSION IF NOT EXISTS age;
LOAD 'age';
SET search_path = ag_catalog, "$user", public;

select * from cypher('dassco', $$
    MERGE (i:Institution { name: "NNAD" })
	MERGE (w:Workstation { name: "ws-01", status: "IN_SERVICE" })
	MERGE (p:Pipeline { name: "pl-01" })
	MERGE (co:Collection { name: "NNAD Coll" })
	MERGE (s:Specimen { name: "specimen_1", barcode: "specimen_1" })
	MERGE (u:User { name: "moogie-woogie", user_id: "moogie-woogie" })
	MERGE (a:Asset { asset_pid: "pidtime-1234", asset_guid: "asset_1", name: "asset_1", tags:{}, asset_locked: "false", status: "WORKING_COPY", funding: "hundredetusindvis af dollars", subject: "folder", file_formats: ["TIF"], payload_type: "ct scan", internal_status: "METADATA_RECEIVED", asset_taken_date: 0, restricted_access: ["USER"] })
	MERGE (e:Event { name: "CREATE_ASSET_METADATA", event: "CREATE_ASSET_METADATA", timestamp: 1683702804000})
	MERGE (e)-[:INITIATED_BY]->(u)
	MERGE (w)-[:STATIONED_AT]->(i)
	MERGE (s)-[:USED_BY]->(a)
	MERGE (co)-[:USED_BY]->(i)
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
    MERGE (i:Institution { name: "WOOP" })
	MERGE (w:Workstation { name: "woop-01", status: "IN_SERVICE" })
	MERGE (p:Pipeline { name: "ploop-01" })
	MERGE (co:Collection { name: "WOOP_coll" })
	MERGE (s:Specimen { name: "specimen_1", barcode: "specimen_1" })
	MERGE (u:User { name: "moogie-woogie", user_id: "moogie-woogie" })
	MERGE (a:Asset { asset_pid: "pidtime-1234", asset_guid: "asset_2", name: "asset_2", tags:{}, asset_locked: "false", status: "WORKING_COPY", funding: "hundredetusindvis af dollars", subject: "folder", file_formats: ["TIF"], payload_type: "ct scan", internal_status: "METADATA_RECEIVED", asset_taken_date: 0, restricted_access: ["USER"] })
	MERGE (e:Event { name: "CREATE_ASSET_METADATA", event: "CREATE_ASSET_METADATA", timestamp: 1683702804000})
	MERGE (e)-[:INITIATED_BY]->(u)
	MERGE (w)-[:STATIONED_AT]->(i)
    MERGE (i)-[:RESTRICTED_TO]->(r:Role {name: "WOOP_USER"})
	MERGE (s)-[:USED_BY]->(a)
	MERGE (co)-[:USED_BY]->(i)
    MERGE (co)-[:RESTRICTED_TO]->(cr:Role {name: "WOOP_coll_USER"})
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
    MERGE (i:Institution { name: "WOOP" })
	MERGE (w:Workstation { name: "woop-01", status: "IN_SERVICE" })
	MERGE (p:Pipeline { name: "ploop-01" })
	MERGE (co:Collection { name: "WOOP_coll_open" })
	MERGE (s:Specimen { name: "specimen_1", barcode: "specimen_1" })
	MERGE (u:User { name: "moogie-woogie", user_id: "moogie-woogie" })
	MERGE (a:Asset { asset_pid: "pidtime-1234", asset_guid: "asset_5", name: "asset_5", tags:{}, asset_locked: "false", status: "WORKING_COPY", funding: "hundredetusindvis af dollars", subject: "folder", file_formats: ["TIF"], payload_type: "ct scan", internal_status: "METADATA_RECEIVED", asset_taken_date: 0, restricted_access: ["USER"] })
	MERGE (e:Event { name: "CREATE_ASSET_METADATA", event: "CREATE_ASSET_METADATA", timestamp: 1683702804000})
	MERGE (e)-[:INITIATED_BY]->(u)
	MERGE (w)-[:STATIONED_AT]->(i)
    MERGE (i)-[:RESTRICTED_TO]->(r:Role {name: "WOOP_USER"})
	MERGE (s)-[:USED_BY]->(a)
	MERGE (co)-[:USED_BY]->(i)
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
    MERGE (i:Institution { name: "WOOP" })
	MERGE (w:Workstation { name: "woop-01", status: "IN_SERVICE" })
	MERGE (p:Pipeline { name: "ploop-01" })
	MERGE (co:Collection { name: "WOOP_coll_closed" })
	MERGE (s:Specimen { name: "specimen_1", barcode: "specimen_1" })
	MERGE (u:User { name: "moogie-woogie", user_id: "moogie-woogie" })
	MERGE (a:Asset { asset_pid: "pidtime-1234", asset_guid: "asset_3", name: "asset_3", tags:{}, asset_locked: "false", status: "WORKING_COPY", funding: "hundredetusindvis af dollars", subject: "folder", file_formats: ["TIF"], payload_type: "ct scan", internal_status: "METADATA_RECEIVED", asset_taken_date: 0, restricted_access: ["USER"] })
	MERGE (e:Event { name: "CREATE_ASSET_METADATA", event: "CREATE_ASSET_METADATA", timestamp: 1683702804000})
	MERGE (e)-[:INITIATED_BY]->(u)
	MERGE (w)-[:STATIONED_AT]->(i)
    MERGE (i)-[:RESTRICTED_TO]->(r:Role {name: "WOOP_USER"})
	MERGE (s)-[:USED_BY]->(a)
	MERGE (co)-[:USED_BY]->(i)
    MERGE (co)-[:RESTRICTED_TO]->(cr:Role {name: "WOOP_coll_closed_USER"})
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
    MERGE (i:Institution { name: "CLOSED_INST" })
	MERGE (w:Workstation { name: "cls-01", status: "IN_SERVICE" })
	MERGE (p:Pipeline { name: "cls-01" })
	MERGE (co:Collection { name: "CLOSED_coll" })
	MERGE (s:Specimen { name: "specimen_1", barcode: "specimen_1" })
	MERGE (u:User { name: "moogie-woogie", user_id: "moogie-woogie" })
	MERGE (a:Asset { asset_pid: "pidtime-1234", asset_guid: "asset_4", name: "asset_4", tags:{}, asset_locked: "false", status: "WORKING_COPY", funding: "hundredetusindvis af dollars", subject: "folder", file_formats: ["TIF"], payload_type: "ct scan", internal_status: "METADATA_RECEIVED", asset_taken_date: 0, restricted_access: ["USER"] })
	MERGE (e:Event { name: "CREATE_ASSET_METADATA", event: "CREATE_ASSET_METADATA", timestamp: 1683702804000})
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
