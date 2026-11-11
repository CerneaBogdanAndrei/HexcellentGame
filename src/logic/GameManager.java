package logic;

import logic.strategies.ComboScoring;
import logic.strategies.ScoringStrategy;
import logic.strategies.StandardScoring;
import model.GameMode;
import model.Hex;
import model.Tile;

import java.util.*;

public class GameManager {
    private static GameManager instance;
    private List<GameObserver> observers = new ArrayList<>();

    public Map<Hex, Stack<Tile>> grid = new HashMap<>();
    public List<Stack<Tile>> hand = new ArrayList<>();
    public int score = 0;
    public int highScore = 0;
    public int currentLevel = 1;
    public GameMode currentMode;
    public int levelTargetScore = 0;

    public int hintCount = 2;
    public int bombCount = 2;

    private ScoringStrategy scoringStrategy;

    private GameManager() { scoringStrategy = new StandardScoring(); }

    public static GameManager getInstance() {
        if (instance == null) instance = new GameManager();
        return instance;
    }

    public void addObserver(GameObserver o) { observers.add(o); }

    public void initGame(GameMode mode, int level) {
        this.currentMode = mode;
        this.currentLevel = level;
        this.score = 0;

        if (mode == GameMode.LEVELS) {
            scoringStrategy = new ComboScoring();
            hintCount = 2; bombCount = 1;
            int[] targets = { 500, 1000, 2000, 3500, 5000, 7000, 10000, 15000, 20000, 30000 };
            int idx = Math.min(level, targets.length) - 1;
            levelTargetScore = targets[idx];
        } else {
            scoringStrategy = new StandardScoring();
            hintCount = 5; bombCount = 3;
            levelTargetScore = 0;
        }

        grid.clear();
        hand.clear();
        initGridTopology();
        refillHand();
        notifyUpdate();
    }

    public void addScore(int tilesRemoved) {
        int points = scoringStrategy.calculateScore(tilesRemoved);
        this.score += points;
        if (this.score > this.highScore) this.highScore = this.score;

        if (currentMode == GameMode.LEVELS && score >= levelTargetScore) {
            for (GameObserver o : observers) o.onLevelWon();
        }
    }

    public void useHint() { if (hintCount > 0) hintCount--; notifyUpdate(); }
    public void useBomb() { if (bombCount > 0) bombCount--; notifyUpdate(); }

    private void initGridTopology() {
        int radius = 2;
        for (int q = -radius; q <= radius; q++) {
            int r1 = Math.max(-radius, -q - radius);
            int r2 = Math.min(radius, -q + radius);
            for (int r = r1; r <= r2; r++) grid.put(new Hex(q, r), new Stack<>());
        }
    }

    public void refillHand() {
        hand.clear();
        for (int i = 0; i < 3; i++) hand.add(TileFactory.createRandomStack());
    }

    public void checkRefill() {
        boolean empty = true;
        for (Stack<Tile> s : hand) if (!s.isEmpty()) empty = false;
        if (empty) refillHand();
    }

    public void checkGameOver() {
        boolean isFull = true;
        for (Stack<Tile> s : grid.values()) if (s.isEmpty()) { isFull = false; break; }
        if (isFull) for (GameObserver o : observers) o.onGameOver();
    }

    public void notifyUpdate() {
        for (GameObserver o : observers) o.onGameStateChanged();
    }
}
