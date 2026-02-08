# wallet-ledger (Spring Boot 3 + Hibernate 6)

## Setup
1. Create a PostgreSQL DB and run your installer DDL (paste into `src/main/resources/schema.sql` and run manually).
2. Update `src/main/resources/application.yml` with DB url/username/password.
3. Run:
   mvn clean spring-boot:run

## Notes
- JPA does not model Postgres partitioning, but mapping the parent tables works fine.
- `spring.jpa.hibernate.ddl-auto=validate` is enabled (DDL-friendly).
- `v_user_statement` is mapped read-only using `@Immutable`.
