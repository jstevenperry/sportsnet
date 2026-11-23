/*
 * Copyright © 2021 J. Steven Perry
 * All rights reserved.
 */

package com.jstevenperry.sportsnet.framework.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A simple cache.
 *
 * @param <K> The cache key
 * @param <V> The cache value (the "thing" the cache stores)
 */
@SuppressWarnings({"UnusedReturnValue", "unused"})
public class SimpleCache<K, V> {

    public static final String ERROR_PROLOGUE = "CORESC";

    private static final Logger log = LoggerFactory.getLogger(SimpleCache.class);

    /**
     * Used to control the cache behavior
     */
    private final AbstractNetworkProperties networkProperties;

    /**
     * The cache. To borrow the words of the Merovingian: "You see, he's just a Map."
     */
    private final Map<K, CacheItem<V>> cache = new ConcurrentHashMap<>();

    public SimpleCache(final AbstractNetworkProperties networkProperties) {
        this.networkProperties = networkProperties;
    }

    /**
     * Searches the cache for the specified item by its key and if found, returns
     * an Optional containing the item. If the item was not in the cache the Optional
     * will be empty. If the item was present but expired, the object will be evicted
     * from the cache and the Optional will be empty.
     *
     * @param cacheKey The item's key
     * @return As described above.
     */
    public Optional<V> fetch(final K cacheKey) {
        Optional<V> itemOptional = Optional.empty();
        CacheItem<V> cacheItem = cache.get(cacheKey);
        if (cacheItem != null) {
            if (cacheItem.isLive()) {
                if (log.isDebugEnabled()) {
                    log.debug("fetch({}): Found item in cache", cacheKey);
                }
                itemOptional = Optional.of(cacheItem.getItem());
            } else {
                evict(cacheKey);
            }
        } else if (log.isDebugEnabled()) {
            log.debug("fetch({}): Item not found in cache.", cacheKey);
        }
        return itemOptional;
    }

    /**
     * Stores the specified item in the cache.
     *
     * @param cacheKey The item key
     * @param item     The item to cache
     */
    public void store(final K cacheKey, final V item) {
        if (networkProperties != null) {
            //
            // Fetch the TTL from the system config
            long itemTtl = networkProperties.getSimpleCacheTtl();
            //
            // Store the item
            cache.put(cacheKey, new CacheItem<>(item, itemTtl));
            if (log.isDebugEnabled()) {
                log.debug("store({}): Stored item in cache: ttl (minutes): {}", cacheKey, itemTtl);
            }
        } else {
            cache.put(cacheKey, new CacheItem<>(item));
            if (log.isDebugEnabled()) {
                log.debug("store({}): Stored item in cache: ttl (ms): {}", cacheKey, CacheItem.DEFAULT_EXPIRATION);
            }
        }
    }

    /**
     * Uses the calling thread to cleanup any expired items in the cache.
     */
    public void cleanup() {
        cache.keySet().forEach(cacheKey -> {
            if (!cache.get(cacheKey).isLive()) {
                evict(cacheKey);
            }
        });
    }

    /**
     * Returns the total size of the cache, including any expired items
     */
    public int size() {
        return cache.keySet().size();
    }

    /**
     * Immediately expires an item, so it will be subjection to eviction according
     * to whatever policy this class decides to implement (including immediate eviction).
     *
     * The expired item is returned to the caller.
     */
    public V expire(final K cacheKey) {
        //
        // You're done
        return evict(cacheKey).getItem();
    }

    /**
     * Returns the cache keys. Allows callers to work with items in the
     * cache without necessarily knowing what's in there a-priori.
     * Use with caution.
     */
    public Set<K> keys() {
        return cache.keySet();
    }

    /**
     * Evicts the item from the cache.
     *
     * @param cacheKey The cache key
     */
    protected CacheItem<V> evict(final K cacheKey) {
        //
        // Make sure the item exists
        if (cache.get(cacheKey) != null) {
            //
            // Remove the item from the cache
            CacheItem<V> cacheItem = cache.remove(cacheKey);

            if (log.isDebugEnabled()) {
                log.debug("evict({}): Evicted item from cache", cacheKey);
            }
            return cacheItem;
        }
        String exceptionMessage = String.format("%s0115E: No item found matching key: %s", ERROR_PROLOGUE, cacheKey);
        log.error("evict({}): {}", cacheKey, exceptionMessage);
        throw new IllegalArgumentException(exceptionMessage);
    }

    /**
     * A simple cache item
     * @param <T>
     */
    public static class CacheItem<T> {

        /**
         * The default expiration: 10 minutes
         */
        public static final int DEFAULT_EXPIRATION = 10 /*min*/ * 60 /*sec/min*/ * 1000 /*millis/sec*/;

        private final T item;
        private final long whenExpires;

        public CacheItem(T item) {
            this(item, DEFAULT_EXPIRATION);
        }

        public CacheItem(T item, long minutesToExpiration) {
            this.item = item;
            whenExpires = System.currentTimeMillis() + (minutesToExpiration * 60 * 1000);
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > whenExpires;
        }

        public T getItem() {
            return item;
        }

        public boolean isLive() {
            return !isExpired();
        }

        @Override
        public String toString() {
            return "CacheItem{" +
                    "item=" + item +
                    ", whenExpires=" + whenExpires +
                    '}';
        }
    }
}
