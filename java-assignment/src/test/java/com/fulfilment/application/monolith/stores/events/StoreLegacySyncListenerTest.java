package com.fulfilment.application.monolith.stores.events;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.fulfilment.application.monolith.stores.LegacyStoreManagerGateway;
import com.fulfilment.application.monolith.stores.Store;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class StoreLegacySyncListenerTest {

  @Mock LegacyStoreManagerGateway legacyStoreManagerGateway;

  private StoreLegacySyncListener listener;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    listener = new StoreLegacySyncListener();
    listener.legacyStoreManagerGateway = legacyStoreManagerGateway;
  }

  @Test
  public void testOnStoreChangedDispatchesCreateForCreatedEvent() {
    var event = new StoreChangedEvent(StoreChangeType.CREATED, new StoreSnapshot(1L, "TONSTAD", 10));

    listener.onStoreChanged(event);

    verify(legacyStoreManagerGateway, times(1)).createStoreOnLegacySystem(any(Store.class));
    verify(legacyStoreManagerGateway, never()).updateStoreOnLegacySystem(any(Store.class));
  }

  @Test
  public void testOnStoreChangedDispatchesUpdateForUpdatedEvent() {
    var event = new StoreChangedEvent(StoreChangeType.UPDATED, new StoreSnapshot(1L, "TONSTAD", 10));

    listener.onStoreChanged(event);

    verify(legacyStoreManagerGateway, times(1)).updateStoreOnLegacySystem(any(Store.class));
    verify(legacyStoreManagerGateway, never()).createStoreOnLegacySystem(any(Store.class));
  }

  @Test
  public void testOnStoreChangedSwallowsGatewayFailure() {
    doThrow(new RuntimeException("legacy system unavailable"))
        .when(legacyStoreManagerGateway)
        .createStoreOnLegacySystem(any(Store.class));

    var event = new StoreChangedEvent(StoreChangeType.CREATED, new StoreSnapshot(1L, "TONSTAD", 10));

    // must not propagate - the DB commit already happened, the caller should not see a failure
    listener.onStoreChanged(event);
  }
}