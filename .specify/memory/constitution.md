<!--
================================================================================
SYNC IMPACT REPORT
================================================================================
Version change: 1.0.0 → 1.1.0 (MINOR - new principle added)

Modified principles: None

Added sections:
- IX. Documentation Language (Traditional Chinese requirement for specs/plans/docs)

Removed sections: None

Templates requiring updates:
- .specify/templates/plan-template.md: ✅ updated (Constitution Check table - IX added)
- .specify/templates/spec-template.md: ✅ updated (zh-TW language requirement note added)
- .specify/templates/tasks-template.md: ✅ updated (IX. Documentation Language compliance added)

Follow-up TODOs: None
================================================================================
-->

# Saga-Axon Constitution

## Core Principles

### I. Code Quality

All code MUST adhere to strict quality standards ensuring maintainability, readability, and correctness.

- Code MUST be self-documenting through clear naming conventions and logical structure
- Functions and methods MUST have single, well-defined responsibilities
- Cyclomatic complexity MUST remain below 10 per function; exceptions require documented justification
- All public APIs MUST include type definitions and contracts
- Code duplication MUST be eliminated through appropriate abstraction when the same logic appears 3+ times
- Magic numbers and strings MUST be extracted to named constants
- All warnings MUST be resolved or explicitly suppressed with documented rationale

### II. Testing Standards

Testing is NON-NEGOTIABLE. All production code MUST have corresponding test coverage.

- Unit test coverage MUST achieve minimum 80% line coverage for business logic
- Integration tests MUST verify all inter-component and external system interactions
- Contract tests MUST validate all API boundaries and service interfaces
- Tests MUST be deterministic: no flaky tests permitted in CI pipeline
- Test data MUST be isolated; tests MUST NOT depend on shared mutable state
- Performance-critical paths MUST include benchmark tests with defined thresholds
- All tests MUST pass before code can be merged to main branch

### III. Behavior Driven Development (BDD)

Features MUST be specified and validated through behavior-driven scenarios.

- All user stories MUST include Given-When-Then acceptance scenarios
- Scenarios MUST be written in domain language understandable by stakeholders
- Acceptance tests MUST directly map to specified scenarios
- Each scenario MUST test a single behavior or outcome
- Scenarios MUST be executable and included in CI pipeline
- Edge cases and error conditions MUST have corresponding scenarios
- Scenarios MUST be written BEFORE implementation begins

### IV. Domain Driven Design (DDD)

The codebase MUST reflect the business domain through strategic and tactical DDD patterns.

- Domain models MUST use Ubiquitous Language consistent with business terminology
- Bounded Contexts MUST be clearly defined with explicit boundaries
- Aggregates MUST protect invariants and enforce consistency boundaries
- Domain events MUST capture state changes for cross-context communication
- Repositories MUST abstract persistence; domain MUST NOT depend on infrastructure
- Value Objects MUST be used for concepts with no identity; immutability required
- Domain services MUST encapsulate operations that don't belong to entities

### V. SOLID Principles

All object-oriented code MUST adhere to SOLID principles.

- **Single Responsibility**: Each class/module MUST have exactly one reason to change
- **Open/Closed**: Components MUST be open for extension, closed for modification
- **Liskov Substitution**: Subtypes MUST be substitutable for their base types without altering correctness
- **Interface Segregation**: Clients MUST NOT be forced to depend on interfaces they don't use
- **Dependency Inversion**: High-level modules MUST NOT depend on low-level modules; both MUST depend on abstractions

### VI. Infrastructure Layer Isolation

Frameworks and external dependencies MUST be isolated to the infrastructure layer only.

- Domain and application layers MUST NOT import framework-specific code
- All framework integrations MUST be implemented through adapters in infrastructure layer
- Database access MUST use repository pattern with interfaces defined in domain layer
- External service clients MUST be wrapped in anti-corruption layers
- Configuration MUST be injected; no framework-specific configuration in domain code
- Switching frameworks MUST NOT require changes to domain or application layers
- Infrastructure dependencies MUST be injected via dependency injection containers

### VII. User Experience Consistency

All user-facing interfaces MUST provide consistent, predictable experiences.

- UI components MUST follow established design system patterns
- Error messages MUST be user-friendly, actionable, and consistent in tone
- Loading states and feedback MUST be provided for all asynchronous operations
- Navigation patterns MUST be consistent across the application
- Accessibility standards (WCAG 2.1 AA) MUST be met for all UI components
- Responsive design MUST ensure usability across supported device sizes
- User workflows MUST minimize cognitive load and required actions

### VIII. Performance Requirements

All features MUST meet defined performance thresholds.

- API response times MUST be under 200ms for p95 latency (excluding external dependencies)
- Page load times MUST be under 3 seconds on standard network conditions
- Database queries MUST complete within 100ms; complex queries MUST be optimized or paginated
- Memory usage MUST remain within defined limits; memory leaks are blocking defects
- Batch operations MUST support cancellation and progress reporting
- Performance regression tests MUST be part of CI pipeline
- Resource-intensive operations MUST be asynchronous and non-blocking

### IX. Documentation Language

All specifications, plans, and user-facing documentation MUST be written in Traditional Chinese (zh-TW).

- Feature specifications (spec.md) MUST be written in Traditional Chinese
- Implementation plans (plan.md) MUST be written in Traditional Chinese
- User-facing documentation (README, guides, help text) MUST be written in Traditional Chinese
- Code comments and inline documentation MAY be in English for technical clarity
- API documentation and technical contracts MAY use English for interoperability
- Commit messages MAY use either language based on team preference
- This constitution document remains in English as the authoritative governance reference

## Architecture Constraints

- Maximum 4 layers: Presentation, Application, Domain, Infrastructure
- Domain layer MUST have zero external dependencies (except language standard library)
- Cross-cutting concerns (logging, caching, transactions) MUST be implemented via aspects/decorators
- Event-driven communication MUST be used between bounded contexts
- Synchronous calls between services MUST be avoided; use async messaging patterns

## Development Workflow

- All changes MUST go through pull request review
- Minimum 1 approval required before merge; 2 approvals for critical paths
- CI pipeline MUST pass: linting, type checking, all tests, security scan
- Feature branches MUST be rebased on main before merge
- Commits MUST be atomic and follow conventional commit format
- Breaking changes MUST be documented and communicated before release
- All production deployments MUST be reversible within 15 minutes

## Governance

This constitution supersedes all other development practices. All team members MUST:

- Verify compliance with these principles in every code review
- Justify any complexity that appears to violate these principles
- Propose amendments through documented change requests
- Use the constitution as the authoritative reference for architectural decisions

### Amendment Procedure

1. Submit amendment proposal with rationale and impact analysis
2. Review period of minimum 5 business days
3. Approval requires consensus among technical leads
4. Version bump follows semantic versioning:
   - MAJOR: Principle removal or redefinition
   - MINOR: New principle or material expansion
   - PATCH: Clarifications and non-semantic refinements
5. All dependent artifacts MUST be updated upon amendment

### Compliance Review

- Quarterly architecture reviews MUST assess adherence to these principles
- Non-compliance MUST be tracked as technical debt with remediation timeline
- Repeated violations require process improvement actions

**Version**: 1.1.0 | **Ratified**: 2025-12-07 | **Last Amended**: 2025-12-07
