package ui;

import main.HexaSortGame;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class MenuPanel extends JPanel {
    private HexaSortGame mainFrame;

    public MenuPanel(HexaSortGame frame) {
        this.mainFrame = frame;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(new Color(109, 207, 246));

        add(Box.createVerticalStrut(150));
        JLabel title = new JLabel("HEXCELLENT");
        title.setFont(new Font("Segoe UI", Font.BOLD, 80));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(title);

        JLabel sub = new JLabel("Master the Hexagons");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 24));
        sub.setForeground(new Color(255, 255, 255, 200));
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(sub);

        add(Box.createVerticalStrut(100));
        add(createButton("ENDLESS MODE", e -> mainFrame.getFacade().startEndless()));
        add(Box.createVerticalStrut(20));
        add(createButton("LEVEL MODE", e -> mainFrame.getFacade().startLevel(1)));
        add(Box.createVerticalStrut(20));
        add(createButton("EXIT", e -> System.exit(0)));
    }

    private JButton createButton(String text, ActionListener action) {
        JButton btn = new JButton(text);
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setMaximumSize(new Dimension(300, 70));
        btn.setBackground(new Color(46, 204, 113));
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 22));
        btn.setFocusPainted(false);
        btn.addActionListener(action);
        return btn;
    }
}