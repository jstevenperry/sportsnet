package com.jstevenperry.sportsnet.nba.menus;

import com.jstevenperry.sportsnet.framework.menus.AbstractDataElementChoice;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NbaDataElementMenuTest {

    @Test
    @DisplayName("Sanity check - make sure each element index and ordinal differ by one")
    void valuesDifferByOne() {
        NbaDataElementMenu nbaDataElementMenu = new NbaDataElementMenu();

        nbaDataElementMenu.values().forEach(choice -> assertEquals(choice.getOrdinalValue() - choice.getElementIndex(), 1));
    }

    @Test
    @DisplayName("Sanity check - make sure that element index and ordinal values are correct ascending order")
    void valuesAscendCorrectly() {
        NbaDataElementMenu nbaDataElementMenu = new NbaDataElementMenu();

        List<AbstractDataElementChoice> dataElementChoices = nbaDataElementMenu.values();
        for (int index = 0; index < dataElementChoices.size(); index++) {
            AbstractDataElementChoice choice = dataElementChoices.get(index);
            if (index > 0) {
                int previousIndex = index - 1;
                AbstractDataElementChoice previousChoice = dataElementChoices.get(previousIndex);
                assertEquals(choice.getOrdinalValue() - previousChoice.getOrdinalValue(), 1);
                assertEquals(choice.getElementIndex() - previousChoice.getElementIndex(), 1);
            }

            index++;
        }
    }
}