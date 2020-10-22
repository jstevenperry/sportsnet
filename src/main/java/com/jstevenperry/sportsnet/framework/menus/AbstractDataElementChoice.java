package com.jstevenperry.sportsnet.framework.menus;

import java.io.Serializable;

public abstract class AbstractDataElementChoice implements Serializable {
  /**
   * Return the index of this Data Element Menu Choice in its
   * corresponding INDArray of features.
   */
  public abstract int getElementIndex();

  public abstract int getOrdinalValue();

  public abstract String getName();

}
