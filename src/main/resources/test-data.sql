CREATE EXTENSION IF NOT EXISTS age;
LOAD 'age';
SET search_path = ag_catalog, "$user", public;

SELECT create_graph('dassco');

-- Nodes
SELECT create_vlabel('dassco','Institute');
SELECT create_vlabel('dassco','Event');
SELECT create_vlabel('dassco','User');
SELECT create_vlabel('dassco','Pipeline');
SELECT create_vlabel('dassco','Workstation');
SELECT create_vlabel('dassco','Collection');
SELECT create_vlabel('dassco','Asset');
SELECT create_vlabel('dassco','Specimen');


-- Relations
SELECT create_elabel('dassco','INITIATED_BY');  --# Event -> User
SELECT create_elabel('dassco','STATIONED_AT');	--# Workstation -> Institution
SELECT create_elabel('dassco','USED_BY');     	--# Specimen -> Asset, Collection -> Institution, Pipeline -> Institution
SELECT create_elabel('dassco','CREATED_BY');    --# Specimen -> Asset
SELECT create_elabel('dassco','CHANGED_BY');    --# Asset -> Event
SELECT create_elabel('dassco','CHILD_OF');		--# Asset -> Asset
SELECT create_elabel('dassco','BELONGS_TO');    --# Specimen -> Asset, Asset -> Institution, Specify_User -> Institution
SELECT create_elabel('dassco','IS_PART_OF');	--# Asset -> Collection
SELECT create_elabel('dassco','USED');			--# Event -> Workstation, Event -> Pipeline

select * from cypher('dassco', $$
    MERGE (i:Institution { name: "NNAD" })
	MERGE (w:Workstation { name: "ws-01", status: "IN_SERVICE" })
	MERGE (p:Pipeline { name: "pl-01" })
	MERGE (co:Collection { name: "NNAD Coll" })
	MERGE (s:Specimen { name: "specimen_1", barcode: "specimen_1" })
	MERGE (u:User { name: "test-user", user_id: "test-user" })
	MERGE (a:Asset { pid: "asdf-12346-3333-100a1", guid: "asset_1", name: "asset_1", tags:{}, status: "WORKING_COPY", funding: "hundredetusindvis af dollars", subject: "folder", file_formats: ["TIF"], payload_type: "ct scan", internal_status: "METADATA_RECEIVED", asset_taken_date: 0, restricted_access: ["USER"] })
	MERGE (e:Event { name: "CREATE_ASSET", event: "CREATE_ASSET", timestamp: 1683702804000}) -- 10/5-2023
	MERGE (e)-[:INITIATED_BY]->(u)
	MERGE (w)-[:STATIONED_AT]->(i)
	MERGE (s)-[:USED_BY]->(a)
	MERGE (co)-[:USED_BY]->(i)
	MERGE (p)-[:USED_BY]->(i)
	MERGE (s)-[:CREATED_BY]->(a)
	MERGE (a)-[:CHANGED_BY]->(e)
	MERGE (a)-[:CHILD_OF]->(a)
	MERGE (s)-[:BELONGS_TO]->(a)
	MERGE (a)-[:BELONGS_TO]->(i)
	MERGE (a)-[:IS_PART_OF]->(co)
	MERGE (e)-[:USED]->(w)
	MERGE (e)-[:USED]->(p)
	RETURN i
$$) as (institute agtype);
