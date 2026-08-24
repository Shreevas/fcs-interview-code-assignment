package com.fulfilment.application.monolith.stores.events;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/**
 * Test-only helper: fires a {@link StoreChangedEvent} inside a transaction that is then rolled
 * back, so tests can assert the {@code AFTER_SUCCESS} listener is never invoked on rollback.
 */
@ApplicationScoped
public class TransactionalEventFirer {

  @Inject Event<StoreChangedEvent> storeChangedEvent;

  @Transactional
  public void fireThenRollback(StoreChangedEvent event) {
    storeChangedEvent.fire(event);
    throw new IllegalStateException("forced rollback for test purposes");
  }
}