package model;

import java.awt.Point;

public class AnimTile {
    public Tile tile;
    public Point start, end;
    public double x, y;
    public double progress;
    public int stackIndex;

    public AnimTile(Tile t, Point s, Point e, int i) {
        this.tile = t;
        this.start = s;
        this.end = e;
        this.x = s.x;
        this.y = s.y;
        this.stackIndex = i;
        this.progress = 0;
    }
}