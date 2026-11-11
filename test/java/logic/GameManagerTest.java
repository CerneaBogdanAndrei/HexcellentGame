package logic;

import model.GameMode;
import model.Hex;
import model.Tile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Stack;

import static org.junit.jupiter.api.Assertions.*;

public class GameManagerTest {
    private GameManager gm;

    static class TestObserver implements GameObserver {
        int stateChanged;
        int levelWon;
        int gameOver;

        @Override public void onGameStateChanged() { stateChanged++; }
        @Override public void onLevelWon() { levelWon++; }
        @Override public void onGameOver() { gameOver++; }
    }

    @BeforeEach
    void setUp() throws Exception {
        gm = GameManager.getInstance();
        Field f = GameManager.class.getDeclaredField("observers");
        f.setAccessible(true);
        f.set(gm, new ArrayList<GameObserver>());
        gm.highScore = 0;
        gm.initGame(GameMode.ENDLESS, 1);
    }

    @Test
    void singletonReturnsSameInstance() {
        assertSame(gm, GameManager.getInstance());
    }

    @Test
    void initGameEndlessResetsStateAndCreatesHand() {
        gm.initGame(GameMode.ENDLESS, 1);
        assertEquals(GameMode.ENDLESS, gm.currentMode);
        assertEquals(1, gm.currentLevel);
        assertEquals(0, gm.score);
        assertEquals(0, gm.levelTargetScore);
        assertEquals(5, gm.hintCount);
        assertEquals(3, gm.bombCount);
        assertEquals(3, gm.hand.size());
        assertTrue(gm.hand.stream().allMatch(s -> s != null && !s.isEmpty()));
        assertFalse(gm.grid.isEmpty());
    }

    @Test
    void initGameLevelsSetsTargetAndResources() {
        gm.initGame(GameMode.LEVELS, 1);
        assertEquals(GameMode.LEVELS, gm.currentMode);
        assertEquals(1, gm.currentLevel);
        assertEquals(0, gm.score);
        assertEquals(500, gm.levelTargetScore);
        assertEquals(2, gm.hintCount);
        assertEquals(1, gm.bombCount);
    }

    @Test
    void addScoreUpdatesScoreAndHighScoreInEndless() {
        gm.initGame(GameMode.ENDLESS, 1);
        gm.highScore = 0;
        gm.addScore(7);
        assertEquals(70, gm.score);
        assertEquals(70, gm.highScore);
    }

    @Test
    void levelWonIsTriggeredWhenTargetReached() {
        gm.initGame(GameMode.LEVELS, 1);
        TestObserver obs = new TestObserver();
        gm.addObserver(obs);
        gm.addScore(50);
        assertTrue(obs.levelWon > 0);
    }

    @Test
    void checkRefillRegeneratesHandWhenAllStacksEmpty() {
        gm.initGame(GameMode.ENDLESS, 1);
        for (Stack<Tile> s : gm.hand) s.clear();
        gm.checkRefill();
        assertEquals(3, gm.hand.size());
        assertTrue(gm.hand.stream().allMatch(s -> s != null && !s.isEmpty()));
    }

    @Test
    void checkGameOverTriggersWhenGridIsFull() {
        gm.initGame(GameMode.ENDLESS, 1);
        TestObserver obs = new TestObserver();
        gm.addObserver(obs);

        for (Hex h : gm.grid.keySet()) {
            Stack<Tile> s = gm.grid.get(h);
            if (s.isEmpty()) s.push(new Tile(Color.RED));
        }

        gm.checkGameOver();
        assertTrue(obs.gameOver > 0);
    }
}
