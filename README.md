# Queries

## TODO

- [ ] Synchronize LDAP server with user table on sign in
- [ ] Enforce LDAP authentication for all routes

## LDAP

### search

`ldapsearch -H ldap://localhost -b dc=queries,dc=org -D cn=admin,dc=queries,dc=org -w admin`

### add users

`ldapadd -H ldap://localhost -D cn=admin,dc=queries,dc=org -w admin -f ldap/bootstrap/users.ldif`
