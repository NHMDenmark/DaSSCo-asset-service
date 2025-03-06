CREATE EXTENSION IF NOT EXISTS age;
LOAD
'age';
SET
search_path = ag_catalog, "$user", public;

-- Seed db with initial values
SELECT *
FROM ag_catalog.cypher('dassco'
    , $$ MERGE (s:Status {status: "WORKING_COPY"})
    RETURN s
$$) as (status agtype);

SELECT *
FROM ag_catalog.cypher('dassco'
    , $$ MERGE (s:Status {status: "ARCHIVE"})
    RETURN s
$$) as (status agtype);

SELECT *
FROM ag_catalog.cypher('dassco'
    , $$ MERGE (s:Status {status: "BEING_PROCESSED"})
    RETURN s
$$) as (status agtype);

SELECT *
FROM ag_catalog.cypher('dassco'
    , $$ MERGE (s:Status {status: "PROCESSING_HALTED"})
    RETURN s
$$) as (status agtype);

SELECT *
FROM ag_catalog.cypher('dassco'
    , $$ MERGE (s:Status {status: "ISSUE_WITH_MEDIA"})
    RETURN s
$$) as (status agtype);

SELECT *
FROM ag_catalog.cypher('dassco'
    , $$ MERGE (s:Status {status: "ISSUE_WITH_METADATA"})
    RETURN s
$$) as (status agtype);

SELECT *
FROM ag_catalog.cypher('dassco'
    , $$ MERGE (s:Status {status: "FOR_DELETION"})
    RETURN s
$$) as (status agtype);

-- Seed file format with initial values
SELECT *
FROM ag_catalog.cypher('dassco'
    , $$ MERGE (f:File_format
        {file_format: "TIF"})
    RETURN f
$$) as (name agtype);

SELECT *
FROM ag_catalog.cypher('dassco'
    , $$ MERGE (f:File_format
        {file_format: "JPEG"})
    RETURN f
$$) as (file_format agtype);

SELECT *
FROM ag_catalog.cypher('dassco'
    , $$ MERGE (f:File_format
        {file_format: "RAW"})
    RETURN f
$$) as (file_format agtype);

SELECT *
FROM ag_catalog.cypher('dassco'
    , $$ MERGE (f:File_format
        {file_format: "RAF"})
    RETURN f
$$) as (file_format agtype);

SELECT *
FROM ag_catalog.cypher('dassco'
    , $$ MERGE (f:File_format
        {file_format: "CR3"})
    RETURN f
$$) as (file_format agtype);

SELECT *
FROM ag_catalog.cypher('dassco'
    , $$ MERGE (f:File_format
        {file_format: "DNG"})
    RETURN f
$$) as (file_format agtype);

SELECT *
FROM ag_catalog.cypher('dassco'
    , $$ MERGE (f:File_format
        {file_format: "TXT"})
    RETURN f
$$) as (file_format agtype);



