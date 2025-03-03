import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class DemoTest {
    @Test
    void shouldBeTrue() {
        final Supplier<Boolean> supplier = mock();
        when(supplier.get()).thenReturn(true);
        assertEquals(true, supplier.get());
    }
}
