package logic;

public interface GameObserver {
    void onGameStateChanged();
    void onLevelWon();
    void onGameOver();
}