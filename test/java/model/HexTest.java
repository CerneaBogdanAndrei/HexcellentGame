package model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class HexTest {
    @Test
    void equalsAndHashCodeAreConsistent() {
        Hex a = new Hex(1, -2);
        Hex b = new Hex(1, -2);
        Hex c = new Hex(2, -2);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
    }
}
