# Raise coverage — `:api` `value` package

## Context

`/raise-coverage :api` (autonomous, pre-approved). Kover is already applied
repo-wide (root `KoverConfig.applyTo` + per-module plugin with
`useJacoco(...)`), so Step 0 is a no-op — no migration.

Module-wide gap (Kover JaCoCo XML): 667 lines / 256 branches across 59
human-written `src/main` files. The `render` package dominates but is
integration-heavy (private helpers reachable only through the full
`SourceFile`/`SourceFileSet` pipeline). The pure proto-extension files are the
tractable, high-quality targets.

Target path: `api/src/main/kotlin/io/spine/tools/compiler/value/`
(`Values.kt` + `Options.kt`).

## Gaps (before)

`Values.kt`
- `singularValue` `when(javaType)` — 11 lines / 7 branches missed
  (only LONG + STRING currently hit; missing INT, FLOAT, DOUBLE, BOOLEAN,
  BYTE_STRING, ENUM, nested MESSAGE).
- `NULL`, `packedTrue`, `packedFalse` public lazy `val`s — accessor + lazy
  lambda lines uncovered.
- `else -> error(...)` arm = non-actionable safety net (unreachable; JavaType
  enum is exhaustive).

`Options.kt` (all remaining gaps are reachable **error paths**)
- `init { check(value.isNotEmpty()) }` — empty-value branch + `optionPath()` +
  `sourceFieldName`/`messageTypeName` lazy delegates (only evaluated on error).
- `parse()` `else -> error(...)` — unexpected-format branch.
- `checkFieldReference` `check(referencedFieldType == sourceFieldType)` —
  type-mismatch branch + error-message lambda (6 lines / 1 branch).

## Plan

1. New test fixture proto `api/src/test/proto/compiler/given/value_samples.proto`
   (`package spine.compiler.given`, `java_package io.spine.tools.compiler.given.value`)
   — message `AllScalars` with every scalar type, an enum field set to a
   non-zero constant, a nested-message field, a repeated field, and a map.
   No Spine field options ⇒ no build-time validation risk.

2. Append to `ValuesSpec.kt` (matching its `@DisplayName`/`@Test` style):
   - "convert a message with all field kinds" → `AllScalars{…}.toValue()`,
     asserting INT/LONG/FLOAT/DOUBLE/BOOLEAN/STRING/BYTE_STRING/ENUM/MESSAGE.
   - "expose well-known packed values" → access `NULL`, `packedTrue`,
     `packedFalse` and assert their content.

3. Append to `OptionsSpec.kt` (matching its `internal`/`@Test` style),
   synthesizing `MinOption` values directly against existing fixtures
   (`DiceRoll`, `NumberGenerated`) — no new option-bearing proto needed:
   - empty value → `IllegalStateException` (empty-value branch + `optionPath`).
   - `"1abc"` (matches none of INTEGER/DOUBLE/field-path regex) → unexpected
     format `error`.
   - reference a differently-typed field (`number` int32 → `range` message) →
     type-mismatch `IllegalStateException`.

## Verify
- Re-run `:api:koverXmlReport`; confirm the listed lines/branches drop and the
  module total does not regress vs `.codecov.yml`.
- Bump `version.gradle.kts` (versioned repo; CI gate).

## Non-actionable (reported, not tested)
- `Values.kt` `singularValue` `else -> error(...)` — unreachable enum safety net.
