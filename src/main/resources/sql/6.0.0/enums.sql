CREATE EXTENSION IF NOT EXISTS age;
LOAD
'age';
SET
search_path = ag_catalog, "$user", public;

-- Seed db with initial values
-- Status
SELECT *
FROM ag_catalog.cypher('dassco'
    , $$ MERGE (s:Status {name: "WORKING_COPY"})
    RETURN s
$$) as (status agtype);

SELECT *
FROM ag_catalog.cypher('dassco'
    , $$ MERGE (s:Status {name: "ARCHIVE"})
    RETURN s
$$) as (status agtype);

SELECT *
FROM ag_catalog.cypher('dassco'
    , $$ MERGE (s:Status {name: "BEING_PROCESSED"})
    RETURN s
$$) as (status agtype);

SELECT *
FROM ag_catalog.cypher('dassco'
    , $$ MERGE (s:Status {name: "PROCESSING_HALTED"})
    RETURN s
$$) as (status agtype);

SELECT *
FROM ag_catalog.cypher('dassco'
    , $$ MERGE (s:Status {name: "ISSUE_WITH_MEDIA"})
    RETURN s
$$) as (status agtype);

SELECT *
FROM ag_catalog.cypher('dassco'
    , $$ MERGE (s:Status {name: "ISSUE_WITH_METADATA"})
    RETURN s
$$) as (status agtype);

SELECT *
FROM ag_catalog.cypher('dassco'
    , $$ MERGE (s:Status {name: "FOR_DELETION"})
    RETURN s
$$) as (status agtype);

-- File format
SELECT *
FROM ag_catalog.cypher('dassco'
    , $$ MERGE (f:File_format
        {name: "TIF"})
    RETURN f
$$) as (name agtype);

SELECT *
FROM ag_catalog.cypher('dassco'
    , $$ MERGE (f:File_format
        {name: "JPEG"})
    RETURN f
$$) as (file_format agtype);

SELECT *
FROM ag_catalog.cypher('dassco'
    , $$ MERGE (f:File_format
        {name: "RAW"})
    RETURN f
$$) as (file_format agtype);

SELECT *
FROM ag_catalog.cypher('dassco'
    , $$ MERGE (f:File_format
        {name: "RAF"})
    RETURN f
$$) as (file_format agtype);

SELECT *
FROM ag_catalog.cypher('dassco'
    , $$ MERGE (f:File_format
        {name: "CR3"})
    RETURN f
$$) as (file_format agtype);

SELECT *
FROM ag_catalog.cypher('dassco'
    , $$ MERGE (f:File_format
        {name: "DNG"})
    RETURN f
$$) as (file_format agtype);

SELECT *
FROM ag_catalog.cypher('dassco'
    , $$ MERGE (f:File_format
        {name: "TXT"})
    RETURN f
$$) as (file_format agtype);

-- Internal status
SELECT *
FROM ag_catalog.cypher('dassco'
    , $$ MERGE (i:Internal_status
        {name: "METADATA_RECEIVED"})
    RETURN i
$$) as (Internal_status agtype);

SELECT *
FROM ag_catalog.cypher('dassco'
    , $$ MERGE (i:Internal_status
        {name: "ASSET_RECEIVED"})
    RETURN i
$$) as (Internal_status agtype);

SELECT *
FROM ag_catalog.cypher('dassco'
    , $$ MERGE (i:Internal_status
        {name: "COMPLETED"})
    RETURN i
$$) as (Internal_status agtype);

SELECT *
FROM ag_catalog.cypher('dassco'
    , $$ MERGE (i:Internal_status
        {name: "ERDA_ERROR"})
    RETURN i
$$) as (Internal_status agtype);



