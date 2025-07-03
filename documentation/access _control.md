# *** Access control in ARS via Keycloak ***


Setup for testing Acces control on Instution and Collection Level in ARS
The following is an test example of how to set up/ test Access control in ARS
Initial Setup - 
Institution in ARS: There should be a institution that should exisit in ARS, for example: "control"
Collections in ARS: There should be collections in the institution that should exisit in ARS, for example: "Control reader", "Control writer" are the Collections in Institution "control"
Users in Keycloak: There should be users that should exisit in Keycloack for example: "creader", "cwriter", "ireader", "iwriter"
Realm Roles in Keycloak: READ_Control reader_role, READ_control_role, WRITE_Control writer_role", "WRITE_control_role"


Step 1

Assign the Realm roles to the users.
creader: READ_Control reader_role
cwriter: WRITE_Control writer_role
ireader: READ_control_role
iwriter: WRITE_control_role
Step 2

Update the Institution and Collections to get the right permissions for the users
Body of Update Institution(use this - wrong in swagger)

{ "name": "control", "roleRestriction": [{"name": "control_role"}] }

Body of Update collection(wrong in swagger, use this)

{ "name": "Control writer", "institution": "control", "roleRestrictions": [{"name": "Control writer_role"}] }
