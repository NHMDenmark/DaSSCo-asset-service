CREATE EXTENSION IF NOT EXISTS age;
LOAD 'age';
SET search_path = ag_catalog, "$user", public;

SELECT create_graph('dassco');

-- Nodes
SELECT create_vlabel('dassco','Institute');
SELECT create_vlabel('dassco','Digitiser');
SELECT create_vlabel('dassco','CopyrightOwner');
SELECT create_vlabel('dassco','Pipeline');
SELECT create_vlabel('dassco','Workstation');
SELECT create_vlabel('dassco','Collection');
SELECT create_vlabel('dassco','Equipment');
SELECT create_vlabel('dassco','Asset');
SELECT create_vlabel('dassco','Media');

-- Relations
SELECT create_elabel('dassco','PIPELINE_USED_BY');      --# Pipeline -> Digitiser
SELECT create_elabel('dassco','STATIONED_AT');          --# Workstation -> Institute
SELECT create_elabel('dassco','EMPLOYED_BY');           --# Digitiser -> Institute
SELECT create_elabel('dassco','USED_BY');               --# Workstation -> Digitiser
SELECT create_elabel('dassco','MEDIA_CREATED_BY');      --# Media -> Digitiser
SELECT create_elabel('dassco','MEDIA_UPDATED_BY');      --# Media -> Digitiser
SELECT create_elabel('dassco','MEDIA_DELETED_BY');      --# Asset -> Digitiser
SELECT create_elabel('dassco','METADATA_CREATED_BY');   --# Asset -> Digitiser
SELECT create_elabel('dassco','METADATA_UPDATED_BY');   --# Asset -> Digitiser
SELECT create_elabel('dassco','ASSET_AUDITED_BY');      --# Asset -> Digitiser
SELECT create_elabel('dassco','PART_OF_COLLECTION');    --# Asset -> Collection
SELECT create_elabel('dassco','HAS_MEDIA');             --# Asset -> Media
SELECT create_elabel('dassco','PROPERTY_OF');           --# Asset -> Institute
SELECT create_elabel('dassco','CHILD_OF');              --# Asset -> Asset ?
SELECT create_elabel('dassco','ORIGINALLY_CHILD_OF');   --# Asset -> Asset ?
SELECT create_elabel('dassco','CREATED_ON');            --# Media -> Workstation
SELECT create_elabel('dassco','OWNED_BY');              --# Media -> CopyrightOwner
SELECT create_elabel('dassco','EQUIPTMENT_USED');       --# Media -> Equiptment
SELECT create_elabel('dassco','RELATED_TO');            --# Media -> Media


select * from cypher('dassco', $$
    MERGE (i:Institute { name: "NNAD", ocr_text: "FLORA DANICA EXSICCATA Lycopodium selago L. Jyll Silkeborg Vesterskov YII 1904 leg. M. Lorenzen.", geographic_region: "", taxon_name: "" })
   MERGE (d:Digitiser { name: "Bret Livingstone" })
   MERGE (co:CopyrightOwner { name: "NNAD" })
   MERGE (p:Pipelines {name: "PL_TR201" })
   MERGE (w:WorkStation {name: "WS_B0004" })
   MERGE (c:Collection { name: "Ichthyology" })
   MERGE (e:Equiptment { name: "Equipment", equipment_details: "[]", exposure_time: "", f_number: "", focal_length: "", iso_setting: "", white_balance: "" })
   MERGE (a:Asset { name: "Asset", notes: "[]", barcode: "", specimen_pid: "", specify_specimen_id: "005c6c96-8d54-492d-88c4-d6f26414cb16", specify_attachment_id: "c5d65adb-8228-4e39-b6f3-ef880df8346e", multi_specimen_status: "no", other_multi_specimen: "", external_link: "", access_level: "", type_status: "", specimen_storage_location: "", funding: "", embargo_type: "", embargo_notes: "[]", original_specify_media_name: "https://specify-attachments.science.ku.dk/fileget?coll=NHMD+Vascular+Plants&type=O&filename=sp68923230029256349442.att.jpg&downloadname=NHMD-679283.jpg&token=d545c06844d5b1fae60be67316374bce%3A1674817928", media_subject: "specimen", push_asset_to_specify: "no", push_metadata_to_specify: "no" })
   MERGE (m:Media { name: "AK8681267_Y_obnoxious_loon", date_media_created: "2008-12-03 15:23:08", media_guid: "7fc097ca-2a80-4b3b-a633-1bb665c76cb0", media_pid: "", payload_type: "image", file_format: "tif", file_info: "" })
   MERGE (w)-[:STATIONED_AT]->(i)
   MERGE (m)-[:MEDIA_CREATED_BY]->(d)
   MERGE (a)-[:OWNED_BY]->(co)
   MERGE (a)-[:PROPERTY_OF]->(i)
   MERGE (a)-[:PART_OF_COLLECTION]->(c)
   MERGE (a)-[:HAS_MEDIA]->(m)
   MERGE (a)-[:METADATA_CREATED_BY]->(d)
   MERGE (m)-[:EQUIPTMENT_USED]->(e)
   MERGE (m)-[:CREATED_ON]->(w)
   RETURN i
$$) as (institute agtype);


select * from cypher('dassco', $$
    MERGE (i:Institute { name: "WNAD", ocr_text: "FLORA DANICA EXSICCATA Lycopodium selago L. Jyll Silkeborg Vesterskov YII 1904 leg. M. Lorenzen.", geographic_region: "", taxon_name: "" })
   MERGE (d:Digitiser { name: "Susan Giannini" })
   MERGE (co:CopyrightOwner { name: "WNAD" })
   MERGE (p:Pipelines {name: "PL_TR001" })
   MERGE (w:WorkStation {name: "WS_B0004" })
   MERGE (c:Collection { name: "Bryozoology" })
   MERGE (e:Equiptment { name: "Equipment", equipment_details: "[]", exposure_time: "", f_number: "", focal_length: "", iso_setting: "", white_balance: "" })
   MERGE (a:Asset { name: "Asset", notes: "[]", barcode: "", specimen_pid: "", specify_specimen_id: "211003d9-b81b-422b-8cba-9b86b1b34f09", specify_attachment_id: "acfea4fe-e9bb-4b50-b4e8-5b75358e3a20", multi_specimen_status: "yes", other_multi_specimen: "", external_link: "", access_level: "", type_status: "", specimen_storage_location: "", funding: "", embargo_type: "", embargo_notes: "[]", original_specify_media_name: "https://specify-attachments.science.ku.dk/fileget?coll=NHMD+Vascular+Plants&type=O&filename=sp68923230029256349442.att.jpg&downloadname=NHMD-679283.jpg&token=d545c06844d5b1fae60be67316374bce%3A1674817928", media_subject: "specimen", push_asset_to_specify: "yes", push_metadata_to_specify: "no" })
   MERGE (m:Media { name: "TO0618590_Y_thirsty_wolverine", date_media_created: "2016-05-01 07:00:57", media_guid: "dfbb8c11-02e1-41a7-8914-5040996472c6", media_pid: "", payload_type: "image", file_format: "tif", file_info: "" })
   MERGE (w)-[:STATIONED_AT]->(i)
   MERGE (m)-[:MEDIA_CREATED_BY]->(d)
   MERGE (a)-[:OWNED_BY]->(co)
   MERGE (a)-[:PROPERTY_OF]->(i)
   MERGE (a)-[:PART_OF_COLLECTION]->(c)
   MERGE (a)-[:HAS_MEDIA]->(m)
   MERGE (a)-[:METADATA_CREATED_BY]->(d)
   MERGE (m)-[:EQUIPTMENT_USED]->(e)
   MERGE (m)-[:CREATED_ON]->(w)
   RETURN i
$$) as (institute agtype);


select * from cypher('dassco', $$
    MERGE (i:Institute { name: "NHMD", ocr_text: "FLORA DANICA EXSICCATA Lycopodium selago L. Jyll Silkeborg Vesterskov YII 1904 leg. M. Lorenzen.", geographic_region: "", taxon_name: "" })
   MERGE (d:Digitiser { name: "Alice Harrison" })
   MERGE (co:CopyrightOwner { name: "NHMD" })
   MERGE (p:Pipelines {name: "PL_BC043" })
   MERGE (w:WorkStation {name: "WS_B0001" })
   MERGE (c:Collection { name: "Zooarchaeology" })
   MERGE (e:Equiptment { name: "Equipment", equipment_details: "[]", exposure_time: "", f_number: "", focal_length: "", iso_setting: "", white_balance: "" })
   MERGE (a:Asset { name: "Asset", notes: "[]", barcode: "", specimen_pid: "", specify_specimen_id: "4a9f6e8e-6466-4edd-aec5-306f6d0a32ac", specify_attachment_id: "c51fa401-9aaa-4bd6-b36e-0fdea9743e72", multi_specimen_status: "no", other_multi_specimen: "", external_link: "", access_level: "", type_status: "", specimen_storage_location: "", funding: "", embargo_type: "", embargo_notes: "[]", original_specify_media_name: "https://specify-attachments.science.ku.dk/fileget?coll=NHMD+Vascular+Plants&type=O&filename=sp68923230029256349442.att.jpg&downloadname=NHMD-679283.jpg&token=d545c06844d5b1fae60be67316374bce%3A1674817928", media_subject: "specimen", push_asset_to_specify: "no", push_metadata_to_specify: "no" })
   MERGE (m:Media { name: "TY8784439_K_acoustic_rhino", date_media_created: "2020-09-14 15:51:01", media_guid: "dbaa2753-41cd-41bd-a118-e22169e89ad3", media_pid: "", payload_type: "image", file_format: "tif", file_info: "" })
   MERGE (w)-[:STATIONED_AT]->(i)
   MERGE (m)-[:MEDIA_CREATED_BY]->(d)
   MERGE (a)-[:OWNED_BY]->(co)
   MERGE (a)-[:PROPERTY_OF]->(i)
   MERGE (a)-[:PART_OF_COLLECTION]->(c)
   MERGE (a)-[:HAS_MEDIA]->(m)
   MERGE (a)-[:METADATA_CREATED_BY]->(d)
   MERGE (m)-[:EQUIPTMENT_USED]->(e)
   MERGE (m)-[:CREATED_ON]->(w)
   RETURN i
$$) as (institute agtype);


select * from cypher('dassco', $$
    MERGE (i:Institute { name: "OWRL", ocr_text: "FLORA DANICA EXSICCATA Lycopodium selago L. Jyll Silkeborg Vesterskov YII 1904 leg. M. Lorenzen.", geographic_region: "", taxon_name: "" })
   MERGE (d:Digitiser { name: "Thomas Hollingsworth" })
   MERGE (co:CopyrightOwner { name: "OWRL" })
   MERGE (p:Pipelines {name: "PL_TR201" })
   MERGE (w:WorkStation {name: "WS_C0013" })
   MERGE (c:Collection { name: "Bryozoology" })
   MERGE (e:Equiptment { name: "Equipment", equipment_details: "[]", exposure_time: "", f_number: "", focal_length: "", iso_setting: "", white_balance: "" })
   MERGE (a:Asset { name: "Asset", notes: "[]", barcode: "", specimen_pid: "", specify_specimen_id: "58536611-b4cc-4a76-9024-c09e1bf7b6f8", specify_attachment_id: "3823ecff-a115-4dcc-af69-30fe6c90be31", multi_specimen_status: "no", other_multi_specimen: "", external_link: "", access_level: "", type_status: "", specimen_storage_location: "", funding: "", embargo_type: "", embargo_notes: "[]", original_specify_media_name: "https://specify-attachments.science.ku.dk/fileget?coll=NHMD+Vascular+Plants&type=O&filename=sp68923230029256349442.att.jpg&downloadname=NHMD-679283.jpg&token=d545c06844d5b1fae60be67316374bce%3A1674817928", media_subject: "specimen", push_asset_to_specify: "no", push_metadata_to_specify: "yes" })
   MERGE (m:Media { name: "TY9178510_K_psychedelic_earwig", date_media_created: "2015-10-18 01:33:34", media_guid: "e0693222-930c-476e-8e66-f1d0f8f6f990", media_pid: "", payload_type: "image", file_format: "tif", file_info: "" })
   MERGE (w)-[:STATIONED_AT]->(i)
   MERGE (m)-[:MEDIA_CREATED_BY]->(d)
   MERGE (a)-[:OWNED_BY]->(co)
   MERGE (a)-[:PROPERTY_OF]->(i)
   MERGE (a)-[:PART_OF_COLLECTION]->(c)
   MERGE (a)-[:HAS_MEDIA]->(m)
   MERGE (a)-[:METADATA_CREATED_BY]->(d)
   MERGE (m)-[:EQUIPTMENT_USED]->(e)
   MERGE (m)-[:CREATED_ON]->(w)
   RETURN i
$$) as (institute agtype);

select * from cypher('dassco', $$
    MERGE (i:Institute { name: "NNAD", ocr_text: "FLORA DANICA EXSICCATA Lycopodium selago L. Jyll Silkeborg Vesterskov YII 1904 leg. M. Lorenzen.", geographic_region: "", taxon_name: "" })
   MERGE (d:Digitiser { name: "Michael Gregory" })
   MERGE (co:CopyrightOwner { name: "NNAD" })
   MERGE (p:Pipelines {name: "PL_AC002" })
   MERGE (w:WorkStation {name: "WS_C0013" })
   MERGE (c:Collection { name: "Paleozoology" })
   MERGE (e:Equiptment { name: "Equipment", equipment_details: "[]", exposure_time: "", f_number: "", focal_length: "", iso_setting: "", white_balance: "" })
   MERGE (a:Asset { name: "Asset", notes: "[]", barcode: "", specimen_pid: "", specify_specimen_id: "ab59ad9c-6aea-49b4-95c4-25e4c0095e20", specify_attachment_id: "6b9c9cd2-0aff-466e-902e-9703073bd664", multi_specimen_status: "no", other_multi_specimen: "", external_link: "", access_level: "", type_status: "", specimen_storage_location: "", funding: "", embargo_type: "", embargo_notes: "[]", original_specify_media_name: "https://specify-attachments.science.ku.dk/fileget?coll=NHMD+Vascular+Plants&type=O&filename=sp68923230029256349442.att.jpg&downloadname=NHMD-679283.jpg&token=d545c06844d5b1fae60be67316374bce%3A1674817928", media_subject: "specimen", push_asset_to_specify: "no", push_metadata_to_specify: "yes" })
   MERGE (m:Media { name: "CP0004369_A_eager_robin", date_media_created: "2013-12-13 22:55:27", media_guid: "b15e6c6d-263b-4013-b314-d567acd1b618", media_pid: "", payload_type: "image", file_format: "tif", file_info: "" })
   MERGE (w)-[:STATIONED_AT]->(i)
   MERGE (m)-[:MEDIA_CREATED_BY]->(d)
   MERGE (a)-[:OWNED_BY]->(co)
   MERGE (a)-[:PROPERTY_OF]->(i)
   MERGE (a)-[:PART_OF_COLLECTION]->(c)
   MERGE (a)-[:HAS_MEDIA]->(m)
   MERGE (a)-[:METADATA_CREATED_BY]->(d)
   MERGE (m)-[:EQUIPTMENT_USED]->(e)
   MERGE (m)-[:CREATED_ON]->(w)
   RETURN i
$$) as (institute agtype);


select * from cypher('dassco', $$
    MERGE (i:Institute { name: "OWRL", ocr_text: "FLORA DANICA EXSICCATA Lycopodium selago L. Jyll Silkeborg Vesterskov YII 1904 leg. M. Lorenzen.", geographic_region: "", taxon_name: "" })
   MERGE (d:Digitiser { name: "Delia Sanchez" })
   MERGE (co:CopyrightOwner { name: "OWRL" })
   MERGE (p:Pipelines {name: "PL_TR001" })
   MERGE (w:WorkStation {name: "WS_D0001" })
   MERGE (c:Collection { name: "Cnidariology" })
   MERGE (e:Equiptment { name: "Equipment", equipment_details: "[]", exposure_time: "", f_number: "", focal_length: "", iso_setting: "", white_balance: "" })
   MERGE (a:Asset { name: "Asset", notes: "[]", barcode: "", specimen_pid: "", specify_specimen_id: "5c9df265-a4af-45e4-bd88-1098c4f76cf2", specify_attachment_id: "7635c2b2-9714-416e-84ed-eaeb62d96642", multi_specimen_status: "yes", other_multi_specimen: "", external_link: "", access_level: "", type_status: "", specimen_storage_location: "", funding: "", embargo_type: "", embargo_notes: "[]", original_specify_media_name: "https://specify-attachments.science.ku.dk/fileget?coll=NHMD+Vascular+Plants&type=O&filename=sp68923230029256349442.att.jpg&downloadname=NHMD-679283.jpg&token=d545c06844d5b1fae60be67316374bce%3A1674817928", media_subject: "specimen", push_asset_to_specify: "yes", push_metadata_to_specify: "no" })
   MERGE (m:Media { name: "TO2341510_K_resolute_flounder", date_media_created: "2021-03-11 16:02:28", media_guid: "672c27bd-ae32-4c8c-a46c-fb9a9ddd30b7", media_pid: "", payload_type: "image", file_format: "tif", file_info: "" })
   MERGE (w)-[:STATIONED_AT]->(i)
   MERGE (m)-[:MEDIA_CREATED_BY]->(d)
   MERGE (a)-[:OWNED_BY]->(co)
   MERGE (a)-[:PROPERTY_OF]->(i)
   MERGE (a)-[:PART_OF_COLLECTION]->(c)
   MERGE (a)-[:HAS_MEDIA]->(m)
   MERGE (a)-[:METADATA_CREATED_BY]->(d)
   MERGE (m)-[:EQUIPTMENT_USED]->(e)
   MERGE (m)-[:CREATED_ON]->(w)
   RETURN i
$$) as (institute agtype);


select * from cypher('dassco', $$
    MERGE (i:Institute { name: "NNAD", ocr_text: "FLORA DANICA EXSICCATA Lycopodium selago L. Jyll Silkeborg Vesterskov YII 1904 leg. M. Lorenzen.", geographic_region: "", taxon_name: "" })
   MERGE (d:Digitiser { name: "Margaret Mcclurkin" })
   MERGE (co:CopyrightOwner { name: "NNAD" })
   MERGE (p:Pipelines {name: "PL_AC002" })
   MERGE (w:WorkStation {name: "WS_B0003" })
   MERGE (c:Collection { name: "Helminthology" })
   MERGE (e:Equiptment { name: "Equipment", equipment_details: "[]", exposure_time: "", f_number: "", focal_length: "", iso_setting: "", white_balance: "" })
   MERGE (a:Asset { name: "Asset", notes: "[]", barcode: "", specimen_pid: "", specify_specimen_id: "d0dc5b51-e88c-4ebb-8d3e-a232bd44cfeb", specify_attachment_id: "c87bb46e-ffd1-4dab-857e-1d5b5f6e3053", multi_specimen_status: "no", other_multi_specimen: "", external_link: "", access_level: "", type_status: "", specimen_storage_location: "", funding: "", embargo_type: "", embargo_notes: "[]", original_specify_media_name: "https://specify-attachments.science.ku.dk/fileget?coll=NHMD+Vascular+Plants&type=O&filename=sp68923230029256349442.att.jpg&downloadname=NHMD-679283.jpg&token=d545c06844d5b1fae60be67316374bce%3A1674817928", media_subject: "specimen", push_asset_to_specify: "yes", push_metadata_to_specify: "no" })
   MERGE (m:Media { name: "TO0031854_A_itchy_sheep", date_media_created: "2021-05-03 20:02:40", media_guid: "9fe110a4-30cb-4ced-abd4-08d77a1cf4ff", media_pid: "", payload_type: "image", file_format: "tif", file_info: "" })
   MERGE (w)-[:STATIONED_AT]->(i)
   MERGE (m)-[:MEDIA_CREATED_BY]->(d)
   MERGE (a)-[:OWNED_BY]->(co)
   MERGE (a)-[:PROPERTY_OF]->(i)
   MERGE (a)-[:PART_OF_COLLECTION]->(c)
   MERGE (a)-[:HAS_MEDIA]->(m)
   MERGE (a)-[:METADATA_CREATED_BY]->(d)
   MERGE (m)-[:EQUIPTMENT_USED]->(e)
   MERGE (m)-[:CREATED_ON]->(w)
   RETURN i
$$) as (institute agtype);


select * from cypher('dassco', $$
    MERGE (i:Institute { name: "OWRL", ocr_text: "FLORA DANICA EXSICCATA Lycopodium selago L. Jyll Silkeborg Vesterskov YII 1904 leg. M. Lorenzen.", geographic_region: "", taxon_name: "" })
   MERGE (d:Digitiser { name: "Alice Harrison" })
   MERGE (co:CopyrightOwner { name: "OWRL" })
   MERGE (p:Pipelines {name: "PL_TR201" })
   MERGE (w:WorkStation {name: "WS_A0001" })
   MERGE (c:Collection { name: "Bryozoology" })
   MERGE (e:Equiptment { name: "Equipment", equipment_details: "[]", exposure_time: "", f_number: "", focal_length: "", iso_setting: "", white_balance: "" })
   MERGE (a:Asset { name: "Asset", notes: "[]", barcode: "", specimen_pid: "", specify_specimen_id: "ff43c52b-da41-490d-93cc-35e8939f2dcd", specify_attachment_id: "cc971b19-fd8c-4baf-ae31-ed336ba47af3", multi_specimen_status: "no", other_multi_specimen: "", external_link: "", access_level: "", type_status: "", specimen_storage_location: "", funding: "", embargo_type: "", embargo_notes: "[]", original_specify_media_name: "https://specify-attachments.science.ku.dk/fileget?coll=NHMD+Vascular+Plants&type=O&filename=sp68923230029256349442.att.jpg&downloadname=NHMD-679283.jpg&token=d545c06844d5b1fae60be67316374bce%3A1674817928", media_subject: "specimen", push_asset_to_specify: "no", push_metadata_to_specify: "no" })
   MERGE (m:Media { name: "TO4057134_Y_rustic_robin", date_media_created: "2003-07-16 07:41:37", media_guid: "ab137544-486a-463b-b0c5-ebedb82a92ae", media_pid: "", payload_type: "image", file_format: "tif", file_info: "" })
   MERGE (w)-[:STATIONED_AT]->(i)
   MERGE (m)-[:MEDIA_CREATED_BY]->(d)
   MERGE (a)-[:OWNED_BY]->(co)
   MERGE (a)-[:PROPERTY_OF]->(i)
   MERGE (a)-[:PART_OF_COLLECTION]->(c)
   MERGE (a)-[:HAS_MEDIA]->(m)
   MERGE (a)-[:METADATA_CREATED_BY]->(d)
   MERGE (m)-[:EQUIPTMENT_USED]->(e)
   MERGE (m)-[:CREATED_ON]->(w)
   RETURN i
$$) as (institute agtype);


select * from cypher('dassco', $$
    MERGE (i:Institute { name: "NNAD", ocr_text: "FLORA DANICA EXSICCATA Lycopodium selago L. Jyll Silkeborg Vesterskov YII 1904 leg. M. Lorenzen.", geographic_region: "", taxon_name: "" })
   MERGE (d:Digitiser { name: "Alice Harrison" })
   MERGE (co:CopyrightOwner { name: "NNAD" })
   MERGE (p:Pipelines {name: "PL_TR001" })
   MERGE (w:WorkStation {name: "WS_B0002" })
   MERGE (c:Collection { name: "Nematology" })
   MERGE (e:Equiptment { name: "Equipment", equipment_details: "[]", exposure_time: "", f_number: "", focal_length: "", iso_setting: "", white_balance: "" })
   MERGE (a:Asset { name: "Asset", notes: "[]", barcode: "", specimen_pid: "", specify_specimen_id: "df6c6de0-a077-44e5-b06f-b5dd32cb4b71", specify_attachment_id: "748db005-dfd1-4d48-87ba-fe84085ec580", multi_specimen_status: "no", other_multi_specimen: "", external_link: "", access_level: "", type_status: "", specimen_storage_location: "", funding: "", embargo_type: "", embargo_notes: "[]", original_specify_media_name: "https://specify-attachments.science.ku.dk/fileget?coll=NHMD+Vascular+Plants&type=O&filename=sp68923230029256349442.att.jpg&downloadname=NHMD-679283.jpg&token=d545c06844d5b1fae60be67316374bce%3A1674817928", media_subject: "specimen", push_asset_to_specify: "no", push_metadata_to_specify: "yes" })
   MERGE (m:Media { name: "AL0974696_D_fragile_trogon", date_media_created: "2020-03-01 02:40:37", media_guid: "d7949c24-7e73-4b23-8663-8bc0ef535f66", media_pid: "", payload_type: "image", file_format: "tif", file_info: "" })
   MERGE (w)-[:STATIONED_AT]->(i)
   MERGE (m)-[:MEDIA_CREATED_BY]->(d)
   MERGE (a)-[:OWNED_BY]->(co)
   MERGE (a)-[:PROPERTY_OF]->(i)
   MERGE (a)-[:PART_OF_COLLECTION]->(c)
   MERGE (a)-[:HAS_MEDIA]->(m)
   MERGE (a)-[:METADATA_CREATED_BY]->(d)
   MERGE (m)-[:EQUIPTMENT_USED]->(e)
   MERGE (m)-[:CREATED_ON]->(w)
   RETURN i
$$) as (institute agtype);


select * from cypher('dassco', $$
    MERGE (i:Institute { name: "OWRL", ocr_text: "FLORA DANICA EXSICCATA Lycopodium selago L. Jyll Silkeborg Vesterskov YII 1904 leg. M. Lorenzen.", geographic_region: "", taxon_name: "" })
   MERGE (d:Digitiser { name: "Delia Sanchez" })
   MERGE (co:CopyrightOwner { name: "OWRL" })
   MERGE (p:Pipelines {name: "PL_TR201" })
   MERGE (w:WorkStation {name: "WS_B0002" })
   MERGE (c:Collection { name: "Anthrozoology" })
   MERGE (e:Equiptment { name: "Equipment", equipment_details: "[]", exposure_time: "", f_number: "", focal_length: "", iso_setting: "", white_balance: "" })
   MERGE (a:Asset { name: "Asset", notes: "[]", barcode: "", specimen_pid: "", specify_specimen_id: "bec3e310-7790-4037-8a9a-b28815665a87", specify_attachment_id: "f63f42df-5a71-4a37-9d06-c900c26342a3", multi_specimen_status: "yes", other_multi_specimen: "", external_link: "", access_level: "", type_status: "", specimen_storage_location: "", funding: "", embargo_type: "", embargo_notes: "[]", original_specify_media_name: "https://specify-attachments.science.ku.dk/fileget?coll=NHMD+Vascular+Plants&type=O&filename=sp68923230029256349442.att.jpg&downloadname=NHMD-679283.jpg&token=d545c06844d5b1fae60be67316374bce%3A1674817928", media_subject: "specimen", push_asset_to_specify: "yes", push_metadata_to_specify: "yes" })
   MERGE (m:Media { name: "AK0969305_K_dusty_rhino", date_media_created: "2018-05-20 03:12:51", media_guid: "73a40854-fee0-46cd-b930-3a12f3484e37", media_pid: "", payload_type: "image", file_format: "tif", file_info: "" })
   MERGE (w)-[:STATIONED_AT]->(i)
   MERGE (m)-[:MEDIA_CREATED_BY]->(d)
   MERGE (a)-[:OWNED_BY]->(co)
   MERGE (a)-[:PROPERTY_OF]->(i)
   MERGE (a)-[:PART_OF_COLLECTION]->(c)
   MERGE (a)-[:HAS_MEDIA]->(m)
   MERGE (a)-[:METADATA_CREATED_BY]->(d)
   MERGE (m)-[:EQUIPTMENT_USED]->(e)
   MERGE (m)-[:CREATED_ON]->(w)
   RETURN i
$$) as (institute agtype);
