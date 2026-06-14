/***************************************************
 * Licensed under MIT No Attribution (SPDX: MIT-0) *
 ***************************************************/

package org.reactivestreams.tck.junit.jupiter;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.reactivestreams.example.unicast.RangePublisher;

/**
 * Self-test of {@link PublisherVerification} driven by upstream's example
 * {@link RangePublisher}. Any failure here indicates a regression in the
 * mechanical TestNG to Jupiter port, not in the subject under test.
 */
class RangePublisherTest extends PublisherVerification<Integer> {

  RangePublisherTest() {
    super(new TestEnvironment());
  }

  @Override
  public Publisher<Integer> createPublisher(final long elements) {
    // start = 0 keeps `start + count` from overflowing when elements == Integer.MAX_VALUE
    // (required for required_spec317_mustNotSignalOnErrorWhenPendingAboveLongMaxValue).
    return new RangePublisher(0, (int) elements);
  }

  @Override
  public Publisher<Integer> createFailedPublisher() {
    // Minimal Publisher that hands out a no-op subscription then immediately fails,
    // so the spec104 / spec109 error-path rules can run.
    return new Publisher<Integer>() {
      @Override
      public void subscribe(final Subscriber<? super Integer> s) {
        s.onSubscribe(new Subscription() {
          @Override public void request(final long n) {}
          @Override public void cancel() {}
        });
        s.onError(new RuntimeException("Can't subscribe subscriber: " + s + ", because of reasons."));
      }
    };
  }

  @Override
  public long maxElementsFromPublisher() {
    return Integer.MAX_VALUE;
  }
}
