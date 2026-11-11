package logic.strategies;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ScoringStrategyTest {
    @Test
    void standardScoringIsTilesTimesTen() {
        ScoringStrategy s = new StandardScoring();
        assertEquals(0, s.calculateScore(0));
        assertEquals(10, s.calculateScore(1));
        assertEquals(70, s.calculateScore(7));
    }

    @Test
    void comboScoringAddsBonusWhenTilesGreaterThanFive() {
        ScoringStrategy s = new ComboScoring();
        assertEquals(50, s.calculateScore(5));
        assertEquals(110, s.calculateScore(6));
    }
}
