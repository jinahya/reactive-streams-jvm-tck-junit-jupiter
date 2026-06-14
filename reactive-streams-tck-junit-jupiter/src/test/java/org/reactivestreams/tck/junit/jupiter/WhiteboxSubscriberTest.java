/***************************************************
 * Licensed under MIT No Attribution (SPDX: MIT-0) *
 ***************************************************/

package org.reactivestreams.tck.junit.jupiter;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * Self-test of {@link SubscriberWhiteboxVerification} driven by a minimal probe-aware
 * Subscriber. Upstream's example Subscribers (SyncSubscriber, AsyncSubscriber) cannot
 * be used here because the whitebox verification model requires the test framework
 * to control {@code request()} / {@code cancel()} timing via the probe's puppet —
 * the example subscribers manage demand autonomously, which is incompatible.
 */
class WhiteboxSubscriberTest extends SubscriberWhiteboxVerification<Integer> {

  WhiteboxSubscriberTest() {
    super(new TestEnvironment());
  }

  @Override
  public Subscriber<Integer> createSubscriber(final WhiteboxSubscriberProbe<Integer> probe) {
    return new Subscriber<Integer>() {
      private Subscription subscription;

      @Override
      public void onSubscribe(final Subscription s) {
        if (s == null) throw new NullPointerException("rule 2.13: onSubscribe MUST NPE on null");
        if (subscription != null) {
          s.cancel(); // rule 2.5: cancel the second subscription if we already have one
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
