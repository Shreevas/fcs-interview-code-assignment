package com.fulfilment.application.monolith.stores.events;

import com.fulfilment.application.monolith.stores.LegacyStoreManagerGateway;
import com.fulfilment.application.monolith.stores.Store;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/**
 * Propagates {@code Store} changes to the legacy system only after the owning transaction has
 * committed successfully, guaranteeing the legacy system never receives data that wasn't actually
 * persisted.
 */
@ApplicationScoped
public class StoreLegacySyncListener {

  private static final Logger LOGGER = Logger.getLogger(StoreLegacySyncListener.class);

  @Inject LegacyStoreManagerGateway legacyStoreManagerGateway;

  public void onStoreChanged(@Observes(during = TransactionPhase.AFTER_SUCCESS) StoreChangedEvent event) {
    StoreSnapshot snapshot = event.snapshot();
    Store store = new Store(snapshot.name());
    store.id = snapshot.id();
    store.quantityProductsInStock = snapshot.quantityProductsInStock();

    try {
      switch (event.changeType()) {
        case CREATED -> legacyStoreManagerGateway.createStoreOnLegacySystem(store);
        case UPDATED -> legacyStoreManagerGateway.updateStoreOnLegacySystem(store);
      }
    } catch (Exception e) {
      // The database commit has already happened by this point and cannot be rolled back here,
      // so a downstream legacy-sync failure must not surface as an error to the original caller.
      LOGGER.errorf(e, "Failed to propagate store change (%s) to legacy system", event.changeType());
    }
  }
}