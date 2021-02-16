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

public class TournamentRunnerPlugin2009 extends AbstractNcaaBbTournamentRunnerPlugin {

    public TournamentRunnerPlugin2009(final SeasonDataDao<NcaaBbSeasonData> seasonDataDao) {
        super(seasonDataDao);
        setTeamCoordinateSeasonDataMap(buildTeamCoordinateMap());
    }

    @Override
    public Integer getTournamentYear() {
        return 2009;
    }

    @Override
    public String getTournamentYearForDisplay() {
        return "2009-10";
    }

    @Override
    public String getTournamentTemplateFileName() {
        return "/ncaabb/2009/tournament-template-ncaabb.txt";
    }

    @Override
    protected String getTournamentResultsTemplateFileName() {
        return "/ncaabb/2009/tournament-results-template.csv";
    }

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
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 0, 0, "Ark-Pine Bluff");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 0, 1, "Winthrop");
    }

    private void createRoundOneMappingsRegion0(final Map<TeamCoordinate, NcaaBbSeasonData> coordinateSeasonDataMap) {
//    [0,1,0],[0,1,1]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 0, "Kansas");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 1, "Lehigh");
//    [0,1,2],[0,1,3]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 2, "UNLV");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 3, "UNI");
//    [0,1,4],[0,1,5]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 4, "Michigan St");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 5, "New Mexico St");
//    [0,1,6],[0,1,7]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 6, "Maryland");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 7, "Houston");
//    [0,1,8],[0,1,9]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 8, "Tennessee");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 9, "San Diego St");
//    [0,1,10],[0,1,11]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 10, "Georgetown");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 11, "Ohio");
//    [0,1,12],[0,1,13]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 12, "Oklahoma St");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 13, "Georgia Tech");
//    [0,1,14],[0,1,15]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 14, "Ohio St");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 15, "UC Santa Barbara");
    }

    private void createRoundOneMappingsRegion1(final Map<TeamCoordinate, NcaaBbSeasonData> coordinateSeasonDataMap) {
//    [1,1,0],[1,1,1]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 0, "Syracuse");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 1, "Vermont");
//    [1,1,2],[1,1,3]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 2, "Gonzaga");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 3, "Florida St");
//    [1,1,4],[1,1,5]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 4, "Butler");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 5, "UTEP");
//    [1,1,6],[1,1,7]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 6, "Vanderbilt");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 7, "Murray St");
//    [1,1,8],[1,1,9]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 8, "Xavier");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 9, "Minnesota");
//    [1,1,10],[1,1,11]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 10, "Pittsburgh");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 11, "Oakland");
//    [1,1,12],[1,1,13]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 12, "BYU");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 13, "Florida");
//    [1,1,14],[1,1,15]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 14, "Kansas St");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 15, "North Texas");
    }

    private void createRoundOneMappingsRegion2(final Map<TeamCoordinate, NcaaBbSeasonData> coordinateSeasonDataMap) {
//    [2,1,0],[2,1,1]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 0, "Kentucky");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 1, "ETSU");
//    [2,1,2],[2,1,3]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 2, "Texas");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 3, "Wake Forest");
//    [2,1,4],[2,1,5]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 4, "Temple");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 5, "Cornell");
//    [2,1,6],[2,1,7]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 6, "Wisconsin");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 7, "Wofford");
//    [2,1,8],[2,1,9]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 8, "Marquette");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 9, "Washington");
//    [2,1,10],[2,1,11]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 10, "New Mexico");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 11, "Montana");
//    [2,1,12],[2,1,13]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 12, "Clemson");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 13, "Missouri");
//    [2,1,14],[2,1,15]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 14, "West Virginia");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 15, "Morgan St");
    }

    private void createRoundOneMappingsRegion3(final Map<TeamCoordinate, NcaaBbSeasonData> coordinateSeasonDataMap) {
//    [3,1,0],[3,1,1]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 0, "Duke");
        // [3,1,1] COMPUTED FROM OPENING ROUND
//    [3,1,2],[3,1,3]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 2, "California");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 3, "Louisville");
//    [3,1,4],[3,1,5]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 4, "Texas AM");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 5, "Utah St");
//    [3,1,6],[3,1,7]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 6, "Purdue");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 7, "Siena");
//    [3,1,8],[3,1,9]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 8, "Notre Dame");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 9, "Old Dominion");
//    [3,1,10],[3,1,11]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 10, "Baylor");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 11, "Sam Houston St");
//    [3,1,12],[3,1,13]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 12, "Richmond");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 13, "Saint Marys CA");
//    [3,1,14],[3,1,15]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 14, "Villanova");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 15, "Robert Morris");
    }

    @Override
    public List<Double> seasonDataAsDoubleList(final NcaaBbSeasonData seasonData, final Double resultClass, final boolean isTraining) {
        return NcaaBbSeasonData.asDoubleList(seasonData, resultClass, isTraining);
    }
}
