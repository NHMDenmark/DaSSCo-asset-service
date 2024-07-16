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
	MERGE (a:Asset { pid: "pidtime-1234", guid: "asset_1", name: "asset_1", tags:{}, status: "WORKING_COPY", funding: "hundredetusindvis af dollars", subject: "folder", file_formats: ["TIF"], payload_type: "ct scan", internal_status: "METADATA_RECEIVED", asset_taken_date: 0, restricted_access: ["USER"] })
	MERGE (e:Event { name: "CREATE_ASSET", event: "CREATE_ASSET", timestamp: 1683702804000})
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
	MERGE (u:User { name: "moogie-auditor", user_id: "moogie-auditor" })
	RETURN u
$$) as (u agtype);
