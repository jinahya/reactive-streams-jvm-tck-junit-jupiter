/***************************************************
 * Licensed under MIT No Attribution (SPDX: MIT-0) *
 ***************************************************/

package org.reactivestreams.tck.junit.jupiter;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.reactivestreams.Subscriber;
import org.reactivestreams.example.unicast.AsyncSubscriber;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Self-test of {@link SubscriberBlackboxVerification} driven by upstream's example
 * {@link AsyncSubscriber}, which dispatches signals onto an external executor.
 */
class AsyncSubscriberBlackboxTest extends SubscriberBlackboxVerification<Integer> {

  private ExecutorService subscriberExecutor;

  AsyncSubscriberBlackboxTest() {
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
  public Subscriber<Integer> createSubscriber() {
    return new AsyncSubscriber<Integer>(subscriberExecutor) {
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
