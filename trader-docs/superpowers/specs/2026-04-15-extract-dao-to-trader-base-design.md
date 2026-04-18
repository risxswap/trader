# 2026-04-15-extract-dao-to-trader-base-design

## Context
The workspace consists of several microservices (`trader-admin`, `trader-collector`, `trader-executor`) that share a common data access layer (DAO, Entity, Mapper). Currently, these classes (e.g., `FundDao`, `Fund`, `FundMapper`) are duplicated across the projects. 

## Goal
Extract the duplicated DAO layer from `trader-admin`, `trader-collector`, and `trader-executor` into the shared `trader-base` module. Other projects will reference `trader-base` to perform database operations, reducing code duplication and centralizing data access logic.

## Architecture & Data Flow
1. **Centralized DAOs**: The `trader-base` project will host a new `cc.riskswap.trader.base.dao` package (along with `.entity` and `.mapper` subpackages).
2. **Merging Duplicates**: Duplicate entities, mappers, and DAO classes from the three projects will be merged. If `FundDao` exists in multiple projects with different helper methods, those methods will be combined into a single `FundDao` class in `trader-base`.
3. **Dependency Injection**: The `trader-base` module uses Mybatis-Plus. The `@Repository` DAOs and `@Mapper` classes in `trader-base` will be scanned and injected into the service layers of the consuming projects.

## Implementation Steps

### 1. Extract and Merge DAOs into `trader-base`
- Create `cc.riskswap.trader.base.dao`, `cc.riskswap.trader.base.dao.entity`, and `cc.riskswap.trader.base.dao.mapper` packages.
- Move all unique entities, mappers, and DAOs from the three projects into these packages.
- For duplicated classes (like `Fund`, `FundDao`), manually merge their fields, annotations, and methods.
- Ensure all moved classes use the correct `cc.riskswap.trader.base.dao.*` package declaration.

### 2. Configure `trader-base`
- Add necessary dependencies (like `mybatis-plus-boot-starter`, `lombok`, etc.) if not fully present for these DAOs.
- Ensure `TraderDataSourceAutoConfiguration` correctly scans the new mapper packages if needed, though it seems it relies on annotations like `@MysqlMapper` and `@ClickHouseMapper`.

### 3. Update Consuming Projects (`trader-admin`, `trader-collector`, `trader-executor`)
- Add `trader-base` as a Maven dependency to `trader-collector` (and verify for others).
- Delete the old `dao`, `entity`, and `mapper` packages from these projects.
- Update all `import` statements in Services, Controllers, Tasks, and Tests to point to `cc.riskswap.trader.base.dao.*`.
- Ensure `@SpringBootApplication` or `@ComponentScan` includes `cc.riskswap.trader.base` to pick up the DAOs.

## Trade-offs and Considerations
- **Pros**: Single source of truth for the database schema and queries. Less duplicate code to maintain.
- **Cons**: Any change to an entity or DAO will require recompiling `trader-base` and updating downstream projects, coupling them more tightly at the data layer. This is standard for shared libraries.
- **Testing**: We must run a full maven build (`mvn clean install`) across the workspace to catch any broken imports or missing methods.
