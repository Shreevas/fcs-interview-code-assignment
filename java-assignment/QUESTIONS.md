# Questions

Here we have 3 questions related to the code base for you to answer. It is not about right or wrong, but more about what's the reasoning behind your decisions.

1. In this code base, we have some different implementation strategies when it comes to database access layer and manipulation. If you would maintain this code base, would you refactor any of those? Why?

**Answer:**
```txt
The project currently uses three different approaches for saving and loading data, which makes the code harder to understand and maintain.

Store communicates directly with the database. It fetches and saves its own records without an additional layer. This is simple, but it also makes the Store logic more dependent on the database and harder to test independently.

Product uses a small repository layer in front of the database. However, the repository does not add much additional functionality, so it provides limited benefit compared with the direct approach used by Store.

Warehouse follows a more structured layered approach. The business rules, such as checking whether a warehouse can be created or whether its stock exceeds its capacity, are separated from the database code. This separation makes the business rules easier to test without requiring a real database. The tests can run quickly and focus only on the application logic.

The Warehouse approach does require more code, and there is one small inconsistency. One part of the Warehouse implementation bypasses the defined abstraction and accesses the database repository directly. This should be cleaned up so that the separation between the application logic and database layer remains consistent.

For long-term maintenance, features that contain important business rules should follow the Warehouse approach, including the Fulfillment feature. For simple CRUD features that only save and retrieve records without significant business rules, the simpler approach is sufficient. Product is a good example of a feature where additional layers would add complexity without much practical benefit.
```
----
2. When it comes to API spec and endpoints handlers, we have an Open API yaml file for the `Warehouse` API from which we generate code, but for the other endpoints - `Product` and `Store` - we just coded directly everything. What would be your thoughts about what are the pros and cons of each approach and what would be your choice?

**Answer:**
```txt
The project can use two approaches for API development: a contract-first approach like Warehouse, or a hand-written approach like Store and Product.

The contract-first approach starts with an OpenAPI specification that defines the API before the implementation. This helps keep the API contract and the implementation aligned. Changes to the specification are reflected in the generated code, and mismatches can be identified during the build instead of causing problems later. The specification also provides clear documentation that other teams can review and agree on before using the API.

However, the generated approach has some limitations. The generated Warehouse API does not provide enough control over the HTTP response in some cases. For example, the specification expects a 201 response when creating a warehouse, but the generated implementation can return 200 instead. Code generation also adds an extra conversion step because the generated models are not always the same as the application's internal models.

The hand-written approach provides more control over HTTP status codes, headers, request handling, validation, and response formats. It is also simpler to change because there is no additional code-generation step. The main disadvantage is that there is no separate API contract outside the code. A small change, such as renaming a field, could unintentionally affect other API consumers.

The preferred approach would be to use the contract-first approach for stable APIs that are published or used by other teams or systems. This is suitable for the Warehouse API.

For simple internal APIs that change frequently, the hand-written approach is more practical. Store, Product, and Fulfillment fit this category because they do not currently have an external API contract.

If an API is expected to be used by other teams or systems, the contract-first approach should be preferred, with HTTP response requirements such as 201 being addressed in the API design before implementation.
```
----
3. Given the need to balance thorough testing with time and resource constraints, how would you prioritize and implement tests for this project? Which types of tests would you focus on, and how would you ensure test coverage remains effective over time?

**Answer:**
```txt
The testing strategy focuses more effort on areas where failures would have the biggest impact, instead of applying the same level of testing everywhere. The tests are divided into three main levels.

The first level is unit testing for business rules. This includes checking whether a warehouse can be created in a specific location, whether replacing a warehouse maintains correct stock information, and whether a warehouse has reached its product limit. These tests do not require a database or a running application, so they execute very quickly. Most testing effort should be placed here, covering both successful scenarios and rules that should reject invalid data. Boundary cases, such as reaching the exact maximum limit or exceeding it by one, should also be covered. This is the main area where additional tests should be added as new business rules are introduced.

The second level is integration and application-level testing. These tests start the application and use a real database to verify that the different components work together correctly. They can verify things such as whether an invalid request returns the correct HTTP status and whether saved data is actually persisted. Since these tests take longer to run, only the important integration scenarios need to be covered instead of repeating every business rule already tested through unit tests.

The third level is a dedicated regression test for the transaction behavior. This ensures that the external legacy system is not notified when a database change is rolled back. This is an important scenario because the problem may not be obvious from the code and could be introduced again during future changes.

To keep the test coverage effective over time, an automated coverage check is used. The build fails if coverage falls below 80%. The coverage configuration also ensures that code executed through the application-level tests is measured correctly instead of being incorrectly reported as untested.

The coverage check runs automatically on every change pushed to GitHub, so it cannot easily be skipped.

However, the coverage percentage should not become the main goal. High coverage does not necessarily mean that the important scenarios are tested. The focus should remain on realistic business rules, edge cases, and failure scenarios rather than adding tests only to increase the coverage number.
```