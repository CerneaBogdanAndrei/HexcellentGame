package logic.strategies;

public class ComboScoring implements ScoringStrategy {
    @Override
    public int calculateScore(int tilesCleared) {
        return (tilesCleared * 10) + (tilesCleared > 5 ? 50 : 0);
    }
}