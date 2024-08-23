-- Actual data with Roles and multiple assets with differing information
CREATE EXTENSION IF NOT EXISTS age;
LOAD 'age';
SET search_path = ag_catalog, "$user", public;

--Institutions
SELECT * FROM ag_catalog.cypher('dassco'
                  , $$
                            MERGE (i:Institution {name: "MWBGY"})
                            MERGE (i)-[:RESTRICTED_TO]->(r:Role {name: "MWGBY_USER"})
                            RETURN i
                        $$) as (name agtype);

SELECT * FROM ag_catalog.cypher('dassco'
                  , $$
                            MERGE (i:Institution {name: "ANTDNA"})
                            RETURN i
                        $$) as (name agtype);

--Collections
SELECT * FROM ag_catalog.cypher('dassco'
                  , $$
                            MERGE (i:Institution {name: "MWBGY"})
                            MERGE (c:Collection {name: "open_c"})
                            MERGE (i)<-[:USED_BY]-(c)
                            RETURN i.name, c.name
                        $$) as (institution_name agtype, collection_name agtype);

SELECT * FROM ag_catalog.cypher('dassco'
                  , $$
                            MERGE (i:Institution {name: "MWBGY"})
                            MERGE (c:Collection {name: "bugs_c"})
                            MERGE (i)<-[:USED_BY]-(c)
                            MERGE (c)-[:RESTRICTED_TO]->(r:Role {name: "bugs_c_USER"})
                            RETURN i.name, c.name
                        $$) as (institution_name agtype, collection_name agtype);

SELECT * FROM ag_catalog.cypher('dassco'
                  , $$
                            MERGE (i:Institution {name: "MWBGY"})
                            MERGE (c:Collection {name: "cups_c"})
                            MERGE (c)-[:RESTRICTED_TO]->(r:Role {name: "cups_c_USER"})
                            MERGE (i)<-[:USED_BY]-(c)
                            RETURN i.name, c.name
                        $$) as (institution_name agtype, collection_name agtype);

SELECT * FROM ag_catalog.cypher('dassco'
                  , $$
                            MERGE (i:Institution {name: "ANTDNA"})
                            MERGE (c:Collection {name: "cowdoy_c"})
                            MERGE (c)-[:RESTRICTED_TO]->(r:Role {name: "cowdoy_c_USER"})
                            MERGE (i)<-[:USED_BY]-(c)
                            RETURN i.name, c.name
                        $$) as (institution_name agtype, collection_name agtype);

SELECT * FROM ag_catalog.cypher('dassco'
                  , $$
                            MERGE (i:Institution {name: "ANTDNA"})
                            MERGE (c:Collection {name: "spooder_c"})
                            MERGE (i)<-[:USED_BY]-(c)
                            RETURN i.name, c.name
                        $$) as (institution_name agtype, collection_name agtype);

-- Workstations
SELECT * FROM ag_catalog.cypher('dassco'
                  , $$
                            MERGE (i:Institution {name: "MWBGY"})
                            MERGE (w:Workstation {name: "bugs_w", status: "IN_SERVICE"})
                            MERGE (i)<-[:STATIONED_AT]-(w)
                            RETURN i.name, w.name
                        $$) as (institution_name agtype, workstation_name agtype);

SELECT * FROM ag_catalog.cypher('dassco'
                  , $$
                            MERGE (i:Institution {name: "MWBGY"})
                            MERGE (w:Workstation {name: "cups_w", status: "IN_SERVICE"})
                            MERGE (i)<-[:STATIONED_AT]-(w)
                            RETURN i.name, w.name
                        $$) as (institution_name agtype, workstation_name agtype);

SELECT * FROM ag_catalog.cypher('dassco'
                  , $$
                            MERGE (i:Institution {name: "ANTDNA"})
                            MERGE (w:Workstation {name: "spooder_w", status: "OUT_OF_SERVICE"})
                            MERGE (i)<-[:STATIONED_AT]-(w)
                            RETURN i.name, w.name
                        $$) as (institution_name agtype, workstation_name agtype);

SELECT * FROM ag_catalog.cypher('dassco'
                  , $$
                            MERGE (i:Institution {name: "ANTDNA"})
                            MERGE (w:Workstation {name: "eggs_w", status: "IN_SERVICE"})
                            MERGE (i)<-[:STATIONED_AT]-(w)
                            RETURN i.name, w.name
                        $$) as (institution_name agtype, workstation_name agtype);
-- Pipeline
SELECT * FROM ag_catalog.cypher('dassco'
                  , $$
                            MATCH (i:Institution {name: "MWBGY"})
                            MERGE (p:Pipeline {name: "bugs_p"})
                            MERGE (i)<-[:USED_BY]-(p)
                            RETURN i.name, p.name
                        $$
                 ) as (institution_name agtype, pipeline_name agtype);

SELECT * FROM ag_catalog.cypher('dassco'
                  , $$
                            MATCH (i:Institution {name: "MWBGY"})
                            MERGE (p:Pipeline {name: "cups_p"})
                            MERGE (i)<-[:USED_BY]-(p)
                            RETURN i.name, p.name
                        $$
                  ) as (institution_name agtype, pipeline_name agtype);

SELECT * FROM ag_catalog.cypher('dassco'
                  , $$
                            MATCH (i:Institution {name: "ANTDNA"})
                            MERGE (p:Pipeline {name: "spooder_p"})
                            MERGE (i)<-[:USED_BY]-(p)
                            RETURN i.name, p.name
                        $$
                  ) as (institution_name agtype, pipeline_name agtype);

SELECT * FROM ag_catalog.cypher('dassco'
                  , $$
                            MATCH (i:Institution {name: "ANTDNA"})
                            MERGE (p:Pipeline {name: "eggs_p"})
                            MERGE (i)<-[:USED_BY]-(p)
                            RETURN i.name, p.name
                        $$
                  ) as (institution_name agtype, pipeline_name agtype);

-- Assets
-- for inst MWBGY
SELECT * FROM ag_catalog.cypher('dassco'
                  , $$
                        MATCH (i:Institution { name: "MWBGY" })
                        MATCH (w:Workstation { name: "bugs_w" })
                        MATCH (p:Pipeline { name: "bugs_p" })
                        MATCH (co:Collection { name: "bugs_c" })
                        MERGE (u:User { name: "moogie", user_id: "moogie" })

                        MERGE (s:Specimen { name: "specimen_1", specimen_barcode: "specimen_1", specimen_pid: "spec_pid_1", preparation_type: "pinning" })
                        MERGE (a:Asset { asset_pid: "5432-pid-10-pid-123", asset_guid: "mw_asset_1", name: "mw_asset_1",
                                tags:{mood: "cranky, a little gross, grosser still, yikes ew", type: "fly"}, asset_locked: "false",
                                status: "WORKING_COPY", funding: "20 dollaridoos", subject: "folder", file_formats: ["TIF"],
                                payload_type: "ct scan", internal_status: "METADATA_RECEIVED", asset_taken_date: 0, restricted_access: ["USER"],
                                synced: false })
                        MERGE (e:Event { name: "CREATE_ASSET_METADATA", event: "CREATE_ASSET_METADATA", timestamp: 1723191772000})
                        MERGE (e)-[:INITIATED_BY]->(u)
                        MERGE (s)-[:USED_BY]->(a)
                        MERGE (s)-[:CREATED_BY]->(a)
                        MERGE (a)-[:CHANGED_BY]->(e)
                        MERGE (s)-[:BELONGS_TO]->(a)
                        MERGE (a)-[:BELONGS_TO]->(i)
                        MERGE (a)-[:IS_PART_OF]->(co)
                        MERGE (e)-[:USED]->(w)
                        MERGE (e)-[:USED]->(p)
                        RETURN a.name, i.name
                    $$
                  ) as (asset_name agtype, institution_name agtype);

-- Completing the mw_asset_1 asset
SELECT * FROM ag_catalog.cypher('dassco', $$
                        MATCH (a:Asset {name: 'mw_asset_1'})
                        MATCH (w:Workstation {name: 'bugs_w'})
                        MATCH (p:Pipeline {name: 'bugs_p'})
                        MERGE (u:User {name: 'moogie', user_id: 'moogie'})
                        MERGE (a)-[:CHANGED_BY]->(e:Event {name: 'CREATE_ASSET', event: 'CREATE_ASSET', timestamp: 1723545652000})
                        MERGE (e)-[:INITIATED_BY]->(u)
                        MERGE (e)-[:USED]->(w)
                        MERGE (e)-[:USED]->(p)
                        SET a.internal_status = 'COMPLETED'
                        return a.asset_guid
                    $$) as (guid agtype);

-- Auditing the mw_asset_1 asset
SELECT * FROM ag_catalog.cypher('dassco', $$
                        MATCH (a:Asset {name: 'mw_asset_1'})
                        MERGE (u:User {name: 'moogie', user_id: 'moogie'})
                        MERGE (a)-[:CHANGED_BY]->(e:Event {name: 'AUDIT_ASSET', event: 'AUDIT_ASSET', timestamp: 1723549252000})
                        MERGE (e)-[:INITIATED_BY]->(u)
                        return a.asset_guid
                    $$) as (guid agtype);

SELECT * FROM ag_catalog.cypher('dassco'
                  , $$
                        MATCH (i:Institution { name: "MWBGY" })
                        MATCH (w:Workstation { name: "bugs_w" })
                        MATCH (p:Pipeline { name: "bugs_p" })
                        MATCH (co:Collection { name: "open_c" })
                        MERGE (u:User { name: "moogie", user_id: "moogie" })

                        MERGE (s:Specimen { name: "specimen_2", specimen_barcode: "specimen_2", specimen_pid: "spec_pid_2", preparation_type: "pinning" })
                        MERGE (a:Asset { asset_pid: "pid-30-pid-123", asset_guid: "mw_asset_2", name: "mw_asset_2",
                                tags:{mood: "upset, shiny", type: "fly"}, asset_locked: "false",
                                status: "WORKING_COPY", funding: "A BILLION dollaridoos", subject: "folder", file_formats: ["TIF", "DNG", "JPEG"],
                                payload_type: "surface scan", internal_status: "METADATA_RECEIVED", asset_taken_date: 0, restricted_access: ["USER"],
                                synced: false })

                        MERGE (e:Event { name: "CREATE_ASSET_METADATA", event: "CREATE_ASSET_METADATA", timestamp: 1723199212000})
                        MERGE (e)-[:INITIATED_BY]->(u)
                        MERGE (s)-[:USED_BY]->(a)
                        MERGE (s)-[:CREATED_BY]->(a)
                        MERGE (a)-[:CHANGED_BY]->(e)
                        MERGE (s)-[:BELONGS_TO]->(a)
                        MERGE (a)-[:BELONGS_TO]->(i)
                        MERGE (a)-[:IS_PART_OF]->(co)
                        MERGE (e)-[:USED]->(w)
                        MERGE (e)-[:USED]->(p)
                        RETURN a.name, i.name
                    $$
                  ) as (asset_name agtype, institution_name agtype);

SELECT * FROM ag_catalog.cypher('dassco'
                  , $$
                        MATCH (i:Institution { name: "MWBGY" })
                        MATCH (w:Workstation { name: "bugs_w" })
                        MATCH (p:Pipeline { name: "bugs_p" })
                        MATCH (co:Collection { name: "bugs_c" })
                        MERGE (u:User { name: "moogie", user_id: "moogie" })

                        MERGE (s:Specimen { name: "specimen_3", specimen_barcode: "specimen_3", specimen_pid: "spec_pid_3", preparation_type: "pinning" })
                        MERGE (a:Asset { asset_pid: "123-pid-15-pid-123", asset_guid: "mw_asset_3", name: "mw_asset_3",
                                tags:{mood: "unhappy, dead", type: "grasshopper"}, asset_locked: "false",
                                status: "WORKING_COPY", funding: "90 dollaridoos", subject: "folder", file_formats: ["DNG", "TXT"],
                                payload_type: "ct scan", internal_status: "METADATA_RECEIVED", asset_taken_date: 0, restricted_access: ["USER"],
                                synced: false })

                        MERGE (e:Event { name: "CREATE_ASSET_METADATA", event: "CREATE_ASSET_METADATA", timestamp: 1723199212000})
                        MERGE (e)-[:INITIATED_BY]->(u)
                        MERGE (s)-[:USED_BY]->(a)
                        MERGE (s)-[:CREATED_BY]->(a)
                        MERGE (a)-[:CHANGED_BY]->(e)
                        MERGE (s)-[:BELONGS_TO]->(a)
                        MERGE (a)-[:BELONGS_TO]->(i)
                        MERGE (a)-[:IS_PART_OF]->(co)
                        MERGE (e)-[:USED]->(w)
                        MERGE (e)-[:USED]->(p)
                        RETURN a.name, i.name
                    $$
                  ) as (asset_name agtype, institution_name agtype);

SELECT * FROM ag_catalog.cypher('dassco'
                  , $$
                        MATCH (i:Institution { name: "MWBGY" })
                        MATCH (w:Workstation { name: "cups_w" })
                        MATCH (p:Pipeline { name: "cups_p" })
                        MATCH (co:Collection { name: "cups_c" })
                        MERGE (u:User { name: "moogie", user_id: "moogie" })

                        MERGE (s:Specimen { name: "specimen_4", specimen_barcode: "specimen_4", specimen_pid: "spec_pid_4", preparation_type: "pinning" })
                        MERGE (ss:Specimen { name: "specimen_4_2", specimen_barcode: "specimen_4_2", specimen_pid: "spec_pid_4_2", preparation_type: "pinning" })
                        MERGE (a:Asset { asset_pid: "00-pid-9-pid-123", asset_guid: "mw_asset_4", name: "mw_asset_4",
                                tags:{type: "red cup", color: "red"}, asset_locked: "false",
                                status: "WORKING_COPY", funding: "1 dollaridoos", subject: "folder", file_formats: ["RAW", "RAF"],
                                payload_type: "image", internal_status: "METADATA_RECEIVED", asset_taken_date: 0, restricted_access: ["USER"],
                                synced: false })

                        MERGE (e:Event { name: "CREATE_ASSET_METADATA", event: "CREATE_ASSET_METADATA", timestamp: 1721301578634})
                        MERGE (e)-[:INITIATED_BY]->(u)
                        MERGE (s)-[:USED_BY]->(a)
                        MERGE (ss)-[:USED_BY]->(a)
                        MERGE (s)-[:CREATED_BY]->(a)
                        MERGE (ss)-[:CREATED_BY]->(a)
                        MERGE (a)-[:CHANGED_BY]->(e)
                        MERGE (s)-[:BELONGS_TO]->(a)
                        MERGE (ss)-[:BELONGS_TO]->(a)
                        MERGE (a)-[:BELONGS_TO]->(i)
                        MERGE (a)-[:IS_PART_OF]->(co)
                        MERGE (e)-[:USED]->(w)
                        MERGE (e)-[:USED]->(p)
                        RETURN a.name, i.name
                    $$
                  ) as (asset_name agtype, institution_name agtype);

SELECT * FROM ag_catalog.cypher('dassco'
                  , $$
                        MATCH (i:Institution { name: "MWBGY" })
                        MATCH (w:Workstation { name: "cups_w" })
                        MATCH (p:Pipeline { name: "cups_p" })
                        MATCH (co:Collection { name: "cups_c" })
                        MERGE (u:User { name: "moogie", user_id: "moogie" })

                        MERGE (s:Specimen { name: "specimen_5", specimen_barcode: "specimen_5", specimen_pid: "spec_pid_5", preparation_type: "pinning" })
                        MERGE (a:Asset { asset_pid: "9999-pid-1-pid-123", asset_guid: "mw_asset_5", name: "mw_asset_5",
                                tags:{type: "blue cup", color: "blue"}, asset_locked: "false",
                                status: "WORKING_COPY", funding: "484 dollaridoos", subject: "folder", file_formats: ["CR3", "TIF"],
                                payload_type: "ct scan", internal_status: "METADATA_RECEIVED", asset_taken_date: 0, restricted_access: ["USER"],
                                synced: false })

                        MERGE (e:Event { name: "CREATE_ASSET_METADATA", event: "CREATE_ASSET_METADATA", timestamp: 1721301578634})
                        MERGE (e)-[:INITIATED_BY]->(u)
                        MERGE (s)-[:USED_BY]->(a)
                        MERGE (s)-[:CREATED_BY]->(a)
                        MERGE (a)-[:CHANGED_BY]->(e)
                        MERGE (s)-[:BELONGS_TO]->(a)
                        MERGE (a)-[:BELONGS_TO]->(i)
                        MERGE (a)-[:IS_PART_OF]->(co)
                        MERGE (e)-[:USED]->(w)
                        MERGE (e)-[:USED]->(p)
                        RETURN a.name, i.name
                    $$
                  ) as (asset_name agtype, institution_name agtype);

SELECT * FROM ag_catalog.cypher('dassco'
                  , $$
                        MATCH (i:Institution { name: "MWBGY" })
                        MATCH (w:Workstation { name: "cups_w" })
                        MATCH (p:Pipeline { name: "cups_p" })
                        MATCH (co:Collection { name: "open_c" })
                        MERGE (u:User { name: "moogie", user_id: "moogie" })

                        MERGE (s:Specimen { name: "specimen_6", specimen_barcode: "specimen_6", specimen_pid: "spec_pid_6", preparation_type: "pinning" })
                        MERGE (a:Asset { asset_pid: "847-pid-13-pid-123", asset_guid: "mw_asset_6", name: "mw_asset_6",
                                tags:{mood: "open", type: "green cup", color: "green"}, asset_locked: "false",
                                status: "WORKING_COPY", funding: "88 dollaridoos", subject: "folder", file_formats: ["JPEG"],
                                payload_type: "ct scan", internal_status: "METADATA_RECEIVED", asset_taken_date: 0, restricted_access: ["USER"],
                                synced: false })

                        MERGE (e:Event { name: "CREATE_ASSET_METADATA", event: "CREATE_ASSET_METADATA", timestamp: 1721301612579})
                        MERGE (e)-[:INITIATED_BY]->(u)
                        MERGE (s)-[:USED_BY]->(a)
                        MERGE (s)-[:CREATED_BY]->(a)
                        MERGE (a)-[:CHANGED_BY]->(e)
                        MERGE (s)-[:BELONGS_TO]->(a)
                        MERGE (a)-[:BELONGS_TO]->(i)
                        MERGE (a)-[:IS_PART_OF]->(co)
                        MERGE (e)-[:USED]->(w)
                        MERGE (e)-[:USED]->(p)
                        RETURN a.name, i.name
                    $$
                  ) as (asset_name agtype, institution_name agtype);

-- for inst ANTDNA
SELECT * FROM ag_catalog.cypher('dassco'
                  , $$
                        MATCH (i:Institution { name: "ANTDNA" })
                        MATCH (w:Workstation { name: "spooder_w" })
                        MATCH (p:Pipeline { name: "spooder_p" })
                        MATCH (co:Collection { name: "cowdoy_c" })
                        MERGE (u:User { name: "moogie", user_id: "moogie" })

                        MERGE (s:Specimen { name: "specimen_7", specimen_barcode: "specimen_7", specimen_pid: "spec_pid_7", preparation_type: "pinning" })
                        MERGE (a:Asset { asset_pid: "00-pid-3-pid-123", asset_guid: "ad_asset_1", name: "ad_asset_1",
                                tags:{mood: "sleepy", type: "tarantula", color: "black"}, asset_locked: "false",
                                status: "WORKING_COPY", funding: "3434 dollaridoos", subject: "folder", file_formats: ["TIF", "CR3"],
                                payload_type: "ct scan", internal_status: "METADATA_RECEIVED", asset_taken_date: 0, restricted_access: ["USER"],
                                synced: false })

                        MERGE (e:Event { name: "CREATE_ASSET_METADATA", event: "CREATE_ASSET_METADATA", timestamp: 1721301664747})
                        MERGE (e)-[:INITIATED_BY]->(u)
                        MERGE (s)-[:USED_BY]->(a)
                        MERGE (s)-[:CREATED_BY]->(a)
                        MERGE (a)-[:CHANGED_BY]->(e)
                        MERGE (s)-[:BELONGS_TO]->(a)
                        MERGE (a)-[:BELONGS_TO]->(i)
                        MERGE (a)-[:IS_PART_OF]->(co)
                        MERGE (e)-[:USED]->(w)
                        MERGE (e)-[:USED]->(p)
                        RETURN a.name, i.name
                    $$
                  ) as (asset_name agtype, institution_name agtype);

SELECT * FROM ag_catalog.cypher('dassco'
                  , $$
                        MATCH (i:Institution { name: "ANTDNA" })
                        MATCH (w:Workstation { name: "spooder_w" })
                        MATCH (p:Pipeline { name: "spooder_p" })
                        MATCH (co:Collection { name: "spooder_c" })
                        MERGE (u:User { name: "moogie", user_id: "moogie" })

                        MERGE (s:Specimen { name: "specimen_8", specimen_barcode: "specimen_8", specimen_pid: "spec_pid_8", preparation_type: "pinning" })
                        MERGE (a:Asset { asset_pid: "12334-pid-0-pid-123", asset_guid: "ad_asset_2", name: "ad_asset_2",
                                tags:{mood: "happy", type: "house spider", color: "black"}, asset_locked: "false",
                                status: "WORKING_COPY", funding: "absolutely no dollaridoos", subject: "folder", file_formats: ["TIF", "JPEG"],
                                payload_type: "image", internal_status: "METADATA_RECEIVED", asset_taken_date: 0, restricted_access: ["USER"],
                                synced: false })

                        MERGE (e:Event { name: "CREATE_ASSET_METADATA", event: "CREATE_ASSET_METADATA", timestamp: 1723200052000})
                        MERGE (e)-[:INITIATED_BY]->(u)
                        MERGE (s)-[:USED_BY]->(a)
                        MERGE (s)-[:CREATED_BY]->(a)
                        MERGE (a)-[:CHANGED_BY]->(e)
                        MERGE (s)-[:BELONGS_TO]->(a)
                        MERGE (a)-[:BELONGS_TO]->(i)
                        MERGE (a)-[:IS_PART_OF]->(co)
                        MERGE (e)-[:USED]->(w)
                        MERGE (e)-[:USED]->(p)
                        RETURN a.name, i.name
                    $$
                  ) as (asset_name agtype, institution_name agtype);

SELECT * FROM ag_catalog.cypher('dassco'
                  , $$
                        MATCH (i:Institution { name: "ANTDNA" })
                        MATCH (w:Workstation { name: "eggs_w" })
                        MATCH (p:Pipeline { name: "eggs_p" })
                        MATCH (co:Collection { name: "cowdoy_c" })
                        MERGE (u:User { name: "moogie", user_id: "moogie" })

                        MERGE (s:Specimen { name: "specimen_9", specimen_barcode: "specimen_9", specimen_pid: "spec_pid_9", preparation_type: "pinning" })
                        MERGE (a:Asset { asset_pid: "3293-pid-32-pid-123", asset_guid: "ad_asset_3", name: "ad_asset_3",
                                tags:{mood: "eggy, shiny, small", type: "dragon"}, asset_locked: "false",
                                status: "WORKING_COPY", funding: "absolutely no dollaridoos", subject: "folder", file_formats: ["TIF", "TXT"],
                                payload_type: "surface scan", internal_status: "METADATA_RECEIVED", asset_taken_date: 0, restricted_access: ["USER"],
                                synced: false })

                        MERGE (e:Event { name: "CREATE_ASSET_METADATA", event: "CREATE_ASSET_METADATA", timestamp: 1721301728287})
                        MERGE (e)-[:INITIATED_BY]->(u)
                        MERGE (s)-[:USED_BY]->(a)
                        MERGE (s)-[:CREATED_BY]->(a)
                        MERGE (a)-[:CHANGED_BY]->(e)
                        MERGE (s)-[:BELONGS_TO]->(a)
                        MERGE (a)-[:BELONGS_TO]->(i)
                        MERGE (a)-[:IS_PART_OF]->(co)
                        MERGE (e)-[:USED]->(w)
                        MERGE (e)-[:USED]->(p)
                        RETURN a.name, i.name
                    $$
                  ) as (asset_name agtype, institution_name agtype);

SELECT * FROM ag_catalog.cypher('dassco'
                  , $$
                        MATCH (i:Institution { name: "ANTDNA" })
                        MATCH (w:Workstation { name: "eggs_w" })
                        MATCH (p:Pipeline { name: "eggs_p" })
                        MATCH (co:Collection { name: "cowdoy_c" })
                        MERGE (u:User { name: "moogie", user_id: "moogie" })

                        MERGE (s:Specimen { name: "specimen_10", specimen_barcode: "specimen_10", specimen_pid: "spec_pid_10", preparation_type: "pinning" })
                        MERGE (ss:Specimen { name: "specimen_11", specimen_barcode: "specimen_11", specimen_pid: "spec_pid_11", preparation_type: "pinning" })
                        MERGE (a:Asset { asset_pid: "93-pid-0-pid-123", asset_guid: "ad_asset_4", name: "ad_asset_4",
                                tags:{mood: "sleepy", type: "goose"}, asset_locked: "false",
                                status: "WORKING_COPY", funding: "34 dollaridoos", subject: "folder", file_formats: ["TIF"],
                                payload_type: "ct scan", internal_status: "METADATA_RECEIVED", asset_taken_date: 0, restricted_access: ["USER"],
                                synced: false })

                        MERGE (e:Event { name: "CREATE_ASSET_METADATA", event: "CREATE_ASSET_METADATA", timestamp: 1723718452000})
                        MERGE (e)-[:INITIATED_BY]->(u)
                        MERGE (s)-[:USED_BY]->(a)
                        MERGE (ss)-[:USED_BY]->(a)
                        MERGE (s)-[:CREATED_BY]->(a)
                        MERGE (ss)-[:CREATED_BY]->(a)
                        MERGE (a)-[:CHANGED_BY]->(e)
                        MERGE (s)-[:BELONGS_TO]->(a)
                        MERGE (ss)-[:BELONGS_TO]->(a)
                        MERGE (a)-[:BELONGS_TO]->(i)
                        MERGE (a)-[:IS_PART_OF]->(co)
                        MERGE (e)-[:USED]->(w)
                        MERGE (e)-[:USED]->(p)
                        RETURN a.name, i.name
                    $$
                  ) as (asset_name agtype, institution_name agtype);