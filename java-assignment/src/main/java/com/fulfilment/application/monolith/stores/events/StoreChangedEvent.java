package com.fulfilment.application.monolith.stores.events;

public record StoreChangedEvent(StoreChangeType changeType, StoreSnapshot snapshot) {}