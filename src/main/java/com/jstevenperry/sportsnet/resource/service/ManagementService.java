package com.jstevenperry.sportsnet.resource.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.jstevenperry.sportsnet.framework.exception.BadRequestException;
import com.jstevenperry.sportsnet.framework.exception.NotFoundException;
import com.jstevenperry.sportsnet.framework.model.ManagementEvent;
import com.jstevenperry.sportsnet.framework.model.ManagementEventSummary;
import com.jstevenperry.sportsnet.framework.model.ManagementEventType;
import com.jstevenperry.sportsnet.resource.config.ResourceServiceNetworkUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Service
public class ManagementService {

    public static final String LOG_PREFIX = "RSMS";

    private static final Logger log = LoggerFactory.getLogger(ManagementService.class);

    /**
     * Management Event Cache - stores management events for a specified period of time
     * after which the events are expired from the cache.
     */
    private final Cache<UUID, ManagementEvent> managementEventCache;
    private final ResourceServiceNetworkUtils networkUtils;

    /**
     * Use this getter to access the cache so it's easy to tell where the cache is
     * being accessed from
     */
    private Cache<UUID, ManagementEvent> getManagementEventCache() {return this.managementEventCache;}

    /**
     * EXPERIMENTAL CODE - This is to try and make this efficient. I never do this up-front,
     * but it seemed like fun, so what the heck?
     * <p>
     * The idea is that each search criterion has a map below that maps from its values to
     * the event ID that it goes with to avoid having to search the entire cache each time
     * a search comes through. This might end up being more trouble than it's worth.
     */
    private final Map<UUID, Set<UUID>> correlationIdToEventIdMap = new HashMap<>();
    private final Map<String, Set<UUID>> sportToEventIdMap = new HashMap<>();
    private final Map<ManagementEventType, Set<UUID>> managementEventTypeToEventIdMap = new HashMap<>();

    public ManagementService(final ResourceServiceNetworkUtils networkUtils) {
        this.networkUtils = networkUtils;
        //
        // Initialize Management Cache
        this.managementEventCache = CacheBuilder.newBuilder()
                .initialCapacity(500)
                //.expireAfterWrite(6000, TimeUnit.MINUTES) // Minimum of 100 hours
                .maximumSize(100000)
                .build();
    }

    /**
     * Creates a new ManagementEvent with this service. The caller must supply at
     * least event type and host name or a BadRequestException is thrown.
     * <p>
     * This should be the ONLY WAY to store an item in the cache, or the EXPERIMENTAL
     * search optimizations might break. BE WARNED!
     *
     * @param managementEvent The event to register
     * @return The original ManagementEvent, possibly decorated with additional information
     * as the service deems necessary and as makes sense for the particular event type,
     * and any other context, so forth, as makes sense. This is intentionally vague so as
     * to be as open and extensible as possible.
     */
    public ManagementEvent createEvent(final ManagementEvent managementEvent) {
        //
        // Validate the ManagementEvent object, and if valid, a copy of the
        // original is returned, ready for decoration.
        final ManagementEvent decoratedManagementEvent = validateCreateEvent(managementEvent);
        //
        // Generate UUID for the new event
        UUID eventId = UUID.randomUUID();
        decoratedManagementEvent.setEventId(eventId);
        //
        // If the correlationId is null, then set it to the eventId, allowing clients
        // to correlate a future event with one that it has already registered
        if (managementEvent.getCorrelationId() == null) {
            decoratedManagementEvent.setCorrelationId(eventId);
        }
        //
        // Manage the search optimizations
        manageCacheSearchOptimizations(decoratedManagementEvent);
        //
        // Store the event in the cache
        getManagementEventCache().put(eventId, decoratedManagementEvent);
        return decoratedManagementEvent;
    }

    /**
     * Inspect the specified ManagementEvent and use its contents to manage any cache
     * search optimizations in place.
     *
     * @param decoratedManagementEvent The management event for which information is to
     *                                 be added that might be optimized for a later search
     */
    private void manageCacheSearchOptimizations(final ManagementEvent decoratedManagementEvent) {
        UUID eventId = decoratedManagementEvent.getEventId();
        //
        // Add the correlation ID -> event ID mapping
        UUID correlationId = decoratedManagementEvent.getCorrelationId();
        Set<UUID> eventIdsForCorrelationId = correlationIdToEventIdMap.computeIfAbsent(correlationId, key -> new HashSet<>());
        eventIdsForCorrelationId.add(eventId);
        //
        // Add the sport -> event ID mapping
        String sport = decoratedManagementEvent.getSport();
        if (StringUtils.isNotEmpty(sport)) {
            Set<UUID> eventIdsForSport = sportToEventIdMap.computeIfAbsent(sport, key -> new HashSet<>());
            eventIdsForSport.add(eventId);
        }
        //
        // Add the management event type -> event ID mapping
        ManagementEventType managementEventType = decoratedManagementEvent.getManagementEventType();
        Set<UUID> eventIdsForManagementEventType = managementEventTypeToEventIdMap.computeIfAbsent(managementEventType, key -> new HashSet<>());
        eventIdsForManagementEventType.add(eventId);
    }

    /**
     * Fetches all events that are currently registered and returns a summary of them
     * in a List of minimally populated ManagementEvent objects. Only a handful of the
     * fields are populated so as to give the caller a sense of the events that are
     * registered and enough information to call back in to get details on an event-by-event
     * basis.
     */
    public List<ManagementEvent> fetchAllEvents() {
        //
        // Get the cache as a map and go through it
        ConcurrentMap<UUID, ManagementEvent> cache = getManagementEventCache().asMap();
        Set<UUID> cacheKeys = cache.keySet();

        return cacheKeys.stream()
                .map(cache::get)
                .collect(Collectors.toList());
    }

    /**
     * Fetches the specified event by its ID.
     * <p>
     * If the event cannot be found, a NotFoundException is thrown.
     *
     * @param eventId The UUID of the event to fetch.
     * @return ManagementEvent object representing the event.
     * @throws NotFoundException if the event
     *                           cannot be located by its ID
     */
    public ManagementEvent fetchEventById(final UUID eventId) {
        //
        // Fetch the event from the cache. If it is not found, throw a NotFoundException.
        ManagementEvent managementEvent = getManagementEventCache().getIfPresent(eventId);
        if (managementEvent == null) {
            String errorMessage = String.format("%s0071E: Event ID not found: %s", LOG_PREFIX, eventId);
            log.error(errorMessage);
            throw new NotFoundException(errorMessage);
        }
        return managementEvent;
    }

    /**
     * Fetches and returns a list of events that are related through their correlationId
     * attribute. This allows a caller to stitch together related events (a training run
     * start and end, for example). While each individual parameter is optional, at least
     * one parameter must be supplied or a BadRequestException will be thrown.
     *
     * @param correlationId        The UUID to correlate management events (optional)
     * @param sport                The sport, such as nba, nfl, etc (optional)
     * @param managementEventTypes The management event type (optional)
     * @param mostRecentN          The most recent N events matching the other criteria
     * @return A list of one or more ManagementEvent all having the same correlationId attribute.
     * If no events were found with the specified correlationId the list will be empty.
     */
    public List<ManagementEvent> fetchCorrelatedEvents(final UUID correlationId,
                                                       final String sport,
                                                       final List<ManagementEventType> managementEventTypes,
                                                       final Integer mostRecentN) {
        long startTime = System.currentTimeMillis();
        validateCorrelatedEventsRequest(correlationId, sport, managementEventTypes);
        //
        // Run the search
        // Correlation ID
        List<ManagementEvent> unfilteredCorrelationIdList = searchBy(correlationId, correlationIdToEventIdMap);
        // Sport
        List<ManagementEvent> unfilteredSportlist = searchBy(sport, sportToEventIdMap);
        //
        // ManagementEventType
        List<ManagementEvent> unfilteredManagementEventTypeList = searchBy(managementEventTypes, managementEventTypeToEventIdMap);
        //
        // Combine the lists
        Set<ManagementEvent> combinedList = new HashSet<>();
        combinedList.addAll(unfilteredCorrelationIdList);
        combinedList.addAll(unfilteredSportlist);
        combinedList.addAll(unfilteredManagementEventTypeList);
        //
        // Stream out the list and apply filters to perform the AND
        List<ManagementEvent> ret = combinedList.stream()
                .filter(managementEvent -> correlationId == null || managementEvent.getCorrelationId().equals(correlationId))
                .filter(managementEvent -> StringUtils.isEmpty(sport) || managementEvent.getSport().equals(sport))
                .filter(managementEvent -> managementEventTypes == null || managementEventTypes.contains(managementEvent.getManagementEventType()))
                .filter(managementEvent -> managementEvent.getNetworkDescriptor() != null)
                .filter(managementEvent -> managementEvent.getNetworkDescriptor().getWhenTrained() != null)
                .sorted(Comparator.comparing(ManagementEvent::getSystemTimeMillis))
                .collect(Collectors.toList());
        if (mostRecentN != null && mostRecentN <= ret.size()) {
            int endIndex = ret.size(); // The end index is exclusive
            int begIndex = endIndex - mostRecentN;
            ret = ret.subList(begIndex, endIndex);
        }
        log.debug("fetchCorrelatedEvents(): Found {} matching events in {}ms", ret.size(), System.currentTimeMillis() - startTime);
        return ret;
    }

    /**
     * Search the cache by a list of things.
     *
     * @param <T>      The type of the thing
     * @param things   The list of thing to search for
     * @param thingMap The map of thing -> eventId
     * @return A list of all ManagementEvent that match the specified things in the list
     */
    private <T> List<ManagementEvent> searchBy(final List<T> things, final Map<T, Set<UUID>> thingMap) {
        final List<ManagementEvent> unfilteredList = new ArrayList<>();
        if (things != null && !things.isEmpty()) {
            things.forEach(thing -> {
                List<ManagementEvent> managementEvents = searchBy(thing, thingMap);
                unfilteredList.addAll(managementEvents);
            });
        }
        return unfilteredList;
    }

    /**
     * Search the cache by thing. If there are expired items in the cache, remove
     * their mapping from the thing -> eventId map.
     *
     * @param <T>      The type of the thing
     * @param thing    The thing to search for
     * @param thingMap The map of thing -> eventId
     * @return A list of all ManagementEvent that match the specified thing
     */
    private <T> List<ManagementEvent> searchBy(final T thing, final Map<T, Set<UUID>> thingMap) {
        final List<ManagementEvent> unfilteredList = new ArrayList<>();
        //
        // Get the set of UUID that maps to the thing
        Set<UUID> eventIdsToManagementEventType = thingMap.get(thing);
        if (eventIdsToManagementEventType != null) {
            Set<UUID> expiredEventIds = new HashSet<>();
            eventIdsToManagementEventType.forEach(eventId -> {
                ManagementEvent event = getManagementEventCache().getIfPresent(eventId);
                if (event == null) {
                    expiredEventIds.add(eventId);
                } else {
                    unfilteredList.add(event);
                }
            });
            //
            // Cleanup any expired eventIds
            expiredEventIds.forEach(eventIdsToManagementEventType::remove);
        }
        return unfilteredList;
    }

    /**
     * Validates a create event request. If the request is invalid, a BadRequestException
     * is thrown.
     *
     * @param managementEvent The ManagementEvent object to validate
     */
    private ManagementEvent validateCreateEvent(final ManagementEvent managementEvent) {
        if (managementEvent.getManagementEventType() == null) {
            String errorMessage = String.format("%s0073E: Expected a valid ManagementEvent, got null instead.", LOG_PREFIX);
            log.error("validateCreateEvent(): {}", errorMessage);
            throw new BadRequestException(errorMessage);
        }
        if (StringUtils.isEmpty(managementEvent.getHostName())) {
            String errorMessage = String.format("%s0075E: Expected a host name, got null instead.", LOG_PREFIX);
            log.error("validateCreateEvent(): {}", errorMessage);
            throw new BadRequestException(errorMessage);
        }
        //
        // We're all good, return a copy of the original object.
        return managementEvent.copy();
    }

    /**
     * Validates a correlated events search request. If the request is invalid a
     * BadRequestException is thrown. At least one parameter must be supplied.
     *
     * @param correlationId The correlationId to be searched for
     * @param sport         The sport to be searched for
     */
    private void validateCorrelatedEventsRequest(final UUID correlationId,
                                                 final String sport,
                                                 final List<ManagementEventType> managementEventType) {
        if (correlationId == null && StringUtils.isEmpty(sport) && (managementEventType == null || managementEventType.isEmpty())) {
            String errorMessage = String.format("%s0077E: Invalid search, no search parameters supplied.", LOG_PREFIX);
            log.error("validateCorrelatedEventsRequest(): {}", errorMessage);
            throw new BadRequestException(errorMessage);
        }
    }

    /**
     * Return a list of ManagementEventSummary objects, which contain aggregate stats from
     * the list of ManagementEvent passed in.
     *
     * @param registeredCorrelatedEvents The list of ManagementEvent to aggregate
     * @return A list of one or more ManagementEventSummary, there will be one for each
     * correlationId in the list of ManagementEvent
     */
    public List<ManagementEventSummary> computeStats(final List<ManagementEvent> registeredCorrelatedEvents) {
        //
        // Framework does all the heavy lifting
        return networkUtils.computeAverageStats(registeredCorrelatedEvents);
    }
}
