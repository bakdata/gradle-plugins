import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class DemoTest {
    @Test
    public void shouldBeEven() {
        assertEquals(true, 4 % 2 == 0);
    }
}
