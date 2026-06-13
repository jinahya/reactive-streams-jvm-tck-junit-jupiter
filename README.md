# reactive-streams-jvm-tck-junit-jupiter

A JUnit Jupiter port of the [Reactive Streams JVM TCK](https://github.com/reactive-streams/reactive-streams-jvm/tree/master/tck).

The upstream TCK is TestNG-based. This project provides equivalent abstract `*Verification` base classes annotated for JUnit Jupiter, so projects on a Jupiter-only test stack can verify their `Publisher` / `Subscriber` / `Processor` implementations against the Reactive Streams spec without dragging TestNG into the build.

It is *not* a fork of the entire upstream TCK. The TestNG-free pieces (rules interfaces, helper publishers, support utilities) are consumed directly from the upstream `org.reactivestreams:reactive-streams-tck:1.0.4` jar with TestNG excluded transitively; only the TestNG-coupled classes are reimplemented locally. Spec semantics, method names (`required_specNNN_…`, `optional_specNNN_…`, `untested_specNNN_…`), and rule-to-test mappings are preserved verbatim.

## Modules

| Module dir | artifactId | Mirrors upstream |
|---|---|---|
| `tck-junit-jupiter/` | `reactive-streams-tck-junit-jupiter` | `org.reactivestreams:reactive-streams-tck` |
| `tck-flow-junit-jupiter/` | `reactive-streams-tck-flow-junit-jupiter` | `org.reactivestreams:reactive-streams-tck-flow` |

`tck-flow-junit-jupiter` is a thin subclass layer over `tck-junit-jupiter` that adapts `java.util.concurrent.Flow` types via `FlowAdapters`.

## Requirements

- Java 17+ (the floor of JUnit 6, which we depend on)
- A JUnit Jupiter test runtime in the consuming project (Maven Surefire 3.0+ or Gradle 5.0+ discovers Jupiter tests automatically)

## Maven coordinates

For Reactive Streams `org.reactivestreams.Publisher` / `Subscriber` / `Processor` implementations:

```xml
<dependency>
  <groupId>io.github.jinahya</groupId>
  <artifactId>reactive-streams-tck-junit-jupiter</artifactId>
  <version>0.0.1-SNAPSHOT</version>
  <scope>test</scope>
</dependency>
```

For `java.util.concurrent.Flow` implementations (transitively pulls in the above):

```xml
<dependency>
  <groupId>io.github.jinahya</groupId>
  <artifactId>reactive-streams-tck-flow-junit-jupiter</artifactId>
  <version>0.0.1-SNAPSHOT</version>
  <scope>test</scope>
</dependency>
```

`junit-jupiter-api` is a `provided` dependency on our side. Your project already has Jupiter on its test classpath, so we don't propagate a version transitively — you control your own Jupiter version (typically via `junit-bom`):

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

## Gradle coordinates

```groovy
testImplementation 'io.github.jinahya:reactive-streams-tck-junit-jupiter:0.0.1-SNAPSHOT'
// or, for Flow:
testImplementation 'io.github.jinahya:reactive-streams-tck-flow-junit-jupiter:0.0.1-SNAPSHOT'

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

The upstream rules interfaces (`PublisherVerificationRules`, `SubscriberWhiteboxVerificationRules`, `SubscriberBlackboxVerificationRules`) are consumed directly from the upstream jar — our verification classes `implements` them. If upstream ever adds a new rule method, our build breaks until we add a test body. This is the static drift detector.

The packages are deliberately distinct from upstream so a consumer can depend on both TCKs simultaneously (for example, to cross-validate that the Jupiter port matches TestNG behaviour rule-by-rule) without import conflicts.

## Building from source

```bash
mvn install                                      # build + install both modules
mvn -pl tck-junit-jupiter install                # one module
mvn -pl tck-junit-jupiter test                   # run that module's tests
```

## License

Apache License, Version 2.0.

The ported source files retain their upstream `Licensed under MIT No Attribution (SPDX: MIT-0)` headers, since they are derivative works of the original Reactive Streams TCK.
