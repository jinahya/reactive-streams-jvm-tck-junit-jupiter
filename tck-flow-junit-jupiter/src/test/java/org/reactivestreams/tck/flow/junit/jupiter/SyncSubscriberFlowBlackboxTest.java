/***************************************************
 * Licensed under MIT No Attribution (SPDX: MIT-0) *
 ***************************************************/

package org.reactivestreams.tck.flow.junit.jupiter;

import org.reactivestreams.FlowAdapters;
import org.reactivestreams.example.unicast.SyncSubscriber;
import org.reactivestreams.tck.junit.jupiter.TestEnvironment;

import java.util.concurrent.Flow;

/**
 * Self-test of {@link FlowSubscriberBlackboxVerification} driven by upstream's example
 * {@link SyncSubscriber} wrapped through {@link FlowAdapters#toFlowSubscriber}.
 */
class SyncSubscriberFlowBlackboxTest extends FlowSubscriberBlackboxVerification<Integer> {

  SyncSubscriberFlowBlackboxTest() {
    super(new TestEnvironment());
  }

  @Override
  public Flow.Subscriber<Integer> createFlowSubscriber() {
    return FlowAdapters.toFlowSubscriber(new SyncSubscriber<Integer>() {
      @Override
      protected boolean whenNext(final Integer element) {
        return true;
      }
    });
  }

  @Override
  public Integer createElement(final int element) {
    return element;
  }
}
