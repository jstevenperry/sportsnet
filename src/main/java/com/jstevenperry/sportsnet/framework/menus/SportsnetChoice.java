package com.jstevenperry.sportsnet.framework.menus;

public enum SportsnetChoice {
  UNKNOWN("Unknown", 0),
  NCAA_MENS_BASKETBALL("NCAA Division I Men's Basketball", 1),
  NHL("National Hockey League", 2)
  ;

  private final String displayName;
  private final int ordinalValue;

  SportsnetChoice(final String displayName, final int ordinalValue) {
    this.displayName = displayName;
    this.ordinalValue = ordinalValue;
  }

  public String getDisplayName() { return this.displayName; }
  public int getOrdinalValue() { return this.ordinalValue; }

  public static SportsnetChoice from(final int ordinalValueCandiate) {
    SportsnetChoice ret = SportsnetChoice.UNKNOWN;
    for (SportsnetChoice mainNetworkMenuChoice : values()) {
      if (mainNetworkMenuChoice.getOrdinalValue() == ordinalValueCandiate) {
        ret = mainNetworkMenuChoice;
        break;
      }
    }
    return ret;
  }

  public boolean isSuitableForDisplay() {
    return this != SportsnetChoice.UNKNOWN;
  }
}
