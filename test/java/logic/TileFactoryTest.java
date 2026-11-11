package logic;

import model.Tile;
import org.junit.jupiter.api.Test;

import java.util.Stack;

import static org.junit.jupiter.api.Assertions.*;

public class TileFactoryTest {
    @Test
    void createRandomStackProducesNonEmptyStackWithinExpectedSize() {
        Stack<Tile> s = TileFactory.createRandomStack();
        assertNotNull(s);
        assertTrue(s.size() >= 4 && s.size() <= 7);
        assertTrue(s.stream().allMatch(t -> t != null && t.color != null));
    }
}
