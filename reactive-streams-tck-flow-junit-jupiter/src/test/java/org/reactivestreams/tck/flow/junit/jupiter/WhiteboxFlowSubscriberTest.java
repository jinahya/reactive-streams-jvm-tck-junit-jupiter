/***************************************************
 * Licensed under MIT No Attribution (SPDX: MIT-0) *
 ***************************************************/

package org.reactivestreams.tck.flow.junit.jupiter;

import org.reactivestreams.tck.junit.jupiter.SubscriberWhiteboxVerification.SubscriberPuppet;
import org.reactivestreams.tck.junit.jupiter.SubscriberWhiteboxVerification.WhiteboxSubscriberProbe;
import org.reactivestreams.tck.junit.jupiter.TestEnvironment;

import java.util.concurrent.Flow;

/**
 * Self-test of {@link FlowSubscriberWhiteboxVerification} driven by a minimal probe-aware
 * {@link Flow.Subscriber}. Mirrors {@code WhiteboxSubscriberTest} in the sibling module.
 */
class WhiteboxFlowSubscriberTest extends FlowSubscriberWhiteboxVerification<Integer> {

  WhiteboxFlowSubscriberTest() {
    super(new TestEnvironment());
  }

  @Override
  protected Flow.Subscriber<Integer> createFlowSubscriber(final WhiteboxSubscriberProbe<Integer> probe) {
    return new Flow.Subscriber<Integer>() {
      private Flow.Subscription subscription;

      @Override
      public void onSubscribe(final Flow.Subscription s) {
        if (s == null) throw new NullPointerException("rule 2.13: onSubscribe MUST NPE on null");
        if (subscription != null) {
          s.cancel(); // rule 2.5
          return;
        }
        subscription = s;
        probe.registerOnSubscribe(new SubscriberPuppet() {
          @Override public void triggerRequest(final long elements) { s.request(elements); }
          @Override public void signalCancel() { s.cancel(); }
        });
      }
      @Override public void onNext(final Integer element) {
        if (element == null) throw new NullPointerException("rule 2.13: onNext MUST NPE on null");
        probe.registerOnNext(element);
      }
      @Override public void onError(final Throwable t) {
        if (t == null) throw new NullPointerException("rule 2.13: onError MUST NPE on null");
        probe.registerOnError(t);
      }
      @Override public void onComplete() { probe.registerOnComplete(); }
    };
  }

  @Override
  public Integer createElement(final int element) {
    return element;
  }
}
