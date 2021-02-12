package com.jstevenperry.sportsnet.ncaabb.plugins;

import com.jstevenperry.sportsnet.framework.model.TeamCoordinate;
import com.jstevenperry.sportsnet.framework.persistence.SeasonDataDao;
import com.jstevenperry.sportsnet.ncaabb.model.NcaaBbSeasonData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TournamentRunnerPlugin2008 extends AbstractNcaaBbTournamentRunnerPlugin {

    public TournamentRunnerPlugin2008(final SeasonDataDao<NcaaBbSeasonData> seasonDataDao) {
        super(seasonDataDao);
        setTeamCoordinateSeasonDataMap(buildTeamCoordinateMap());
    }

    @Override
    public Integer getTournamentYear() {
        return 2008;
    }

    @Override
    public String getTournamentYearForDisplay() {
        return "2008-09";
    }

    @Override
    public String getTournamentTemplateFileName() {
        return "/ncaabb/2008/tournament-template-ncaabb.txt";
    }

    @Override
    protected String getTournamentResultsTemplateFileName() {
        return "/ncaabb/2008/tournament-results-template.csv";
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
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 0, 0, "Morehead St");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 0, 1, "Alabama St");
    }

    // For 2013-14 Region 0 is the South
    private void createRoundOneMappingsRegion0(final Map<TeamCoordinate, NcaaBbSeasonData> coordinateSeasonDataMap) {
//    [0,1,0],[0,1,1]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 0, "Louisville");
        // [0,1,1] is First Four Winner, cannot initialize this TeamCoordinate (it must be computed)
//    [0,1,2],[0,1,3]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 2, "Ohio St");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 3, "Siena");
//    [0,1,4],[0,1,5]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 4, "Utah");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 5, "Arizona");
//    [0,1,6],[0,1,7]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 6, "Wake Forest");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 7, "Cleveland St");
//    [0,1,8],[0,1,9]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 8, "West Virginia");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 9, "Dayton");
//    [0,1,10],[0,1,11]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 10, "Kansas");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 11, "North Dakota St");
//    [0,1,12],[0,1,13]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 12, "Boston College");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 13, "Southern California");
//    [0,1,14],[0,1,15]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 14, "Robert Morris");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 15, "Michigan St");
    }

    // For 2013-14 Region 1 is the East
    private void createRoundOneMappingsRegion1(final Map<TeamCoordinate, NcaaBbSeasonData> coordinateSeasonDataMap) {
//    [1,1,0],[1,1,1]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 0, "UConn");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 1, "Chattanooga");
//    [1,1,2],[1,1,3]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 2, "BYU");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 3, "Texas AM");
//    [1,1,4],[1,1,5]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 4, "Purdue");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 5, "UNI");
//    [1,1,6],[1,1,7]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 6, "Washington");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 7, "Mississippi St");
//    [1,1,8],[1,1,9]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 8, "Marquette");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 9, "Utah St");
//    [1,1,10],[1,1,11]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 10, "Missouri");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 11, "Cornell");
//    [1,1,12],[1,1,13]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 12, "California");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 13, "Maryland");
//    [1,1,14],[1,1,15]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 14, "Memphis");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 15, "CSUN");
    }

    // For 2013-14 Region 2 is the West
    private void createRoundOneMappingsRegion2(final Map<TeamCoordinate, NcaaBbSeasonData> coordinateSeasonDataMap) {
//    [2,1,0],[2,1,1]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 0, "Pittsburgh");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 1, "ETSU");
//    [2,1,2],[2,1,3]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 2, "Oklahoma St");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 3, "Tennessee");
//    [2,1,4],[2,1,5]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 4, "Florida St");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 5, "Wisconsin");
//    [2,1,6],[2,1,7]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 6, "Xavier");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 7, "Portland St");
//    [2,1,8],[2,1,9]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 8, "UCLA");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 9, "VCU");
//    [2,1,10],[2,1,11]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 10, "Villanova");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 11, "American");
//    [2,1,12],[2,1,13]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 12, "Texas");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 13, "Minnesota");
//    [2,1,14],[2,1,15]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 14, "Duke");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 15, "Binghamton");
    }

    // For 2013-14 Region 3 is the Midwest
    private void createRoundOneMappingsRegion3(final Map<TeamCoordinate, NcaaBbSeasonData> coordinateSeasonDataMap) {
//    [3,1,0],[3,1,1]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 0, "North Carolina");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 1, "Radford");
//    [3,1,2],[3,1,3]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 2, "LSU");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 3, "Butler");
//    [3,1,4],[3,1,5]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 4, "Illinois");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 5, "Western Ky");
//    [3,1,6],[3,1,7]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 6, "Gonzaga");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 7, "Akron");
//    [3,1,8],[3,1,9]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 8, "Arizona St");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 9, "Temple");
//    [3,1,10],[3,1,11]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 10, "Syracuse");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 11, "SFA");
//    [3,1,12],[3,1,13]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 12, "Clemson");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 13, "Michigan");
//    [3,1,14],[3,1,15]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 14, "Oklahoma");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 15, "Morgan St");
    }

    @Override
    public List<Double> seasonDataAsDoubleList(final NcaaBbSeasonData seasonData, final Double resultClass, final boolean isTraining) {
        return NcaaBbSeasonData.asDoubleList(seasonData, resultClass, isTraining);
    }
}
