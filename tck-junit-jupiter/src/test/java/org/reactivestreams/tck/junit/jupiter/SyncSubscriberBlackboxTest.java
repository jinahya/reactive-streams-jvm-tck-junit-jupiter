/***************************************************
 * Licensed under MIT No Attribution (SPDX: MIT-0) *
 ***************************************************/

package org.reactivestreams.tck.junit.jupiter;

import org.reactivestreams.Subscriber;
import org.reactivestreams.example.unicast.SyncSubscriber;

/**
 * Self-test of {@link SubscriberBlackboxVerification} driven by upstream's example
 * {@link SyncSubscriber}.
 */
class SyncSubscriberBlackboxTest extends SubscriberBlackboxVerification<Integer> {

  SyncSubscriberBlackboxTest() {
    super(new TestEnvironment());
  }

  @Override
  public Subscriber<Integer> createSubscriber() {
    return new SyncSubscriber<Integer>() {
      @Override
      protected boolean whenNext(final Integer element) {
        return true;
      }
    };
  }

  @Override
  public Integer createElement(final int element) {
    return element;
  }
}
