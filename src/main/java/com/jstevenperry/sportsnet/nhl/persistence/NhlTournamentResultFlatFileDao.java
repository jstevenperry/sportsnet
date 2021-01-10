package com.jstevenperry.sportsnet.nhl.persistence;

import com.jstevenperry.sportsnet.framework.model.TournamentResult;
import com.jstevenperry.sportsnet.framework.persistence.AbstractTournamentResultFlatFileDao;
import com.jstevenperry.sportsnet.nhl.model.NhlSeasonData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("nhlTournamentResultDao")
public class NhlTournamentResultFlatFileDao extends AbstractTournamentResultFlatFileDao<NhlSeasonData> {

  private static final Logger log = LoggerFactory.getLogger(NhlTournamentResultFlatFileDao.class);

  @Override
  public List<TournamentResult> fetchAllByYear(final Integer year) {
    return null;
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
