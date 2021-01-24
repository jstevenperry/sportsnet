package com.jstevenperry.sportsnet.nba.persistence;

import com.jstevenperry.sportsnet.framework.persistence.AbstractTournamentResultFlatFileDao;
import com.jstevenperry.sportsnet.nba.model.NbaSeasonData;
import org.springframework.stereotype.Component;

@Component("nbaTournamentResultDao")
public class NbaTournamentResultFlatFileDao extends AbstractTournamentResultFlatFileDao<NbaSeasonData> {

    @Override
    protected int getNumberOfFields() {
        return 4;
    }

    @Override
    protected String[][] getDataFileNames() {
        return new String[][] {
                {"2010", "/nba/2010-11-Playoff-Results.csv"},
                {"2011", "/nba/2011-12-Playoff-Results.csv"},
                {"2012", "/nba/2012-13-Playoff-Results.csv"},
                {"2013", "/nba/2013-14-Playoff-Results.csv"},
                {"2014", "/nba/2014-15-Playoff-Results.csv"},
                {"2015", "/nba/2015-16-Playoff-Results.csv"},
                {"2016", "/nba/2016-17-Playoff-Results.csv"},
                {"2017", "/nba/2017-18-Playoff-Results.csv"},
                {"2018", "/nba/2018-19-Playoff-Results.csv"},
                {"2019", "/nba/2019-20-Playoff-Results.csv"},
        };
    }
}
