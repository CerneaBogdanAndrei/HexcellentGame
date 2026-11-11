package ui;

import logic.GameManager;
import logic.GameObserver;
import main.HexaSortGame;
import model.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;
import javax.swing.Timer;

public class GamePanel extends JPanel implements GameObserver {
    private HexaSortGame mainFrame;
    private GameManager manager;

    private final int HEX_SIZE = 50;
    private final int HEX_DRAW_SIZE = 42;
    private final int TILE_THICKNESS = 8;
    private final double ANIMATION_SPEED = 0.08;
    private final int MAX_STACK_CLEAR = 10;

    private int selectedHandIndex = -1;
    private List<AnimTile> activeAnimations = new ArrayList<>();
    private List<FloatingText> floatingTexts = new ArrayList<>();
    private Timer gameLoop;
    private Hex pendingMergeTarget = null;

    private boolean isBombActive = false;
    private boolean isGameOver = false;
    private boolean isLevelWon = false;

    private int hintHandIdx = -1;
    private Hex hintTargetHex = null;

    private Rectangle actionBtnBounds, menuBtnBounds, hintBtnBounds, bombBtnBounds;

    public GamePanel(HexaSortGame frame) {
        this.mainFrame = frame;
        this.manager = GameManager.getInstance();

        setBackground(new Color(109, 207, 246));
        gameLoop = new Timer(16, e -> updateVisuals());
        gameLoop.start();

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                handleMouse(e);
            }
        });
    }

    public void resetAnimations() {
        activeAnimations.clear();
        floatingTexts.clear();
        selectedHandIndex = -1;
        pendingMergeTarget = null;
        isGameOver = false;
        isLevelWon = false;
        isBombActive = false;
        clearHint();
        repaint();
    }

    @Override
    public void onGameStateChanged() { repaint(); }
    @Override
    public void onLevelWon() { isLevelWon = true; repaint(); }
    @Override
    public void onGameOver() { isGameOver = true; repaint(); }

    private void handleMouse(MouseEvent e) {
        int x = e.getX(); int y = e.getY();

        if (menuBtnBounds != null && menuBtnBounds.contains(x, y)) {
            mainFrame.getFacade().showMenu();
            return;
        }

        if (!isGameOver && !isLevelWon) {
            if (hintBtnBounds != null && hintBtnBounds.contains(x, y)) { activateHint(); return; }
            if (bombBtnBounds != null && bombBtnBounds.contains(x, y)) { toggleBombMode(); return; }
        }

        if (isGameOver || isLevelWon) {
            if (actionBtnBounds != null && actionBtnBounds.contains(x, y)) {
                if(isGameOver) mainFrame.getFacade().restartCurrent();
                else if(isLevelWon) mainFrame.getFacade().nextLevel();
            }
        } else {
            if (activeAnimations.isEmpty() && pendingMergeTarget == null) {
                processGameInput(x, y);
            }
        }
    }

    private void processGameInput(int x, int y) {
        if (isBombActive) {
            Hex target = getHexAtPosition(x, y);
            if (target != null && manager.grid.containsKey(target) && !manager.grid.get(target).isEmpty()) {
                manager.grid.get(target).clear();
                manager.useBomb();
                Point p = hexToPixel(target);
                floatingTexts.add(new FloatingText("BOOM!", p.x, p.y, Color.RED));
                isBombActive = false;
                manager.checkRefill();
            } else {
                int clickedHand = getHandIndexAtPosition(x, y);
                if (clickedHand != -1) isBombActive = false;
            }
            return;
        }

        if (hintHandIdx != -1) clearHint();

        int clickedHandIndex = getHandIndexAtPosition(x, y);
        if (clickedHandIndex != -1) {
            selectedHandIndex = clickedHandIndex;
            repaint();
            return;
        }

        if (selectedHandIndex != -1) {
            Hex target = getHexAtPosition(x, y);
            if (target != null && manager.grid.containsKey(target) && manager.grid.get(target).isEmpty()) {
                startHandToGridAnimation(target);
            } else {
                selectedHandIndex = -1;
                repaint();
            }
        }
    }

    private void updateVisuals() {
        boolean repaintNeeded = false;
        if (!activeAnimations.isEmpty()) {
            List<AnimTile> finished = new ArrayList<>();
            for (AnimTile anim : activeAnimations) {
                anim.progress += ANIMATION_SPEED;
                double t = anim.progress; double eased = t*t*(3-2*t);
                anim.x = anim.start.x + (anim.end.x - anim.start.x) * eased;
                anim.y = anim.start.y + (anim.end.y - anim.start.y) * eased;
                if (anim.progress >= 1.0) { anim.progress = 1.0; finished.add(anim); }
            }
            if (finished.size() == activeAnimations.size()) {
                List<AnimTile> copy = new ArrayList<>(activeAnimations); activeAnimations.clear(); onAnimationsFinished(copy);
            }
            repaintNeeded = true;
        } else if (pendingMergeTarget == null && !isGameOver && !isLevelWon) {
            if (checkIdleMerges()) repaintNeeded = true;
            else manager.checkGameOver();
        }

        Iterator<FloatingText> it = floatingTexts.iterator();
        while (it.hasNext()) {
            if (it.next().update()) it.remove();
            repaintNeeded = true;
        }
        if (repaintNeeded) repaint();
    }

    private void startHandToGridAnimation(Hex target) {
        Stack<Tile> handStack = manager.hand.get(selectedHandIndex);
        Point targetPoint = hexToPixel(target);
        int slotWidth = getWidth() / 3;
        Point startPoint = new Point(selectedHandIndex * slotWidth + slotWidth / 2, getHeight() - 120);
        pendingMergeTarget = target;
        for (int i = 0; i < handStack.size(); i++) activeAnimations.add(new AnimTile(handStack.get(i), startPoint, targetPoint, i));
        manager.hand.set(selectedHandIndex, new Stack<>());
        selectedHandIndex = -1;
    }

    private void onAnimationsFinished(List<AnimTile> finishedTiles) {
        Stack<Tile> targetStack = manager.grid.get(pendingMergeTarget);
        if (targetStack != null) for (AnimTile anim : finishedTiles) targetStack.push(anim.tile);
        checkAndStartMergeAnimations(pendingMergeTarget);
    }

    private void checkAndStartMergeAnimations(Hex center) {
        Stack<Tile> centerStack = manager.grid.get(center);
        if (centerStack == null || centerStack.isEmpty()) { pendingMergeTarget = null; manager.checkRefill(); return; }
        Color targetColor = centerStack.peek().color;
        boolean foundMerge = false; Point centerPixel = hexToPixel(center); int currentHeight = centerStack.size();
        for (Hex n : getNeighbors(center)) {
            if (!manager.grid.containsKey(n)) continue; Stack<Tile> nStack = manager.grid.get(n);
            if (!nStack.isEmpty() && nStack.peek().color.equals(targetColor)) {
                foundMerge = true; Point startPixel = hexToPixel(n);
                while (!nStack.isEmpty() && nStack.peek().color.equals(targetColor)) {
                    activeAnimations.add(new AnimTile(nStack.pop(), startPixel, centerPixel, currentHeight++));
                }
            }
        }
        if (!foundMerge) checkClearCondition(center);
    }

    private void checkClearCondition(Hex center) {
        Stack<Tile> stack = manager.grid.get(center);
        if (stack.isEmpty()) { pendingMergeTarget = null; manager.checkRefill(); return; }
        Color c = stack.peek().color; int count = 0;
        for (int i = stack.size() - 1; i >= 0; i--) { if (stack.get(i).color.equals(c)) count++; else break; }
        if (count >= MAX_STACK_CLEAR) {
            manager.addScore(count);
            floatingTexts.add(new FloatingText("+" + (count*10), hexToPixel(center).x, hexToPixel(center).y - 50, Color.WHITE));
            for (int i = 0; i < count; i++) stack.pop();
            manager.checkRefill(); pendingMergeTarget = null;
        } else { pendingMergeTarget = null; manager.checkRefill(); }
    }

    private void toggleBombMode() { if(manager.bombCount > 0) { isBombActive = !isBombActive; clearHint(); repaint(); } }
    private void activateHint() {
        if(manager.hintCount > 0) {
            isBombActive = false; findBestMove();
            if(hintHandIdx != -1) { manager.useHint(); repaint(); }
        }
    }
    private void clearHint() { hintHandIdx = -1; hintTargetHex = null; }

    private void findBestMove() {
        int bestScore = -1; int bestHIdx = -1; Hex bestTarget = null;
        for (int i = 0; i < manager.hand.size(); i++) {
            Stack<Tile> hStack = manager.hand.get(i);
            if (hStack.isEmpty()) continue;
            Color hColor = hStack.peek().color;
            int handSize = hStack.size();
            for (Hex gridHex : manager.grid.keySet()) {
                if (!manager.grid.get(gridHex).isEmpty()) continue;
                int currentScore = 0; boolean isMerge = false; int potentialStackHeight = handSize;
                for (Hex n : getNeighbors(gridHex)) {
                    if (manager.grid.containsKey(n) && !manager.grid.get(n).isEmpty()) {
                        Stack<Tile> neighborStack = manager.grid.get(n);
                        if (neighborStack.peek().color.equals(hColor)) {
                            isMerge = true; currentScore += 10;
                            int matchingInNeighbor = 0;
                            for(int k=neighborStack.size()-1; k>=0; k--) { if(neighborStack.get(k).color.equals(hColor)) matchingInNeighbor++; else break; }
                            currentScore += matchingInNeighbor; potentialStackHeight += matchingInNeighbor;
                        }
                    }
                }
                if (potentialStackHeight >= MAX_STACK_CLEAR) currentScore += 50;
                if (isMerge && currentScore > bestScore) { bestScore = currentScore; bestHIdx = i; bestTarget = gridHex; }
            }
        }
        if (bestHIdx != -1) { hintHandIdx = bestHIdx; hintTargetHex = bestTarget; return; }
        for (int i = 0; i < manager.hand.size(); i++) {
            if (manager.hand.get(i).isEmpty()) continue;
            for (Hex gridHex : manager.grid.keySet()) {
                if (manager.grid.get(gridHex).isEmpty()) { hintHandIdx = i; hintTargetHex = gridHex; return; }
            }
        }
    }

    private boolean checkIdleMerges() {
        List<Hex> sortedKeys = new ArrayList<>(manager.grid.keySet());
        sortedKeys.sort((h1,h2)->{if(h1.r!=h2.r)return Integer.compare(h1.r,h2.r); return Integer.compare(h1.q,h2.q);});
        for (Hex center : sortedKeys) {
            Stack<Tile> centerStack = manager.grid.get(center); if (centerStack.isEmpty()) continue;
            Color targetColor = centerStack.peek().color;
            boolean foundSomething = false;
            for (Hex n : getNeighbors(center)) {
                if (!manager.grid.containsKey(n)) continue; Stack<Tile> nStack = manager.grid.get(n);
                if (!nStack.isEmpty() && nStack.peek().color.equals(targetColor)) { foundSomething = true; break; }
            }
            if (foundSomething) { pendingMergeTarget = center; checkAndStartMergeAnimations(center); return true; }
        }
        return false;
    }

    private Point hexToPixel(Hex h) { double x=HEX_SIZE*(Math.sqrt(3)*(h.q+h.r/2.0)); double y=HEX_SIZE*(3.0/2.0*h.r); return new Point((int)(x+getWidth()/2), (int)(y+getHeight()/2-100)); }
    private List<Hex> getNeighbors(Hex h) { List<Hex> l=new ArrayList<>(); int[][] d={{1,0},{1,-1},{0,-1},{-1,0},{-1,1},{0,1}}; for(int[] k:d) l.add(new Hex(h.q+k[0],h.r+k[1])); return l; }
    private Polygon getHexPoly(int x, int y, int size) { Polygon p=new Polygon(); for(int i=0;i<6;i++) { double a=Math.PI/180*(60*i+30); p.addPoint((int)(x+size*Math.cos(a)), (int)(y+size*Math.sin(a))); } return p; }
    private int getHandIndexAtPosition(int mx, int my) {
        int hy=getHeight()-120, sw=getWidth()/3;
        for(int i=0;i<manager.hand.size();i++) {
            Stack<Tile> s=manager.hand.get(i); int cx=i*sw+sw/2;
            if(s.isEmpty()) { if(getHexPoly(cx,hy,HEX_DRAW_SIZE).contains(mx,my)) return i; }
            else { for(int k=0;k<s.size();k++) if(getHexPoly(cx,hy-k*TILE_THICKNESS,HEX_DRAW_SIZE).contains(mx,my)) return i; }
            if(my>hy-100 && mx>i*sw && mx<(i+1)*sw && !s.isEmpty()) return i;
        } return -1;
    }
    private Hex getHexAtPosition(int mx, int my) {
        List<Hex> ks=new ArrayList<>(manager.grid.keySet()); ks.sort((h1,h2)->{if(h1.r!=h2.r)return Integer.compare(h1.r,h2.r); return Integer.compare(h2.q,h1.q);});
        for(Hex h:ks) { Stack<Tile> s=manager.grid.get(h); Point p=hexToPixel(h); int ho=s.isEmpty()?0:(s.size()-1)*TILE_THICKNESS; if(getHexPoly(p.x,p.y-ho,HEX_DRAW_SIZE).contains(mx,my)) return h; } return null;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g); Graphics2D g2=(Graphics2D)g; g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        List<Hex> ks=new ArrayList<>(manager.grid.keySet()); ks.sort((h1,h2)->{if(h1.r!=h2.r)return Integer.compare(h1.r,h2.r); return Integer.compare(h1.q,h2.q);});
        for(Hex k:ks) {
            Stack<Tile> s=manager.grid.get(k); Point p=hexToPixel(k);
            g2.setColor(new Color(255,255,255,80)); g2.fillPolygon(getHexPoly(p.x,p.y,HEX_DRAW_SIZE));
            for(int i=0;i<s.size();i++) drawTile(g2,p.x,p.y-i*TILE_THICKNESS,s.get(i).color);
            if(hintTargetHex != null && hintTargetHex.equals(k)) { g2.setColor(Color.WHITE); g2.setStroke(new BasicStroke(4)); g2.drawPolygon(getHexPoly(p.x, p.y - (s.isEmpty()?0:(s.size()-1)*TILE_THICKNESS), HEX_DRAW_SIZE)); }
        }

        int hy=getHeight()-120, sw=getWidth()/3;
        for(int i=0;i<manager.hand.size();i++) {
            int cx=i*sw+sw/2;
            if(i==selectedHandIndex) { g2.setColor(new Color(255,255,255,100)); g2.fillOval(cx-60,hy-70,120,120); }
            if(hintHandIdx == i) { g2.setColor(new Color(255, 255, 0, 100)); g2.fillOval(cx-70, hy-80, 140, 140); }
            Stack<Tile> s=manager.hand.get(i);
            for(int k=0;k<s.size();k++) drawTile(g2,cx,hy-k*TILE_THICKNESS,s.get(k).color);
        }

        for(AnimTile a:activeAnimations) drawTile(g2,(int)a.x,(int)a.y-a.stackIndex*TILE_THICKNESS,a.tile.color);

        g2.setColor(Color.WHITE); g2.setFont(new Font("Segoe UI",Font.BOLD,30));
        g2.drawString("Score: "+manager.score, 30, 50);
        if(manager.currentMode==GameMode.LEVELS) { g2.drawString("Level: "+manager.currentLevel,30,90); g2.setFont(new Font("Segoe UI",Font.PLAIN,20)); g2.drawString("Target: "+manager.levelTargetScore,30,120); }
        else { g2.setFont(new Font("Segoe UI",Font.BOLD,20)); g2.setColor(new Color(255, 255, 255, 200)); g2.drawString("High Score: " + manager.highScore, 30, 80); }

        drawPowerUps(g2);

        menuBtnBounds=new Rectangle(getWidth()-120,20,100,40);
        g2.setColor(new Color(231,76,60)); g2.fillRoundRect(menuBtnBounds.x,menuBtnBounds.y,menuBtnBounds.width,menuBtnBounds.height,10,10);
        g2.setColor(Color.WHITE); g2.setFont(new Font("Segoe UI",Font.BOLD,14)); g2.drawString("MENU",menuBtnBounds.x+30,menuBtnBounds.y+25);

        for(FloatingText t:floatingTexts) t.draw(g2);
        if(isGameOver) drawScreen(g2,"GAME OVER","Better luck next time!",new Color(0,0,0,180));
        if(isLevelWon) drawScreen(g2,"LEVEL CLEARED!","Amazing!",new Color(0,0,0,180));
    }

    private void drawPowerUps(Graphics2D g2) {
        int btnSize = 70, startX = getWidth() - 100, startY = 100;
        hintBtnBounds = new Rectangle(startX, startY, btnSize, btnSize);
        g2.setColor(manager.hintCount > 0 ? new Color(241, 196, 15) : Color.GRAY);
        g2.fillOval(hintBtnBounds.x, hintBtnBounds.y, btnSize, btnSize);
        g2.setColor(Color.WHITE); g2.setStroke(new BasicStroke(3)); g2.drawOval(hintBtnBounds.x, hintBtnBounds.y, btnSize, btnSize);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 12)); g2.drawString(manager.hintCount + " LEFT", startX + 15, startY + 90);
        g2.fillOval(startX + 20, startY + 15, 30, 30); g2.fillRect(startX + 28, startY + 45, 14, 10);

        int bombY = startY + 120;
        bombBtnBounds = new Rectangle(startX, bombY, btnSize, btnSize);
        g2.setColor(isBombActive ? new Color(231, 76, 60) : (manager.bombCount > 0 ? Color.DARK_GRAY : Color.GRAY));
        g2.fillOval(bombBtnBounds.x, bombBtnBounds.y, btnSize, btnSize);
        if (isBombActive) { g2.setColor(Color.YELLOW); g2.setStroke(new BasicStroke(5)); } else { g2.setColor(Color.WHITE); g2.setStroke(new BasicStroke(3)); }
        g2.drawOval(bombBtnBounds.x, bombBtnBounds.y, btnSize, btnSize);
        g2.setColor(Color.WHITE); g2.drawString(manager.bombCount + " LEFT", startX + 15, bombY + 90);
        g2.fillOval(startX + 20, bombY + 20, 30, 30); g2.setStroke(new BasicStroke(2)); g2.drawLine(startX + 35, bombY + 20, startX + 35, bombY + 10);
    }

    private void drawScreen(Graphics2D g2, String t1, String t2, Color bg) {
        g2.setColor(bg); g2.fillRect(0,0,getWidth(),getHeight());
        g2.setColor(Color.WHITE); g2.setFont(new Font("Segoe UI",Font.BOLD,60)); FontMetrics fm=g2.getFontMetrics(); g2.drawString(t1,getWidth()/2-fm.stringWidth(t1)/2,getHeight()/2-50);
        g2.setFont(new Font("Segoe UI",Font.PLAIN,24)); fm=g2.getFontMetrics(); g2.drawString(t2,getWidth()/2-fm.stringWidth(t2)/2,getHeight()/2);
        actionBtnBounds=new Rectangle(getWidth()/2-100,getHeight()/2+50,200,60);
        g2.setColor(new Color(46,204,113)); g2.fillRoundRect(actionBtnBounds.x,actionBtnBounds.y,actionBtnBounds.width,actionBtnBounds.height,20,20);
        g2.setColor(Color.WHITE); g2.setFont(new Font("Segoe UI",Font.BOLD,24)); String bt=(isLevelWon&&manager.currentLevel<10)?"NEXT LEVEL":"RESTART"; if(isLevelWon&&manager.currentLevel>=10) bt="FINISH";
        fm=g2.getFontMetrics(); g2.drawString(bt,actionBtnBounds.x+100-fm.stringWidth(bt)/2,actionBtnBounds.y+40);
    }
    private void drawTile(Graphics2D g2, int x, int y, Color c) {
        Polygon t=getHexPoly(x,y,HEX_DRAW_SIZE), s=getHexPoly(x,y+TILE_THICKNESS,HEX_DRAW_SIZE);
        g2.setColor(c.darker()); g2.fillPolygon(s); g2.setColor(c); g2.fillPolygon(t);
        g2.setColor(new Color(255,255,255,100)); g2.setStroke(new BasicStroke(2)); g2.drawPolygon(t);
    }
}