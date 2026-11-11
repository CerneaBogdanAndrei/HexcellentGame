package main;

import logic.GameManager;
import ui.GameFacade;
import ui.GamePanel;
import ui.MenuPanel;

import javax.swing.*;
import java.awt.*;

public class HexaSortGame extends JFrame {
    private CardLayout cardLayout;
    private JPanel mainContainer;
    private GameFacade gameFacade;

    public HexaSortGame() {
        setTitle("Hexcellent");
        setSize(1000, 900);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        mainContainer = new JPanel(cardLayout);

        // Pasam 'this' pentru a evita problemele de casting
        GamePanel gamePanel = new GamePanel(this);
        MenuPanel menuPanel = new MenuPanel(this);

        GameManager.getInstance().addObserver(gamePanel);

        gameFacade = new GameFacade(cardLayout, mainContainer, gamePanel);

        mainContainer.add(menuPanel, "MENU");
        mainContainer.add(gamePanel, "GAME");

        add(mainContainer);
        cardLayout.show(mainContainer, "MENU");
    }

    public GameFacade getFacade() { return gameFacade; }

    public static void main(String[] args) {
        System.setProperty("sun.java2d.opengl", "true");
        SwingUtilities.invokeLater(() -> new HexaSortGame().setVisible(true));
    }
}