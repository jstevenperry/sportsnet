/*
 * Copyright 2015-2021, James Steven Perry. All rights reserved.
 */

package com.jstevenperry.sportsnet.ncaabb.plugins;

import com.jstevenperry.sportsnet.framework.model.TeamCoordinate;
import com.jstevenperry.sportsnet.framework.persistence.SeasonDataDao;
import com.jstevenperry.sportsnet.ncaabb.model.NcaaBbSeasonData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TournamentRunnerPlugin2020 extends AbstractNcaaBbTournamentRunnerPlugin {

    public TournamentRunnerPlugin2020(final SeasonDataDao<NcaaBbSeasonData> seasonDataDao) {
        super(seasonDataDao);
        setTeamCoordinateSeasonDataMap(buildTeamCoordinateMap());
    }

    @Override
    public Integer getTournamentYear() {
        return 2020;
    }

    @Override
    public String getTournamentYearForDisplay() {
        return "2020-21";
    }

    @Override
    public String getTournamentTemplateFileName() {
        return "/ncaabb/2020/tournament-template-ncaabb-2020.txt";
    }

    @Override
    protected String getTournamentResultsTemplateFileName() {
        return "/ncaabb/2020/tournament-results-template.csv";
    }

    /**
     * Builds the internal map of TeamCoordinate -> SeasonData
     */
    @Override
    protected Map<TeamCoordinate, NcaaBbSeasonData> buildTeamCoordinateMap() {
        // Map a team to its initial team coordinates
        Map<TeamCoordinate, NcaaBbSeasonData> coordinateSeasonDataMap = new HashMap<>();
        // First Four
        createFirstFourMappings(coordinateSeasonDataMap);
        // Round of 64 (Round 1)
        createRoundOneMappingsRegion0(coordinateSeasonDataMap);
        createRoundOneMappingsRegion1(coordinateSeasonDataMap);
        createRoundOneMappingsRegion2(coordinateSeasonDataMap);
        createRoundOneMappingsRegion3(coordinateSeasonDataMap);
        return coordinateSeasonDataMap;
    }

    private void createFirstFourMappings(final Map<TeamCoordinate, NcaaBbSeasonData> coordinateSeasonDataMap) {
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 0, 0, "ASDF");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 0, 1, "ASDF");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 0, 8, "ASDF");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 0, 9, "ASDF");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 0, 0, "ASDF");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 0, 1, "ASDF");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 0, 8, "ASDF");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 0, 9, "ASDF");
    }

    // For 2018-19 Region 0
    private void createRoundOneMappingsRegion0(final Map<TeamCoordinate, NcaaBbSeasonData> coordinateSeasonDataMap) {
//    [0,1,0],[0,1,1]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 0, "ASDF");
        // [0,1,1] is First Four Winner, cannot initialize this TeamCoordinate (it must be computed)
//    [0,1,2],[0,1,3]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 2, "ASDF");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 3, "ASDF");
//    [0,1,4],[0,1,5]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 4, "ASDF");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 5, "ASDF");
//    [0,1,6],[0,1,7]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 6, "ASDF");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 7, "ASDF");
//    [0,1,8],[0,1,9]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 8, "ASDF");
        // [0,1,9] is First Four Winner, cannot initialize this TeamCoordinate (it must be computed)
//    [0,1,10],[0,1,11]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 10, "ASDF");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 11, "ASDF");
//    [0,1,12],[0,1,13]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 12, "ASDF");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 13, "ASDF");
//    [0,1,14],[0,1,15]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 14, "ASDF");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 15, "ASDF");
    }

    // For 2018-19 Region 1
    private void createRoundOneMappingsRegion1(final Map<TeamCoordinate, NcaaBbSeasonData> coordinateSeasonDataMap) {
//    [1,1,0],[1,1,1]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 0, "ASDF");
        // [1,1,1] is First Four Winner, cannot initialize this TeamCoordinate (it must be computed)
//    [1,1,2],[1,1,3]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 2, "ASDF");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 3, "ASDF");
//    [1,1,4],[1,1,5]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 4, "ASDF");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 5, "ASDF");
//    [1,1,6],[1,1,7]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 6, "ASDF");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 7, "ASDF");
//    [1,1,8],[1,1,9]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 8, "ASDF");
        // [1,1,9] is First Four Winner, cannot initialize this TeamCoordinate (it must be computed)
//    [1,1,10],[1,1,11]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 10, "ASDF");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 11, "ASDF");
//    [1,1,12],[1,1,13]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 12, "ASDF");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 13, "ASDF");
//    [1,1,14],[1,1,15]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 14, "ASDF");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 15, "ASDF");
    }

    // For 2018-19 Region 2
    private void createRoundOneMappingsRegion2(final Map<TeamCoordinate, NcaaBbSeasonData> coordinateSeasonDataMap) {
//    [2,1,0],[2,1,1]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 0, "ASDF");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 1, "ASDF");
//    [2,1,2],[2,1,3]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 2, "ASDF");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 3, "ASDF");
//    [2,1,4],[2,1,5]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 4, "ASDF");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 5, "ASDF");
//    [2,1,6],[2,1,7]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 6, "ASDF");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 7, "ASDF");
//    [2,1,8],[2,1,9]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 8, "ASDF");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 9, "ASDF");
//    [2,1,10],[2,1,11]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 10, "ASDF");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 11, "ASDF");
//    [2,1,12],[2,1,13]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 12, "ASDF");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 13, "ASDF");
//    [2,1,14],[2,1,15]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 14, "ASDF");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 15, "ASDF");
    }

    // For 2018-19 Region 3
    private void createRoundOneMappingsRegion3(final Map<TeamCoordinate, NcaaBbSeasonData> coordinateSeasonDataMap) {
//    [3,1,0],[3,1,1]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 0, "ASDF");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 1, "ASDF");
//    [3,1,2],[3,1,3]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 2, "ASDF");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 3, "ASDF");
//    [3,1,4],[3,1,5]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 4, "ASDF");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 5, "ASDF");
//    [3,1,6],[3,1,7]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 6, "ASDF");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 7, "ASDF");
//    [3,1,8],[3,1,9]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 8, "ASDF");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 9, "ASDF");
//    [3,1,10],[3,1,11]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 10, "ASDF");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 11, "ASDF");
//    [3,1,12],[3,1,13]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 12, "ASDF");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 13, "ASDF");
//    [3,1,14],[3,1,15]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 14, "ASDF");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 15, "ASDF");
    }

    @Override
    public List<Double> seasonDataAsDoubleList(final NcaaBbSeasonData seasonData, final Double resultClass, final boolean isTraining) {
        return NcaaBbSeasonData.asDoubleList(seasonData, resultClass, isTraining);
    }
}
