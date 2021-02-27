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

public class TournamentRunnerPlugin2007 extends AbstractNcaaBbTournamentRunnerPlugin {

    public TournamentRunnerPlugin2007(final SeasonDataDao<NcaaBbSeasonData> seasonDataDao) {
        super(seasonDataDao);
        setTeamCoordinateSeasonDataMap(buildTeamCoordinateMap());
    }

    @Override
    public Integer getTournamentYear() {
        return 2007;
    }

    @Override
    public String getTournamentYearForDisplay() {
        return "2007-08";
    }

    @Override
    public String getTournamentTemplateFileName() {
        return "/ncaabb/2007/tournament-template-ncaabb.txt";
    }

    @Override
    protected String getTournamentResultsTemplateFileName() {
        return "/ncaabb/2007/tournament-results-template.csv";
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
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 0, 0, "Mt St Marys");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 0, 1, "Coppin St");
    }

    private void createRoundOneMappingsRegion0(final Map<TeamCoordinate, NcaaBbSeasonData> coordinateSeasonDataMap) {
//    [0,1,0],[0,1,1]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 0, "North Carolina");
        // [0,1,1] is First Four Winner, cannot initialize this TeamCoordinate (it must be computed)
//    [0,1,2],[0,1,3]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 2, "Indiana");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 3, "Arkansas");
//    [0,1,4],[0,1,5]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 4, "Notre Dame");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 5, "George Mason");
//    [0,1,6],[0,1,7]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 6, "Washington St");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 7, "Winthrop");
//    [0,1,8],[0,1,9]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 8, "Oklahoma");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 9, "Saint Josephs");
//    [0,1,10],[0,1,11]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 10, "Louisville");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 11, "Boise St");
//    [0,1,12],[0,1,13]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 12, "Butler");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 13, "South Alabama");
//    [0,1,14],[0,1,15]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 14, "Tennessee");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 15, "American");
    }

    private void createRoundOneMappingsRegion1(final Map<TeamCoordinate, NcaaBbSeasonData> coordinateSeasonDataMap) {
//    [1,1,0],[1,1,1]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 0, "Kansas");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 1, "Portland St");
//    [1,1,2],[1,1,3]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 2, "UNLV");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 3, "Kent St");
//    [1,1,4],[1,1,5]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 4, "Clemson");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 5, "Villanova");
//    [1,1,6],[1,1,7]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 6, "Vanderbilt");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 7, "Siena");
//    [1,1,8],[1,1,9]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 8, "Southern California");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 9, "Kansas St");
//    [1,1,10],[1,1,11]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 10, "Wisconsin");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 11, "Cal St Fullerton");
//    [1,1,12],[1,1,13]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 12, "Gonzaga");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 13, "Davidson");
//    [1,1,14],[1,1,15]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 14, "Georgetown");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 15, "UMBC");
    }

    private void createRoundOneMappingsRegion2(final Map<TeamCoordinate, NcaaBbSeasonData> coordinateSeasonDataMap) {
//    [2,1,0],[2,1,1]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 0, "Memphis");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 1, "Texas Arlington");
//    [2,1,2],[2,1,3]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 2, "Mississippi St");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 3, "Oregon");
//    [2,1,4],[2,1,5]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 4, "Michigan St");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 5, "Temple");
//    [2,1,6],[2,1,7]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 6, "Pittsburgh");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 7, "Oral Roberts");
//    [2,1,8],[2,1,9]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 8, "Marquette");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 9, "Kentucky");
//    [2,1,10],[2,1,11]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 10, "Stanford");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 11, "Cornell");
//    [2,1,12],[2,1,13]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 12, "Miami FL");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 13, "Saint Marys CA");
//    [2,1,14],[2,1,15]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 14, "Texas");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 15, "Austin Peay");
    }

    private void createRoundOneMappingsRegion3(final Map<TeamCoordinate, NcaaBbSeasonData> coordinateSeasonDataMap) {
//    [3,1,0],[3,1,1]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 0, "UCLA");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 1, "Mississippi Val");
//    [3,1,2],[3,1,3]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 2, "BYU");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 3, "Texas AM");
//    [3,1,4],[3,1,5]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 4, "Drake");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 5, "Western Ky");
//    [3,1,6],[3,1,7]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 6, "UConn");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 7, "San Diego");
//    [3,1,8],[3,1,9]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 8, "Purdue");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 9, "Baylor");
//    [3,1,10],[3,1,11]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 10, "Xavier");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 11, "Georgia");
//    [3,1,12],[3,1,13]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 12, "West Virginia");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 13, "Arizona");
//    [3,1,14],[3,1,15]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 14, "Duke");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 15, "Belmont");
    }

    @Override
    public List<Double> seasonDataAsDoubleList(final NcaaBbSeasonData seasonData, final Double resultClass, final boolean isTraining) {
        return NcaaBbSeasonData.asDoubleList(seasonData, resultClass, isTraining);
    }
}
