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

public class TournamentRunnerPlugin2013 extends AbstractNcaaBbTournamentRunnerPlugin {

    public TournamentRunnerPlugin2013(final SeasonDataDao<NcaaBbSeasonData> seasonDataDao) {
        super(seasonDataDao);
        setTeamCoordinateSeasonDataMap(buildTeamCoordinateMap());
    }

    @Override
    public Integer getTournamentYear() {
        return 2013;
    }

    @Override
    public String getTournamentYearForDisplay() {
        return "2013-14";
    }

    @Override
    public String getTournamentTemplateFileName() {
        return "/ncaabb/2013/tournament-template-ncaabb.txt";
    }

    @Override
    protected String getTournamentResultsTemplateFileName() {
        return "/ncaabb/2013/tournament-results-template.csv";
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
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 0, 0, "Albany NY");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 0, 1, "Mt St Marys");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 0, 0, "Cal Poly");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 0, 1, "Texas Southern");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 0, 4, "NC State");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 0, 5, "Xavier");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 0, 8, "Iowa");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 0, 9, "Tennessee");
    }

    // For 2013-14 Region 0 is the South
    private void createRoundOneMappingsRegion0(final Map<TeamCoordinate, NcaaBbSeasonData> coordinateSeasonDataMap) {
//    [0,1,0],[0,1,1]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 0, "Florida");
        // [0,1,1] is First Four Winner, cannot initialize this TeamCoordinate (it must be computed)
//    [0,1,2],[0,1,3]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 2, "Colorado");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 3, "Pittsburgh");
//    [0,1,4],[0,1,5]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 4, "VCU");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 5, "SFA");
//    [0,1,6],[0,1,7]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 6, "UCLA");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 7, "Tulsa");
//    [0,1,8],[0,1,9]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 8, "Ohio St");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 9, "Dayton");
//    [0,1,10],[0,1,11]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 10, "Syracuse");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 11, "Western Mich");
//    [0,1,12],[0,1,13]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 12, "New Mexico");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 13, "Stanford");
//    [0,1,14],[0,1,15]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 14, "Kansas");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 15, "Eastern Ky");
    }

    // For 2013-14 Region 1 is the East
    private void createRoundOneMappingsRegion1(final Map<TeamCoordinate, NcaaBbSeasonData> coordinateSeasonDataMap) {
//    [1,1,0],[1,1,1]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 0, "Virginia");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 1, "Coastal Caro");
//    [1,1,2],[1,1,3]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 2, "Memphis");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 3, "George Washington");
//    [1,1,4],[1,1,5]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 4, "Cincinnati");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 5, "Harvard");
//    [1,1,6],[1,1,7]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 6, "Michigan St");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 7, "Delaware");
//    [1,1,8],[1,1,9]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 8, "North Carolina");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 9, "Providence");
//    [1,1,10],[1,1,11]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 10, "Iowa St");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 11, "NC Central");
//    [1,1,12],[1,1,13]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 12, "UConn");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 13, "Saint Josephs");
//    [1,1,14],[1,1,15]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 14, "Villanova");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 15, "Milwaukee");
    }

    // For 2013-14 Region 2 is the West
    private void createRoundOneMappingsRegion2(final Map<TeamCoordinate, NcaaBbSeasonData> coordinateSeasonDataMap) {
//    [2,1,0],[2,1,1]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 0, "Arizona");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 1, "Weber St");
//    [2,1,2],[2,1,3]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 2, "Gonzaga");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 3, "Oklahoma St");
//    [2,1,4],[2,1,5]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 4, "Oklahoma");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 5, "North Dakota St");
//    [2,1,6],[2,1,7]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 6, "San Diego St");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 7, "New Mexico St");
//    [2,1,8],[2,1,9]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 8, "Baylor");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 9, "Nebraska");
//    [2,1,10],[2,1,11]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 10, "Creighton");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 11, "Lafayette");
//    [2,1,12],[2,1,13]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 12, "Oregon");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 13, "BYU");
//    [2,1,14],[2,1,15]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 14, "Wisconsin");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 15, "American");
    }

    // For 2013-14 Region 3 is the Midwest
    private void createRoundOneMappingsRegion3(final Map<TeamCoordinate, NcaaBbSeasonData> coordinateSeasonDataMap) {
//    [3,1,0],[3,1,1]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 0, "Wichita St");
        // [3,1,1] is First Four Winner, cannot initialize this TeamCoordinate (it must be computed)
//    [3,1,2],[3,1,3]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 2, "Kentucky");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 3, "Kansas St");
//    [3,1,4],[3,1,5]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 4, "Saint Louis");
        // [3,1,5] is First Four Winner, cannot initialize this TeamCoordinate (it must be computed)
//    [3,1,6],[3,1,7]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 6, "Louisville");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 7, "Manhattan");
//    [3,1,8],[3,1,9]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 8, "Massachusetts");
        // [3,1,9] is First Four Winner, cannot initialize this TeamCoordinate (it must be computed)
//    [3,1,10],[3,1,11]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 10, "Duke");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 11, "Mercer");
//    [3,1,12],[3,1,13]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 12, "Texas");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 13, "Arizona St");
//    [3,1,14],[3,1,15]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 14, "Michigan");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 15, "Wofford");
    }

    @Override
    public List<Double> seasonDataAsDoubleList(final NcaaBbSeasonData seasonData, final Double resultClass, final boolean isTraining) {
        return NcaaBbSeasonData.asDoubleList(seasonData, resultClass, isTraining);
    }
}
