# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this repository is

A JUnit Jupiter port of the **Reactive Streams JVM TCK**. The upstream TCK (`org.reactivestreams:reactive-streams-tck`) is TestNG-based; this project provides equivalent abstract `*Verification` base classes annotated for JUnit Jupiter so projects on a Jupiter-only test stack can verify their `Publisher`/`Subscriber`/`Processor` implementations against the Reactive Streams spec without dragging TestNG into the build.

**This is a full fork**: the 6 TestNG-coupled upstream classes are mechanically transformed to Jupiter (in our `org.reactivestreams.tck.junit.jupiter` package), and the 10 TestNG-free upstream support classes are inlined verbatim under their original packages (`org.reactivestreams.tck.flow.support.*`, plus `org.reactivestreams.example.unicast.AsyncIterablePublisher`). The resulting jar has **zero third-party runtime dependencies** — consumers declare `reactive-streams` and JUnit Jupiter themselves at `provided`/`test` scope.

The companion analysis document at the *root of the upstream working copy* (`reactive-streams-jvm/_JUPITER_TRANSITION.asciidoc`) explains in detail what needs to be ported, the five mechanical transformations, and the behavioral gotchas. Read that file before starting any porting work.

## Modules

| Module dir | artifactId | Mirrors upstream |
|---|---|---|
| `reactive-streams-tck-junit-jupiter/` | `reactive-streams-tck-junit-jupiter` | `org.reactivestreams:reactive-streams-tck` |
| `reactive-streams-tck-flow-junit-jupiter/` | `reactive-streams-tck-flow-junit-jupiter` | `org.reactivestreams:reactive-streams-tck-flow` |

Parent: `io.github.jinahya:reactive-streams-jvm-tck-junit-jupiter` (packaging=pom), inheriting from `io.github.jinahya:jinahya-parent:1.0.8`.

`reactive-streams-tck-flow-junit-jupiter` depends on `reactive-streams-tck-junit-jupiter` — the Flow variants are thin subclasses that wrap `Flow.Publisher` via `FlowAdapters` and delegate to the Reactive Streams TCK verification classes.

## Build commands

```bash
mvn install                                      # build + install both modules
mvn -pl reactive-streams-tck-junit-jupiter install                # one module
mvn -pl reactive-streams-tck-junit-jupiter test                   # run that module's tests
mvn -pl reactive-streams-tck-junit-jupiter -Dtest=ClassName test  # single Jupiter test class
mvn -pl reactive-streams-tck-junit-jupiter -Dtest='ClassName#methodName' test
```

The parent pom is `pom.xml` at the repo root. Maven Surefire 3.5.6+ discovers Jupiter tests automatically — no `<provider>` config required.

## Java floor

**Java 17.** Deliberately lower than the `jinahya-parent` style (which targets Java 25 with preview features). Rationale: this is a *library* consumed by Reactive Streams implementers — its compile floor becomes their compile floor. Java 17 is the minimum supported by JUnit 6, which is the floor enforced upstream by `junit-bom:6.1.0`. Do not raise this to 21/25 without a strong reason.

Preview features are *not* enabled. Don't add `--enable-preview` to the compiler or javadoc plugin configs.

## Dependency strategy (the key architectural decision)

**The jar is self-contained — zero runtime dependencies on `reactive-streams-tck` or `reactive-streams-examples`.** The 10 TestNG-free upstream classes that we used to consume transitively are now inlined into our jar at their original packages:

- `org.reactivestreams.tck.flow.support.PublisherVerificationRules` (interface, 38 method decls)
- `org.reactivestreams.tck.flow.support.SubscriberWhiteboxVerificationRules` (interface, 27 method decls)
- `org.reactivestreams.tck.flow.support.SubscriberBlackboxVerificationRules` (interface, 26 method decls)
- `org.reactivestreams.tck.flow.support.{HelperPublisher, InfiniteHelperPublisher, NonFatal, Optional, Function, TestException, SubscriberBufferOverflowException}`
- `org.reactivestreams.example.unicast.AsyncIterablePublisher` (from upstream `reactive-streams-examples`; `HelperPublisher` extends it)

These have zero TestNG references and are copied verbatim from upstream `reactive-streams-jvm:1.0.4` under MIT-0 license. The rules interfaces are the static drift detector — our Jupiter verification classes `implements` them, so if upstream's interfaces ever evolve, a yearly `git diff` against upstream `master` is the maintenance signal (we no longer get a compile break automatically, because we've frozen our copy of the interfaces).

**What we transformed (TestNG → JUnit Jupiter mechanical port):**

- `org.reactivestreams.tck.junit.jupiter.TestEnvironment` (forked from upstream because it has TestNG static imports)
- `org.reactivestreams.tck.junit.jupiter.PublisherVerification`
- `org.reactivestreams.tck.junit.jupiter.SubscriberWhiteboxVerification`
- `org.reactivestreams.tck.junit.jupiter.SubscriberBlackboxVerification`
- `org.reactivestreams.tck.junit.jupiter.IdentityProcessorVerification`
- `org.reactivestreams.tck.junit.jupiter.WithHelperPublisher` (forked because it references the upstream `TestEnvironment`)
- The four `reactive-streams-tck-flow-junit-jupiter` variants (thin adapters over our Jupiter base classes)

**Split-package caveat:** if a consumer also depends on `org.reactivestreams:reactive-streams-tck:1.0.4` (e.g. for cross-validation against TestNG), the `org.reactivestreams.tck.flow.support.*` package is split across two jars. Plain classpath usage works fine (one class definition wins; both are byte-equivalent at 1.0.4). JPMS/module path would error, but this project does not declare a `module-info.java`, so JPMS is opt-in.

## Porting rules (what to do when adding the verification classes)

The five mechanical transformations, in increasing risk order:

1. Annotation package swap: `org.testng.annotations.{Test,BeforeMethod,BeforeClass,AfterClass}` → `org.junit.jupiter.api.{Test,BeforeEach,BeforeAll,AfterAll}`.
2. Skip mechanism: `throw new SkipException(msg)` → `org.opentest4j.TestAbortedException` (use `org.junit.jupiter.api.Assumptions.abort(msg)`).
3. Static import swap: `org.testng.Assert.{fail,assertTrue,assertEquals}` → `org.junit.jupiter.api.Assertions.{fail,assertTrue,assertEquals}`.
4. **`assertEquals` argument order is FLIPPED.** TestNG: `(actual, expected, message)`. Jupiter: `(expected, actual, message)`. Wrong swap makes failure messages lie — audit every call site.
5. `@BeforeAll`/`@AfterAll` lifecycle: upstream uses instance methods (`startPublisherExecutorService()`/`shutdownPublisherExecutorService()` in the Subscriber verifications, touching the `publisherExecutor` instance field). Jupiter `@BeforeAll`/`@AfterAll` require `static` methods *unless* the class is annotated `@TestInstance(Lifecycle.PER_CLASS)`. **Use `@TestInstance(Lifecycle.PER_CLASS)` to preserve original semantics exactly.**

What *not* to do during the port:

- Don't convert to `@ParameterizedTest`, `@Nested`, or `@DisplayName`. Don't refactor `flop*` async-error machinery. Don't add `Awaitility`. Mechanical port only — every "improvement" risks subtle TCK semantic drift.
- Don't change the `TYPE_specNNN_DESC` test-method naming convention (e.g. `required_spec101_subscriptionRequestMustResultInTheCorrectNumberOfProducedElements`). It encodes spec-rule-to-test mapping and is enforced by the upstream `*VerificationRules` interfaces we `implements`.

## Cross-validation (the only real correctness check)

The mechanical port can pass `mvn install` and still differ subtly from upstream in behavior. The only defense is to run the same downstream consumer subclasses (e.g. `RangePublisher`) against *both* TCKs and assert identical pass/skip/fail results per rule. If you change anything in a verification class, run cross-validation before pushing.

Sketch: a test class that extends both `org.reactivestreams.tck.PublisherVerification` (TestNG) and reflectively invokes our `org.reactivestreams.tck.junit.jupiter.PublisherVerification`, then diffs the per-method results. Build this once and wire it into CI.

## Source layout and package naming

```
reactive-streams-tck-junit-jupiter/src/main/java/org/reactivestreams/tck/junit/jupiter/
reactive-streams-tck-flow-junit-jupiter/src/main/java/org/reactivestreams/tck/flow/junit/jupiter/
```

Packages are deliberately distinct from upstream (`org.reactivestreams.tck.junit.jupiter` vs upstream's `org.reactivestreams.tck`) so a consumer can import both TCKs simultaneously for cross-validation without name clashes.

## Upstream tracking

The upstream `reactive-streams-jvm` repo released `1.0.4` in 2019 and is in maintenance mode (essentially frozen). The `*VerificationRules` interfaces — our drift detector — haven't grown in years. Realistic maintenance: a yearly `git diff` against upstream `master` over the 5 TestNG-coupled files. The full inventory of what's coupled and what isn't is in `_JUPITER_TRANSITION.asciidoc` in the upstream working copy.
