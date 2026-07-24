# Team memory index

One line per memory. Scan at the start of every session.
See [README.md](README.md) for the format and routing rules.

## Feedback (validated patterns & corrections)

- [copilot-review-request](feedback/copilot-review-request.md) — GraphQL `requestReviews` with `botIds: ["BOT_kgDOCnlnWA"]`; REST endpoint silently no-ops on re-requests.
- [gradle-annotations-pin-exclusion](feedback/gradle-annotations-pin-exclusion.md) — publish plugins without `org.jetbrains:annotations`; never fix the `strictly 13.0` pin with consumer-side forces.

## Project (durable context & rationale)

- [integration tests stale compiler after version bump](project/integration-tests-stale-compiler-after-version-bump.md) — after a version bump, clean-build `integrationTest` or it may launch the old compiler
- [functional-test fixtures](project/functional-test-fixtures.md) — authoring `gradle-plugin` TestKit fixtures: copied buildSrc, import rules, Maven Local prerequisite, `tuneRunner()`

## Reference (external systems)

- [cache-warm-window](reference/cache-warm-window.md) — How prompt cache entries are shared between sibling-repo sessions and how to maximise overlap.
- [anthropic-api-caching](reference/anthropic-api-caching.md) — Pattern and pricing for adding prompt caching to any direct Anthropic API call.
