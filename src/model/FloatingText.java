package model;

import java.awt.*;

public class FloatingText {
    public String text;
    public float x, y;
    public float alpha;
    public Color color;

    public FloatingText(String t, int x, int y, Color c) {
        this.text = t;
        this.x = x;
        this.y = y;
        this.color = c;
        this.alpha = 1.0f;
    }

    public boolean update() {
        y -= 1.0f;
        alpha -= 0.015f;
        return alpha <= 0;
    }

    public void draw(Graphics2D g2) {
        if (alpha <= 0) return;
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g2.setFont(new Font("Segoe UI", Font.BOLD, 24));
        FontMetrics fm = g2.getFontMetrics();
        g2.setColor(new Color(0, 0, 0, 150));
        g2.drawString(text, x - fm.stringWidth(text) / 2 + 2, y + 2);
        g2.setColor(color);
        g2.drawString(text, x - fm.stringWidth(text) / 2, y);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
    }
}