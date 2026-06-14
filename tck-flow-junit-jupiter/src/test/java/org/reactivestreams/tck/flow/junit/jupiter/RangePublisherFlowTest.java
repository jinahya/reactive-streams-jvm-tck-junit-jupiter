/***************************************************
 * Licensed under MIT No Attribution (SPDX: MIT-0) *
 ***************************************************/

package org.reactivestreams.tck.flow.junit.jupiter;

import org.reactivestreams.FlowAdapters;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.reactivestreams.example.unicast.RangePublisher;
import org.reactivestreams.tck.junit.jupiter.TestEnvironment;

import java.util.concurrent.Flow;

/**
 * Self-test of {@link FlowPublisherVerification} driven by upstream's example
 * {@link RangePublisher} wrapped through {@link FlowAdapters#toFlowPublisher}.
 * Mirrors {@code RangePublisherTest} in the sibling module — together they
 * exercise both ends of the Flow / Reactive Streams adapter bridge.
 */
class RangePublisherFlowTest extends FlowPublisherVerification<Integer> {

  RangePublisherFlowTest() {
    super(new TestEnvironment());
  }

  @Override
  public Flow.Publisher<Integer> createFlowPublisher(final long elements) {
    // start = 0 keeps `start + count` from overflowing at the Integer.MAX_VALUE boundary.
    return FlowAdapters.toFlowPublisher(new RangePublisher(0, (int) elements));
  }

  @Override
  public Flow.Publisher<Integer> createFailedFlowPublisher() {
    // Same minimal failed Publisher as the sibling RangePublisherTest, adapted to Flow.
    final Publisher<Integer> rs = new Publisher<Integer>() {
      @Override
      public void subscribe(final Subscriber<? super Integer> s) {
        s.onSubscribe(new Subscription() {
          @Override public void request(final long n) {}
          @Override public void cancel() {}
        });
        s.onError(new RuntimeException("Can't subscribe subscriber: " + s + ", because of reasons."));
      }
    };
    return FlowAdapters.toFlowPublisher(rs);
  }

  @Override
  public long maxElementsFromPublisher() {
    return Integer.MAX_VALUE;
  }
}
