/***************************************************
 * Licensed under MIT No Attribution (SPDX: MIT-0) *
 ***************************************************/

package org.reactivestreams.tck.flow.junit.jupiter;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.reactivestreams.FlowAdapters;
import org.reactivestreams.example.unicast.AsyncSubscriber;
import org.reactivestreams.tck.junit.jupiter.TestEnvironment;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;

/**
 * Self-test of {@link FlowSubscriberBlackboxVerification} driven by upstream's example
 * {@link AsyncSubscriber} wrapped through {@link FlowAdapters#toFlowSubscriber}.
 */
class AsyncSubscriberFlowBlackboxTest extends FlowSubscriberBlackboxVerification<Integer> {

  private ExecutorService subscriberExecutor;

  AsyncSubscriberFlowBlackboxTest() {
    super(new TestEnvironment());
  }

  @BeforeAll
  void startSubscriberExecutor() {
    subscriberExecutor = Executors.newCachedThreadPool();
  }

  @AfterAll
  void shutdownSubscriberExecutor() {
    if (subscriberExecutor != null) subscriberExecutor.shutdown();
  }

  @Override
  public Flow.Subscriber<Integer> createFlowSubscriber() {
    return FlowAdapters.toFlowSubscriber(new AsyncSubscriber<Integer>(subscriberExecutor) {
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
