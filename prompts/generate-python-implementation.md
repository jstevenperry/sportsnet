Build a working Python application from the specifications in docs/, using
test-first development. Read all five specs, especially
docs/python-design-spec.md, before changing code.
The Python spec is the implementation profile; the other specs define domain
and compatibility behavior. The intended production deployment is ONE
application that serves both NCAA basketball and MLB, keeping their sport
modules separate. Where the generic spec allows a single-sport deployment,
the Python spec’s combined deployment is intentional.
Create the Python app in a directory isolated from the existing Java app.
Do not overwrite or refactor the Java implementation or edit the specs.
Inspect the Java source when needed to resolve legacy behavior. If a
requirement remains materially ambiguous, report it rather than silently
inventing behavior.
Use test-first development for each feature: add a focused failing test,
confirm the failure is for the expected reason, implement the smallest fix,
then run the targeted and affected suites. Continue through implementation;
do not stop after a plan or scaffold.
Data rules:
- Treat production season data, aliases, participants, templates, model
  archives, and indexes as external deployment inputs.
- Cover documented CSV contracts and parsing behavior with small synthetic
  fixtures. Do not require production datasets to run tests.
- Test training, evaluation, and tournament logic with synthetic data, controlled predictions, and fake backends. Before a tournament request runs inference, validate its configured inputs and fail without partial results if they do not match the declared contract.
  Implement the specified FastAPI/Keras application, persistence, APIs,
  management events, and operations. Preserve the Java-compatible year
  manifests and API contracts. Persist qualifying models only; do not add
  unsaved-network routes, candidate saving, or in-memory network retention.
  Keep generic workflows sport-neutral and test both production modules plus
  the synthetic test-only module.
  Run the documented tests, lint, formatting, and type
  checks. Fix implementation-caused failures. Report completed work, command
  results, and any remaining blockers or unresolved compatibility decisions.