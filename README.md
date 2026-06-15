# reactive-streams-jvm-tck-junit-jupiter

[![Java CI with Maven](https://github.com/jinahya/reactive-streams-jvm-tck-junit-jupiter/actions/workflows/maven.yml/badge.svg)](https://github.com/jinahya/reactive-streams-jvm-tck-junit-jupiter/actions/workflows/maven.yml)

[![Maven Central Version](https://img.shields.io/maven-central/v/io.github.jinahya/reactive-streams-jvm-tck-junit-jupiter)](https://central.sonatype.com/artifact/io.github.jinahya/reactive-streams-jvm-tck-junit-jupiter)
[![reactive-streams-tck-junit-jupiter](https://img.shields.io/maven-central/v/io.github.jinahya/reactive-streams-tck-junit-jupiter?label=reactive-streams-tck-junit-jupiter)](https://central.sonatype.com/artifact/io.github.jinahya/reactive-streams-tck-junit-jupiter)
[![reactive-streams-tck-flow-junit-jupiter](https://img.shields.io/maven-central/v/io.github.jinahya/reactive-streams-tck-flow-junit-jupiter?label=reactive-streams-tck-flow-junit-jupiter)](https://central.sonatype.com/artifact/io.github.jinahya/reactive-streams-tck-flow-junit-jupiter)


A JUnit Jupiter port of the [Reactive Streams JVM TCK](https://github.com/reactive-streams/reactive-streams-jvm/tree/master/tck).

The upstream TCK is TestNG-based. This project provides equivalent abstract `*Verification` base classes annotated for JUnit Jupiter, so projects on a Jupiter-only test stack can verify their `Publisher` / `Subscriber` / `Processor` implementations against the Reactive Streams spec without dragging TestNG into the build.

The jar is self-contained: we mechanically ported the 6 TestNG-coupled upstream classes to JUnit Jupiter (`TestEnvironment`, `WithHelperPublisher`, and the four `*Verification` classes), and we inlined the 10 TestNG-free upstream support classes (rules interfaces, helper publishers, support utilities) plus `AsyncIterablePublisher` from `reactive-streams-examples`, all preserved at their original packages. There is no transitive dependency on `reactive-streams-tck` or `reactive-streams-examples`. Spec semantics, method names (`required_specNNN_…`, `optional_specNNN_…`, `untested_specNNN_…`), and rule-to-test mappings are preserved verbatim.

## Modules

| Module dir | artifactId | Mirrors upstream |
|---|---|---|
| `reactive-streams-tck-junit-jupiter/` | `reactive-streams-tck-junit-jupiter` | `org.reactivestreams:reactive-streams-tck` |
| `reactive-streams-tck-flow-junit-jupiter/` | `reactive-streams-tck-flow-junit-jupiter` | `org.reactivestreams:reactive-streams-tck-flow` |

`reactive-streams-tck-flow-junit-jupiter` is a thin subclass layer over `reactive-streams-tck-junit-jupiter` that adapts `java.util.concurrent.Flow` types via `FlowAdapters`.

## Requirements

- Java 17+ (the floor of JUnit 6, which we depend on)
- A JUnit Jupiter test runtime in the consuming project (Maven Surefire 3.0+ or Gradle 5.0+ discovers Jupiter tests automatically)

## Maven coordinates

For Reactive Streams `org.reactivestreams.Publisher` / `Subscriber` / `Processor` implementations:

```xml
<dependency>
  <groupId>io.github.jinahya</groupId>
  <artifactId>reactive-streams-tck-junit-jupiter</artifactId>
  <version>0.0.2-SNAPSHOT</version>
  <scope>test</scope>
</dependency>
```

For `java.util.concurrent.Flow` implementations (transitively pulls in the above):

```xml
<dependency>
  <groupId>io.github.jinahya</groupId>
  <artifactId>reactive-streams-tck-flow-junit-jupiter</artifactId>
  <version>0.0.2-SNAPSHOT</version>
  <scope>test</scope>
</dependency>
```

`reactive-streams` and `junit-jupiter-api` are `provided` on our side — neither propagates transitively. You're expected to declare them yourself, in the scope that fits your project:

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>org.junit</groupId>
      <artifactId>junit-bom</artifactId>
      <version>6.1.0</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>

<dependencies>
  <!--
    The spec API. Library authors implementing a Publisher/Subscriber/Processor
    already have this declared (usually at compile). For test-only consumers,
    declare it at test scope.
  -->
  <dependency>
    <groupId>org.reactivestreams</groupId>
    <artifactId>reactive-streams</artifactId>
    <version>1.0.4</version>
  </dependency>

  <!-- Jupiter API + engine. -->
  <dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter-api</artifactId>
    <scope>test</scope>
  </dependency>
  <dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter-engine</artifactId>
    <scope>test</scope>
  </dependency>
</dependencies>
```

Why `provided` on our side? You already have these — `reactive-streams` because you're testing a `Publisher`, Jupiter because that's your test framework. Propagating our versions transitively would only fight your own pinning (and break the inherited `dependencyConvergence` enforcer in our parent pom if versions mismatched). Letting you declare them keeps you in control.

## Gradle coordinates

```groovy
testImplementation 'io.github.jinahya:reactive-streams-tck-junit-jupiter:0.0.2-SNAPSHOT'
// or, for Flow:
testImplementation 'io.github.jinahya:reactive-streams-tck-flow-junit-jupiter:0.0.2-SNAPSHOT'

// The spec API — library authors usually declare this at implementation/api scope already.
testImplementation 'org.reactivestreams:reactive-streams:1.0.4'

testImplementation platform('org.junit:junit-bom:6.1.0')
testImplementation 'org.junit.jupiter:junit-jupiter-api'
testRuntimeOnly    'org.junit.jupiter:junit-jupiter-engine'
```

## Usage

Same shape as the upstream TestNG TCK: extend the abstract verification class for the type you want to verify, pass a `TestEnvironment` to the super constructor, and implement the factory method(s).

### Verifying a `Publisher`

```java
import org.reactivestreams.Publisher;
import org.reactivestreams.tck.junit.jupiter.PublisherVerification;
import org.reactivestreams.tck.junit.jupiter.TestEnvironment;

public class MyPublisherTest extends PublisherVerification<Integer> {

  public MyPublisherTest() {
    super(new TestEnvironment());
  }

  @Override
  public Publisher<Integer> createPublisher(long elements) {
    return new MyPublisher(elements);
  }

  @Override
  public Publisher<Integer> createFailedPublisher() {
    // return a Publisher that immediately signals onError to opt into
    // the failed-publisher rules; return null to skip them.
    return null;
  }
}
```

### Verifying a `Subscriber` — blackbox

```java
import org.reactivestreams.Subscriber;
import org.reactivestreams.tck.junit.jupiter.SubscriberBlackboxVerification;
import org.reactivestreams.tck.junit.jupiter.TestEnvironment;

public class MySubscriberTest extends SubscriberBlackboxVerification<Integer> {

  public MySubscriberTest() {
    super(new TestEnvironment());
  }

  @Override
  public Subscriber<Integer> createSubscriber() {
    return new MySubscriber();
  }

  @Override
  public Integer createElement(int element) {
    return element;
  }
}
```

### Verifying a `Subscriber` — whitebox

```java
import org.reactivestreams.Subscriber;
import org.reactivestreams.tck.junit.jupiter.SubscriberWhiteboxVerification;
import org.reactivestreams.tck.junit.jupiter.TestEnvironment;

public class MySubscriberWhiteboxTest extends SubscriberWhiteboxVerification<Integer> {

  public MySubscriberWhiteboxTest() {
    super(new TestEnvironment());
  }

  @Override
  public Subscriber<Integer> createSubscriber(WhiteboxSubscriberProbe<Integer> probe) {
    return new MyInstrumentedSubscriber(probe);
  }

  @Override
  public Integer createElement(int element) {
    return element;
  }
}
```

### Verifying a `Processor`

```java
import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;
import org.reactivestreams.tck.junit.jupiter.IdentityProcessorVerification;
import org.reactivestreams.tck.junit.jupiter.TestEnvironment;

public class MyProcessorTest extends IdentityProcessorVerification<Integer> {

  public MyProcessorTest() {
    super(new TestEnvironment());
  }

  @Override
  public Processor<Integer, Integer> createIdentityProcessor(int bufferSize) {
    return new MyIdentityProcessor(bufferSize);
  }

  @Override
  public Publisher<Integer> createFailedPublisher() { return null; }

  @Override
  public Integer createElement(int element) { return element; }

  @Override
  public long maxSupportedSubscribers() { return 1; }
}
```

### Verifying a `Flow.Publisher` (`java.util.concurrent.Flow`)

```java
import java.util.concurrent.Flow;
import org.reactivestreams.tck.flow.junit.jupiter.FlowPublisherVerification;
import org.reactivestreams.tck.junit.jupiter.TestEnvironment;

public class MyFlowPublisherTest extends FlowPublisherVerification<Integer> {

  public MyFlowPublisherTest() {
    super(new TestEnvironment());
  }

  @Override
  public Flow.Publisher<Integer> createFlowPublisher(long elements) {
    return new MyFlowPublisher(elements);
  }

  @Override
  public Flow.Publisher<Integer> createFailedFlowPublisher() { return null; }
}
```

The Flow side mirrors the Reactive Streams side one-to-one:
`FlowPublisherVerification`, `FlowSubscriberBlackboxVerification`, `FlowSubscriberWhiteboxVerification`, `IdentityFlowProcessorVerification`.

## Running

```bash
mvn test                                              # all
mvn -Dtest=MyPublisherTest test                       # single class
mvn -Dtest='MyPublisherTest#required_spec101_*' test  # single rule
```

The test methods preserve the upstream `required_specNNN_*` / `optional_specNNN_*` / `untested_specNNN_*` naming, so test reports stay comparable to upstream TestNG runs.

## Behavioural notes

- **`@TestInstance(Lifecycle.PER_CLASS)`** is applied to all verification classes to preserve TestNG's per-class instance semantics. Do not override this in your subclass.
- **Skipped tests** use `org.opentest4j.TestAbortedException` (Jupiter's standard skip path) instead of TestNG's `SkipException`. They show as "skipped" in your test report, not as failures.
- **Timeouts** come from the `TestEnvironment(defaultTimeoutMillis, …)` constructor. Bump them for slow CI:
  ```java
  super(new TestEnvironment(500L /* defaultTimeoutMillis */));
  ```
- **Parallel execution** should remain off for these verifications — verification state is per-class and not designed to be method-parallel.

## How this differs from the upstream TCK

| Concern | Upstream | This project |
|---|---|---|
| Test framework | TestNG | JUnit Jupiter |
| Skip mechanism | `org.testng.SkipException` | `org.opentest4j.TestAbortedException` |
| Lifecycle annotations | `@BeforeClass` / `@AfterClass` / `@BeforeMethod` | `@BeforeAll` / `@AfterAll` / `@BeforeEach` (with `@TestInstance(PER_CLASS)`) |
| Assertion order | `assertEquals(actual, expected)` | `assertEquals(expected, actual)` (TCK call sites flipped accordingly) |
| Package | `org.reactivestreams.tck` | `org.reactivestreams.tck.junit.jupiter` |
| Spec rule coverage | All upstream rules | Identical — every `*VerificationRules` interface method has the same body |

The rules interfaces (`PublisherVerificationRules`, `SubscriberWhiteboxVerificationRules`, `SubscriberBlackboxVerificationRules`) are inlined into our jar at their original upstream package (`org.reactivestreams.tck.flow.support`) — same package, same classes, same method contracts. Our verification classes `implements` them. The same is true for the helper publishers (`HelperPublisher`, `InfiniteHelperPublisher`), the support utilities (`Function`, `Optional`, `NonFatal`, `TestException`, `SubscriberBufferOverflowException`), and `AsyncIterablePublisher` (kept at `org.reactivestreams.example.unicast`).

The verification class packages (`org.reactivestreams.tck.junit.jupiter`) are deliberately distinct from upstream's (`org.reactivestreams.tck`) so a consumer can depend on both TCKs simultaneously — for example to cross-validate that the Jupiter port matches TestNG behaviour rule-by-rule — without import conflicts.

> ⚠️ **Note**: if a consumer also explicitly depends on `org.reactivestreams:reactive-streams-tck:1.0.4` (e.g. for cross-validation), the `org.reactivestreams.tck.flow.support.*` package becomes split across two jars. On the classpath this works (one definition wins, both are byte-equivalent). Under JPMS / module path, it would error — but this project does not declare a `module-info.java`, so JPMS is opt-in.

## Building from source

```bash
mvn install                                                      # build + install both modules
mvn -pl reactive-streams-tck-junit-jupiter install                # one module
mvn -pl reactive-streams-tck-junit-jupiter test                   # run that module's tests
```

## License

Apache License, Version 2.0.

The ported source files retain their upstream `Licensed under MIT No Attribution (SPDX: MIT-0)` headers, since they are derivative works of the original Reactive Streams TCK.
