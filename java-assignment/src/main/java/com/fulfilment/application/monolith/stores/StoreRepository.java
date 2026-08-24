package com.fulfilment.application.monolith.stores;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Injectable repository for {@link Store}, used where a mockable dependency is needed (e.g. from
 * {@code FulfillmentService}) instead of Panache's static active-record methods.
 */
@ApplicationScoped
public class StoreRepository implements PanacheRepository<Store> {}