/*
 * Copyright © 2021 J. Steven Perry
 * All rights reserved.
 */

package com.jstevenperry.sportsnet.framework.service;

import com.jstevenperry.sportsnet.framework.config.AbstractSportsnetProperties;
import com.jstevenperry.sportsnet.framework.dto.NetworkCandidateDto;
import com.jstevenperry.sportsnet.framework.exception.NotFoundException;
import com.jstevenperry.sportsnet.framework.model.*;
import com.jstevenperry.sportsnet.framework.persistence.SeasonDataDao;
import com.jstevenperry.sportsnet.framework.persistence.TournamentParticipantDao;
import com.jstevenperry.sportsnet.framework.util.AbstractNetworkProperties;
import com.jstevenperry.sportsnet.framework.util.AbstractNetworkUtils;
import com.jstevenperry.sportsnet.framework.util.SimpleCache;
import com.opencsv.CSVWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("SameParameterValue")
public abstract class AbstractNetworkService<T extends SeasonData> {

    private static final Logger log = LoggerFactory.getLogger(AbstractNetworkService.class);

    public static final String LOG_PREFIX = "COREANS";

    private final SeasonDataDao<T> seasonDataDao;

    protected SeasonDataDao<T> getSeasonDataDao() {
        log.debug("Returning seasonDataDao...");
        return seasonDataDao;
    }

    private final TournamentParticipantDao tournamentParticipantDao;

    public TournamentParticipantDao getTournamentParticipantDao() {
        log.debug("Returning tournamentParticipantDao...");
        return tournamentParticipantDao;
    }

    private final AbstractNetworkUtils networkUtils;

    public AbstractNetworkUtils getNetworkUtils() {
        return networkUtils;
    }

    private final AbstractSportsnetProperties sportsnetProperties;
    private final AbstractNetworkProperties networkProperties;

    public AbstractNetworkProperties getNetworkProperties() {
        return networkProperties;
    }

    /**
     * The network candidate "stash". This is where trained networks will live until the
     * client clears or persists them, or until the service bean dies (most likely when the
     * microservice restarts).
     */
    private SimpleCache<UUID, NetworkCandidate> networkCandidateSimpleCache;

    public SimpleCache<UUID, NetworkCandidate> getNetworkCandidateSimpleCache() {
        log.debug("Returning networkCandidateMap..., current size: {}", networkCandidateSimpleCache.size());
        return networkCandidateSimpleCache;
    }

    public AbstractNetworkService(final SeasonDataDao<T> seasonDataDao,
                                  final TournamentParticipantDao tournamentParticipantDao,
                                  final AbstractNetworkUtils networkUtils,
                                  final AbstractSportsnetProperties sportsnetProperties,
                                  final AbstractNetworkProperties networkProperties) {

        this.seasonDataDao = seasonDataDao;
        this.tournamentParticipantDao = tournamentParticipantDao;
        this.networkUtils = networkUtils;
        this.sportsnetProperties = sportsnetProperties;
        this.networkProperties = networkProperties;
        //
        // Init caches
        initializeCaches(networkProperties);
    }

    protected void initializeCaches(final AbstractNetworkProperties networkProperties) {
        networkCandidateSimpleCache = new SimpleCache<>(networkProperties);
    }

    protected void cleanup() {
        getNetworkCandidateSimpleCache().cleanup();
    }

    public abstract List<Double> seasonDataAsDoubleList(final T seasonData,
                                                        final Double resultClass,
                                                        final boolean isTraining);

    public List<Double> seasonDataAsDoubleList(final T seasonData) {
        return seasonDataAsDoubleList(seasonData, null, false);
    }

    /**
     * Return the network type of the implementing subclass
     *
     * @deprecated Replaced by getSport()
     */
    @Deprecated
    public abstract String getNetworkType();

    /**
     * Return the sports-specific URI used by the implementing subclass.
     */
    protected abstract String getSportsSpecificUri();

    /**
     * Retrieves the service ID for the implementing subclass service.
     */
    protected abstract UUID getServiceId();

    /**
     * Returns the name of the sport, such as nba, ncaabb, nfl, etc. This replaces getNetworkType().
     */
    protected abstract String getSport();

    /**
     * Loads all networks for the specified network type implemented by the
     * subclass. This is done using the resource server.
     */
    public List<NetworkCandidateDto> getNetworks() {
        log.debug("getNetworks(): Fetching all networks...");
        long startTime = System.currentTimeMillis();

        String url = getNetworkUtils().computeResourceServiceNetworksUrl(sportsnetProperties, getSportsSpecificUri(), ResourceType.NETWORK_DESCRIPTOR);
        List<NetworkCandidateDto> networkDescriptors = getNetworkUtils().fetchAllNetworks(url);

        log.debug("getNetworks(): Fetched {} networks from {} in {}ms", networkDescriptors.size(), url, System.currentTimeMillis() - startTime);
        return networkDescriptors;
    }

    /**
     * Fetches and returns the network descriptor specified by the network ID from the
     * resource service.
     */
    public Optional<NetworkCandidateDto> findNetworkById(final UUID networkId) {
        final Optional<NetworkCandidateDto> networkCandidateDto;
        log.debug("findNetworkById({}): Finding network...", networkId);
        long startTime = System.currentTimeMillis();

        String url = networkUtils.computeNetworkFileUrl(sportsnetProperties, getSportsSpecificUri(), networkId, ResourceType.DL4J_NETWORK);
        networkCandidateDto = getNetworkUtils().fetchNetworkFileFromResourceService(url);

        log.debug("findNetworkById({}): Done in {}ms", networkId, System.currentTimeMillis() - startTime);
        return networkCandidateDto;
    }

    /**
     * Saves the specified network, leaving the implementation details of this
     * to the implementing subclass. Meant to be called from a microservice that
     * has trained a network and now needs to save it. Must be implemented by
     * subclasses, which may, for example, use the resource server to save the
     * network, rather than saving it locally.
     * <p>
     * If the network cannot be saved, an exception is thrown.
     */
    protected void saveNetwork(final NetworkCandidate networkCandidate, final ResourceType resourceType) {
        log.info("saveNetwork(): Saving {} network: {}", resourceType, networkCandidate);
        long startTime = System.currentTimeMillis();
        //
        // Compute the full URL to the resource server where we will need to send the payload
        String networkFileUrl = getNetworkUtils().computeResourceServerUrl(sportsnetProperties, resourceType);
        //
        // Send it
        NetworkDescriptor savedNetworkDescriptor = getNetworkUtils().saveNetworkOnResourceServer(networkFileUrl, networkCandidate);
        log.info("saveNetwork(): Saved network: {} in {}ms", savedNetworkDescriptor, System.currentTimeMillis() - startTime);
    }


    /**
     * Saves the specified network by its ID, presumably because the network is
     * currently unsaved. This is meant to be called by a REST client that only
     * has the network ID and the knowledge that the network has never been
     * persisted.
     */
    public NetworkDescriptor saveNetwork(final UUID networkId, final ResourceType resourceType) {
        final NetworkDescriptor ret;
        long startTime = System.currentTimeMillis();
        //
        // Fetch the network candidate from the map of previously trained but unsaved networks
        Optional<NetworkCandidate> networkCandidate = getNetworkCandidateSimpleCache().fetch(networkId);
        //
        // Expectation: the network is present, if not throw an exception
        if (networkCandidate.isPresent()) {
            //
            // Network trained but unsaved, save it
            saveNetwork(networkCandidate.get(), resourceType);
            //
            // Now remove it from the map of trained but unsaved networks
            ret = popNetworkCandidate(networkId);
        } else {
            String errorMessage = String.format("Network ID %s not found.", networkId);
            log.error("saveNetwork({}): {}", networkId, errorMessage);
            throw new NotFoundException(errorMessage);
        }
        log.debug("saveNetwork({}): Saved network: {} in {}ms", networkId, ret, System.currentTimeMillis() - startTime);
        return ret;
    }

    /**
     * Stashes the specified network. This is a way to keep trained networks around as long as
     * the service bean is alive, so the networks have a limited and non-deterministic lifetime,
     * hence they are "stashed". This gives a client the opportunity to query them and possibly
     * persist them if desired, but after the service bean goes away (probably after a microservice
     * restart), they are gone for good.
     */
    public void stashNetworkCandidate(final NetworkCandidate networkCandidate) {
        NetworkDescriptor networkDescriptor = networkCandidate.getTrainedNetworkParameters();
        if (networkDescriptor != null) {
            UUID networkId = networkDescriptor.getNetworkId();
            if (networkId != null) {
                getNetworkCandidateSimpleCache().cleanup(); // Just being a good citizen
                getNetworkCandidateSimpleCache().store(networkCandidate.getTrainedNetworkParameters()
                        .getNetworkId(), networkCandidate);
                log.info("stashNetwork(): Stashed Network Candidate: {}", networkCandidate);
            } else {
                String errorMessage = String.format("%s0019E: Network ID is null! Network Candidate not stashed: %s", LOG_PREFIX, networkCandidate);
                log.error("stashNetwork(): {}", errorMessage);
                throw new RuntimeException(errorMessage);
            }
        } else {
            String errorMessage = String.format("%s0021E: TrainedNetworkParameters is null! Network Candidate not stashed: %s", LOG_PREFIX, networkCandidate);
            log.error("stashNetwork(): {}", errorMessage);
            throw new RuntimeException(errorMessage);
        }
    }

    /**
     * Removes (pops) a NetworkCandidate from the stash, the network is specified by its
     * network ID. If the network is in the stash, it is removed and returned to the caller.
     */
    public NetworkDescriptor popNetworkCandidate(final UUID networkId) {
        NetworkDescriptor ret;
        NetworkCandidate networkCandidate = getNetworkCandidateSimpleCache().expire(networkId);
        ret = networkCandidate.getTrainedNetworkParameters();
        return ret;
    }

    /**
     * Fetches the specified network by its NetworkId (if found), and returns its
     * TrainedNetworkParameters object descriptor. The NetworkCandidate is left in
     * the map (hence the "peek", as opposed to "pop")
     */
    public NetworkCandidate peekNetworkCandidate(final UUID networkId) {
        return getNetworkCandidateSimpleCache().fetch(networkId).orElse(null);
    }

    /**
     * Returns a list of all the networks in the network stash, that is, networks that have not
     * been persisted. The list that is returned is sorted by whenTrained, descending
     */
    public List<NetworkCandidateDto> fetchUnsavedNetworks() {
        SimpleCache<UUID, NetworkCandidate> networkCandidateMap = getNetworkCandidateSimpleCache();
        return networkCandidateMap.keys().stream()
                .map(networkCandidateMap::fetch)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .sorted()
                .map(NetworkCandidate::toDto)
                .collect(Collectors.toList());
    }

    protected T pullSeasonData(final Integer year, final String teamName) {
        long startTime = System.currentTimeMillis();
        log.debug(String.format("Reading season data for team %s and year %d from DB...", teamName, year));
        T seasonData = getSeasonDataDao().fetchByYearAndTeamName(year, teamName);
        log.debug("pullSeasonData({},{}): completed in {}ms", year, teamName, System.currentTimeMillis() - startTime);
        return seasonData;
    }

    public String createTrainingCsvData(final NetworkDescriptor networkDescriptor) {
        return createCsvData(networkDescriptor.getYearsToTrainNetwork(), networkProperties.getSmoothTrainingData());
    }

    public String createEvaluationCsvData(final NetworkDescriptor networkDescriptor) {
        return createCsvData(networkDescriptor.getYearsToEvaluateNetwork(), networkProperties.getSmoothEvaluationData());
    }

    private String createCsvData(final List<Integer> yearsToPull, final boolean smoothTrainingData) {
        StringWriter stringWriter = new StringWriter();
        CSVWriter csvWriter = new CSVWriter(stringWriter);
        for (Integer year : yearsToPull) {
            List<TournamentParticipant> tournamentParticipants = getTournamentParticipantDao().fetchAllByYear(year);
            final List<String[]> dataRows = new ArrayList<>();
            for (TournamentParticipant tournamentParticipant : tournamentParticipants) {
                String teamName = tournamentParticipant.getTeamName();
                T seasonData = pullSeasonData(year, teamName);
                //
                // Transform the data, then write out the data
                List<Double> rowWinDouble = seasonDataAsDoubleList(seasonData, tournamentParticipant.getNumberOfVictories()
                        .doubleValue(), true);
                String[] rowWinString = seasonData.transformRow(rowWinDouble, true);
                dataRows.add(rowWinString);
                if (smoothTrainingData) {
                    // Only smooth for evaluation, not for training!
                    final List<String[]> smoothedData = smoothData(tournamentParticipant, rowWinString);
                    log.info("createCsvData(): Create {} additional records for {} (numberOfVictories={})", smoothedData.size(), tournamentParticipant.getTeamName(), tournamentParticipant.getNumberOfVictories());
                    dataRows.addAll(smoothedData);
                }
                log.debug("CSV SeasonData: Team: {} (#victories:{}), data: {}", teamName, tournamentParticipant.getNumberOfVictories(), Arrays.toString(rowWinString));
            }
            Collections.shuffle(dataRows); // Attempt to defeat any dl4j optimizations that might recognize identical adjacent rows
            log.info("createCsvData(): After shuffle: {} records remain", dataRows.size());
            dataRows.forEach(csvWriter::writeNext);
        }
        //
        // Get the underlying String and via dl4j's arcane class structure transform it into a DataSet and return it
        StringBuffer stringBuffer = stringWriter.getBuffer();
        return stringBuffer.toString();
    }

    /**
     * Creates a new ManagementEvent object on the management service and returns it to the caller.
     */
    protected ManagementEvent createManagementEvent(final ManagementEventType managementEventType,
                                                    final String message) {
        return createManagementEvent((ManagementEvent) null, managementEventType, message);
    }

    protected ManagementEvent createManagementEvent(final NetworkDescriptor networkDescriptor,
                                                    final ManagementEventType managementEventType,
                                                    final String message) {
        return createManagementEvent(null, networkDescriptor, managementEventType, message);
    }

    /**
     * Creates a new ManagementEvent object on the management service and returns it to the caller.
     */
    @SuppressWarnings("unused")
    protected ManagementEvent createManagementEvent(final ManagementEvent managementEvent,
                                                    final ManagementEventType managementEventType,
                                                    final String message) {
        return createManagementEvent(null, null, managementEventType, message);
    }

    /**
     * Returns a ManagementEvent object on the management service and returns it to the caller.
     * The caller may pass an existing ManagementEvent object to be used as the payload, which
     * allows the caller some control over the payload, at their discretion (setting the
     * correlationId, for example).
     */
    protected ManagementEvent createManagementEvent(final ManagementEvent managementEvent,
                                                    final NetworkDescriptor networkDescriptor,
                                                    final ManagementEventType managementEventType,
                                                    final String message) {
        //
        // Compute URL to the management service and the payload
        String managementEventUrl = getNetworkUtils().computeManagementServiceUrl(sportsnetProperties, sportsnetProperties.getSportsnetServiceUriEvents());
        final ManagementEvent managementEventPayload;
        if (managementEvent == null) {
            //
            // Create payload from scratch
            managementEventPayload = getNetworkUtils().createManagementEventPayload(getServiceId());
        } else {
            //
            // Reuse existing payload
            managementEventPayload = managementEvent;
        }
        //
        // Decorate the payload
        managementEventPayload
                .setSystemTimeMillis(System.currentTimeMillis())
                .setNetworkDescriptor(networkDescriptor)
                .setManagementEventType(managementEventType)
                .setMessage(message)
                .setSport(getSport());
        //
        // Make the call to the management service
        final ManagementEvent managementEventFromServer = getNetworkUtils().createManagementEvent(managementEventUrl, managementEventPayload);
        //
        // Return the object decorated by the server to the caller
        log.debug("createManagementEvent({}): ManagementEvent from server: {}", managementEventType, managementEventFromServer);
        return managementEventFromServer;
    }

    /**
     * Implement smoothing algorithm for the specified training data. Serves to smooth
     * over the skewed nature of the number of tournament participants who advance further
     * in a tournament. The further a participant advances, the fewer of those type of
     * records are in the data, and the network metrics are skewed. If a network evalates
     * well for, say, zero-labeled data (for which there are MANY more rows), the network
     * may not really be performing well.
     *
     * @param tournamentParticipant The labeled tournament participant data for a single participant
     * @param rowWinString          The data to be duplicated (naive smoothing, I know, but it's all we can do)
     * @return List of string arrays, containing the smoothed data
     */
    @SuppressWarnings("unused")
    protected List<String[]> smoothData(final TournamentParticipant tournamentParticipant,
                                        final String[] rowWinString) {
        // Default: do nothing, subclasses can override (but don't have to)
        return new ArrayList<>();
    }

}
