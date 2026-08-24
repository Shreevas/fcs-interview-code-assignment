package com.fulfilment.application.monolith.stores.events;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;

import com.fulfilment.application.monolith.stores.LegacyStoreManagerGateway;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

/**
 * Regression coverage for the core guarantee behind the CDI-transactional-event fix: the legacy
 * system must never be contacted for a change whose transaction did not actually commit.
 */
@QuarkusTest
public class StoreEventTransactionalityTest {

  @Inject TransactionalEventFirer eventFirer;

  @InjectMock LegacyStoreManagerGateway legacyStoreManagerGateway;

  @Test
  public void testLegacyGatewayNotInvokedWhenTransactionRollsBack() {
    var event = new StoreChangedEvent(StoreChangeType.CREATED, new StoreSnapshot(1L, "ROLLED-BACK", 1));

    assertThrows(IllegalStateException.class, () -> eventFirer.fireThenRollback(event));

    verifyNoInteractions(legacyStoreManagerGateway);
  }
}