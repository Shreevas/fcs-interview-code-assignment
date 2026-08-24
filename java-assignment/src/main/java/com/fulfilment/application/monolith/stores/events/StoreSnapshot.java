package com.fulfilment.application.monolith.stores.events;

/**
 * Immutable snapshot of a {@code Store}'s state at the moment a change was committed. Used as the
 * event payload instead of the live Panache entity so it stays valid to read after the request's
 * persistence context has ended.
 */
public record StoreSnapshot(Long id, String name, int quantityProductsInStock) {}