package com.jstevenperry.sportsnet.ncaabb.plugins;

import com.jstevenperry.sportsnet.framework.model.TeamCoordinate;
import com.jstevenperry.sportsnet.framework.persistence.SeasonDataDao;
import com.jstevenperry.sportsnet.ncaabb.model.NcaaBbSeasonData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TournamentRunnerPlugin2016 extends AbstractNcaaBbTournamentRunnerPlugin {

    public TournamentRunnerPlugin2016(final SeasonDataDao<NcaaBbSeasonData> seasonDataDao) {
        super(seasonDataDao);
        setTeamCoordinateSeasonDataMap(buildTeamCoordinateMap());
    }

    @Override
    public Integer getTournamentYear() {
        return 2016;
    }

    @Override
    public String getTournamentTemplateFileName() {
        return "/ncaabb/2016/tournament-template-ncaabb.txt";
    }

    @Override
    protected String getTournamentResultsTemplateFileName() {
        return "/ncaabb/2016/tournament-results-template.csv";
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
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 0, 1, "New Orleans");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 0, 8, "Providence");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 0, 9, "Southern California");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 0, 0, "NC Central");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 0, 1, "UC Davis");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 0, 8, "Kansas St");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 0, 9, "Wake Forest");
    }

    // For 2016-17 Region 0 is the East
    private void createRoundOneMappingsRegion0(final Map<TeamCoordinate, NcaaBbSeasonData> coordinateSeasonDataMap) {
//    [0,1,0],[0,1,1]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 0, "Villanova");
        // [0,1,1] is First Four Winner, cannot initialize this TeamCoordinate (it must be computed)
//    [0,1,2],[0,1,3]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 2, "Wisconsin");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 3, "Virginia Tech");
//    [0,1,4],[0,1,5]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 4, "Virginia");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 5, "UNCW");
//    [0,1,6],[0,1,7]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 6, "Florida");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 7, "ETSU");
//    [0,1,8],[0,1,9]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 8, "SMU");
        // [0,1,9] is First Four Winner, cannot initialize this TeamCoordinate (it must be computed)
//    [0,1,10],[0,1,11]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 10, "Baylor");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 11, "New Mexico St");
//    [0,1,12],[0,1,13]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 12, "South Carolina");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 13, "Marquette");
//    [0,1,14],[0,1,15]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 14, "Duke");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 0, 1, 15, "Troy");
    }

    // For 2016-17 Region 1 is the West
    private void createRoundOneMappingsRegion1(final Map<TeamCoordinate, NcaaBbSeasonData> coordinateSeasonDataMap) {
//    [1,1,0],[1,1,1]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 0, "Gonzaga");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 1, "South Dakota St");
//    [1,1,2],[1,1,3]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 2, "Northwestern");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 3, "Vanderbilt");
//    [1,1,4],[1,1,5]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 4, "Notre Dame");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 5, "Princeton");
//    [1,1,6],[1,1,7]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 6, "West Virginia");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 7, "Bucknell");
//    [1,1,8],[1,1,9]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 8, "Maryland");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 9, "Xavier");
//    [1,1,10],[1,1,11]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 10, "Florida St");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 11, "FGCU");
//    [1,1,12],[1,1,13]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 12, "Saint Marys CA");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 13, "VCU");
//    [1,1,14],[1,1,15]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 14, "Arizona");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 1, 1, 15, "North Dakota");
    }

    // For 2016-17 Region 2 is the Midwest
    private void createRoundOneMappingsRegion2(final Map<TeamCoordinate, NcaaBbSeasonData> coordinateSeasonDataMap) {
//    [2,1,0],[2,1,1]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 0, "Kansas");
        // [2,1,1] is First Four Winner, cannot initialize this TeamCoordinate (it must be computed)
//    [2,1,2],[2,1,3]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 2, "Miami FL");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 3, "Michigan St");
//    [2,1,4],[2,1,5]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 4, "Iowa St");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 5, "Nevada");
//    [2,1,6],[2,1,7]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 6, "Purdue");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 7, "Vermont");
//    [2,1,8],[2,1,9]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 8, "Creighton");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 9, "Rhode Island");
//    [2,1,10],[2,1,11]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 10, "Oregon");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 11, "Iona");
//    [2,1,12],[2,1,13]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 12, "Michigan");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 13, "Oklahoma St");
//    [2,1,14],[2,1,15]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 14, "Louisville");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 2, 1, 15, "Jacksonville St");
    }

    // For 2016-17 Region 3 is the South
    private void createRoundOneMappingsRegion3(final Map<TeamCoordinate, NcaaBbSeasonData> coordinateSeasonDataMap) {
//    [3,1,0],[3,1,1]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 0, "North Carolina");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 1, "Texas Southern");
//    [3,1,2],[3,1,3]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 2, "Arkansas");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 3, "Seton Hall");
//    [3,1,4],[3,1,5]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 4, "Minnesota");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 5, "Middle Tenn");
//    [3,1,6],[3,1,7]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 6, "Butler");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 7, "Winthrop");
//    [3,1,8],[3,1,9]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 8, "Cincinnati");
        // [3,1,9] is First Four Winner, cannot initialize this TeamCoordinate (it must be computed)
//    [3,1,10],[3,1,11]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 10, "UCLA");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 11, "Kent St");
//    [3,1,12],[3,1,13]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 12, "Dayton");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 13, "Wichita St");
//    [3,1,14],[3,1,15]
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 14, "Kentucky");
        createTeamCoordinateMapping(coordinateSeasonDataMap, 3, 1, 15, "Northern Ky");
    }

    @Override
    public List<Double> seasonDataAsDoubleList(final NcaaBbSeasonData seasonData, final Double resultClass, final boolean isTraining) {
        return NcaaBbSeasonData.asDoubleList(seasonData, resultClass, isTraining);
    }
}
