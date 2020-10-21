package com.jstevenperry.sportsnet.ncaabb.menus;

import com.jstevenperry.sportsnet.framework.menus.AbstractDataElementChoice;

public class DataElementChoice extends AbstractDataElementChoice {

  private final String name;
  private final int elementIndex;
  private final int ordinalValue;

  public DataElementChoice(final String name, final int elementIndex, final int ordinalValue) {

    this.name = name;
    this.elementIndex = elementIndex;
    this.ordinalValue = ordinalValue;
  }

  @Override
  public int getElementIndex() {
    return elementIndex;
  }

  @Override
  public int getOrdinalValue() {
    return ordinalValue;
  }

  @Override
  public String getName() {
    return name;
  }
}
