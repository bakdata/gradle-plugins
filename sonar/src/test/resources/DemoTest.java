import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
public class DemoTest {
    @Test
    public void shouldBeEven() {
        assertEquals(true, 4 % 2 == 0);
    }
}