# Project: Spine Compiler

## Overview

Spine Compiler is a collection of tools for generating quality domain models
from Protobuf definitions. It is part of the Spine SDK organisation and powers
code generation across Spine-based projects, turning `.proto` files into rich
domain types with validation, factories, and other model conveniences.

## Architecture

**Role**: Library + Gradle plugin + Protobuf compiler plugin.

The repo is a multi-module Gradle build (`rootProject.name = "spine-compiler"`)
with these modules:

- `api`, `api-tests` — public compiler API and its tests.
- `backend` — core code-generation engine.
- `params` — parameter/configuration model passed to the compiler.
- `cli` — command-line entry point.
- `protoc-plugin` — `protoc` plugin that invokes the compiler.
- `jvm` — JVM-specific code-generation support.
- `gradle-api`, `gradle-plugin` — Gradle integration. `gradle-plugin` is
  published separately from the rest of the modules.
- `test-env`, `testlib` — shared test fixtures and utilities.

Module artifacts are published under `io.spine.tools` (see
[`dependencies.md`](../dependencies.md) for the published coordinates). Public
API boundaries live in `api` and `gradle-api`; downstream Spine repos depend
on these.

Read [`.agents/guidelines/jvm-project.md`](../.agents/guidelines/jvm-project.md) for build stack, coding style,
tests, and versioning.
