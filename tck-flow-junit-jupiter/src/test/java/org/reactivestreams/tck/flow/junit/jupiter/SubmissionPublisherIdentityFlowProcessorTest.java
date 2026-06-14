/***************************************************
 * Licensed under MIT No Attribution (SPDX: MIT-0) *
 ***************************************************/

package org.reactivestreams.tck.flow.junit.jupiter;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.reactivestreams.FlowAdapters;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.reactivestreams.tck.junit.jupiter.TestEnvironment;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;

/**
 * Self-test of {@link IdentityFlowProcessorVerification} using {@link SubmissionPublisher}
 * from the JDK composed with a small {@link Flow.Subscriber} half to form an identity
 * {@link Flow.Processor}. Upstream's reactive-streams-examples ships no sample Processor,
 * so this is the lightest-touch JDK-only subject we can drive the verification with.
 *
 * <p>Note: {@code SubmissionPublisher} is not specifically engineered to satisfy every
 * rule of {@code IdentityProcessorVerification}; some skips here may be intrinsic to the
 * subject rather than the TCK port.
 */
class SubmissionPublisherIdentityFlowProcessorTest extends IdentityFlowProcessorVerification<Integer> {

  private ExecutorService executor;

  SubmissionPublisherIdentityFlowProcessorTest() {
    super(new TestEnvironment());
  }

  @BeforeAll
  void startExecutor() {
    executor = Executors.newCachedThreadPool();
  }

  @AfterAll
  void shutdownExecutor() {
    if (executor != null) executor.shutdown();
  }

  @Override
  public ExecutorService publisherExecutorService() {
    return executor;
  }

  @Override
  public Integer createElement(final int element) {
    return element;
  }

  @Override
  protected Flow.Processor<Integer, Integer> createIdentityFlowProcessor(final int bufferSize) {
    return new IdentitySubmissionProcessor<Integer>(executor, bufferSize);
  }

  @Override
  protected Flow.Publisher<Integer> createFailedFlowPublisher() {
    return FlowAdapters.toFlowPublisher(new org.reactivestreams.Publisher<Integer>() {
      @Override
      public void subscribe(final Subscriber<? super Integer> s) {
        s.onSubscribe(new Subscription() {
          @Override public void request(final long n) {}
          @Override public void cancel() {}
        });
        s.onError(new RuntimeException("Can't subscribe subscriber: " + s + ", because of reasons."));
      }
    });
  }

  static final class IdentitySubmissionProcessor<T> extends SubmissionPublisher<T>
      implements Flow.Processor<T, T> {

    private final int bufferSize;
    private volatile Flow.Subscription subscription;

    IdentitySubmissionProcessor(final ExecutorService executor, final int bufferSize) {
      super(executor, bufferSize);
      this.bufferSize = bufferSize;
    }

    @Override
    public void onSubscribe(final Flow.Subscription s) {
      if (s == null) throw new NullPointerException("rule 2.13");
      if (subscription != null) { s.cancel(); return; }
      subscription = s;
      s.request(bufferSize);
    }

    @Override
    public void onNext(final T item) {
      if (item == null) throw new NullPointerException("rule 2.13");
      // Once downstream has cancelled (subscription nulled), drop without re-requesting.
      final Flow.Subscription up = subscription;
      if (up == null) return;
      submit(item);
      up.request(1);
    }

    @Override
    public void onError(final Throwable t) {
      if (t == null) throw new NullPointerException("rule 2.13");
      closeExceptionally(t);
    }

    @Override
    public void onComplete() {
      close();
    }

    // Propagate downstream cancellation to upstream — SubmissionPublisher has no
    // public hook for cancel events, so we intercept subscribe() to wrap the
    // Subscription handed to the downstream subscriber.
    @Override
    public void subscribe(final Flow.Subscriber<? super T> downstream) {
      if (downstream == null) throw new NullPointerException("rule 1.9");
      super.subscribe(new Flow.Subscriber<T>() {
        @Override
        public void onSubscribe(final Flow.Subscription wrapped) {
          downstream.onSubscribe(new Flow.Subscription() {
            @Override public void request(final long n) { wrapped.request(n); }
            @Override public void cancel() {
              wrapped.cancel();
              final Flow.Subscription up = subscription;
              subscription = null; // make onNext a no-op afterwards
              if (up != null) up.cancel();
            }
          });
        }
        @Override public void onNext(final T item) { downstream.onNext(item); }
        @Override public void onError(final Throwable t) { downstream.onError(t); }
        @Override public void onComplete() { downstream.onComplete(); }
      });
    }
  }
}
