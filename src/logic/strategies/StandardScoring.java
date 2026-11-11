package logic.strategies;

public class StandardScoring implements ScoringStrategy {
    @Override
    public int calculateScore(int tilesCleared) {
        return tilesCleared * 10;
    }
}