package com.jstevenperry.sportsnet.nhl.persistence;

import com.jstevenperry.sportsnet.framework.persistence.AbstractTournamentResultFlatFileDao;
import com.jstevenperry.sportsnet.nhl.model.NhlSeasonData;
import org.springframework.stereotype.Component;

@Component("nhlTournamentResultDao")
public class NhlTournamentResultFlatFileDao extends AbstractTournamentResultFlatFileDao<NhlSeasonData> {

  @Override
  protected int getNumberOfFields() {
    return 4;
  }

  @Override
  protected String[][] getDataFileNames() {
    return new String[][] {
            {"2011", "/nhl/2010-11-tournament_result.csv"},
            {"2012", "/nhl/2011-12-tournament_result.csv"},
            {"2013", "/nhl/2012-13-tournament_result.csv"},
            {"2014", "/nhl/2013-14-tournament_result.csv"},
            {"2015", "/nhl/2014-15-tournament_result.csv"},
            {"2016", "/nhl/2015-16-tournament_result.csv"},
            {"2017", "/nhl/2016-17-tournament_result.csv"},
            {"2018", "/nhl/2017-18-tournament_result.csv"},
            {"2019", "/nhl/2018-19-tournament_result.csv"},
            {"2020", "/nhl/2019-20-tournament_result.csv"},
    };
  }
}
