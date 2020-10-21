package com.jstevenperry.sportsnet.framework.menus;

public abstract class AbstractDataElementChoice {
  /**
   * Return the index of this Data Element Menu Choice in its
   * corresponding INDArray of features.
   */
  public abstract int getElementIndex();

  public abstract int getOrdinalValue();

  public abstract String getName();

}
