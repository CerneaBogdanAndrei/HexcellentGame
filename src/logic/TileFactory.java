package logic;

import model.Tile;
import java.awt.Color;
import java.util.Random;
import java.util.Stack;

public class TileFactory {
    private static final Color[] PALETTE = {
            new Color(52, 152, 219), new Color(241, 196, 15),
            new Color(155, 89, 182), new Color(231, 76, 60), new Color(46, 204, 113)
    };
    private static final Random rand = new Random();

    public static Stack<Tile> createRandomStack() {
        Stack<Tile> stack = new Stack<>();
        int count = 4 + rand.nextInt(4);
        Color c1 = PALETTE[rand.nextInt(PALETTE.length)];

        if (rand.nextDouble() > 0.6) {
            for (int k=0; k<count; k++) stack.push(new Tile(c1));
        } else {
            Color c2 = PALETTE[rand.nextInt(PALETTE.length)];
            int b = count - (2 + rand.nextInt(2));
            for(int k=0; k<b; k++) stack.push(new Tile(c1));
            for(int k=0; k<count-b; k++) stack.push(new Tile(c2));
        }
        return stack;
    }
}