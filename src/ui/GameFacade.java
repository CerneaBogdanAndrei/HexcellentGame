package ui;

import logic.GameManager;
import model.GameMode;
import javax.swing.*;
import java.awt.*;

public class GameFacade {
    private CardLayout cardLayout;
    private JPanel container;
    private GamePanel gamePanel;
    private GameManager manager;

    public GameFacade(CardLayout l, JPanel c, GamePanel gp) {
        this.cardLayout = l;
        this.container = c;
        this.gamePanel = gp;
        this.manager = GameManager.getInstance();
    }

    public void startEndless() {
        manager.initGame(GameMode.ENDLESS, 1);
        gamePanel.resetAnimations();
        cardLayout.show(container, "GAME");
    }

    public void startLevel(int level) {
        manager.initGame(GameMode.LEVELS, level);
        gamePanel.resetAnimations();
        cardLayout.show(container, "GAME");
    }

    public void showMenu() {
        cardLayout.show(container, "MENU");
    }

    public void restartCurrent() {
        manager.initGame(manager.currentMode, manager.currentLevel);
        gamePanel.resetAnimations();
    }

    public void nextLevel() {
        manager.initGame(GameMode.LEVELS, manager.currentLevel + 1);
        gamePanel.resetAnimations();
    }
}