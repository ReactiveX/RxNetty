package io.reactivex.netty.client.pool;

import static org.junit.Assert.*;
import io.reactivex.netty.client.pool.AdjustableSemaphore;

import org.junit.Test;

public class AdjustableSemaphoreTest {

    @Test
    public void testIncrease() {
        AdjustableSemaphore semophore = new AdjustableSemaphore(3);
        try {
            semophore.acquire();
            semophore.acquire();
            assertEquals(1, semophore.availablePermits());
            semophore.setMaxPermits(6);
            assertEquals(4, semophore.availablePermits());
            for (int i = 0; i < 4; i++) {
                semophore.acquire();
            }
            assertEquals(0, semophore.availablePermits());            
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testDecrease() throws Exception {
        AdjustableSemaphore semophore = new AdjustableSemaphore(6);
        semophore.acquire();
        semophore.acquire();
        semophore.acquire();
        // 3 permits are already given out
        semophore.setMaxPermits(2);

        // -1
        assertFalse(semophore.tryAcquire());
        // 0
        semophore.release();
        assertFalse(semophore.tryAcquire());
        // 1
        semophore.release();
        semophore.acquire();
        assertEquals(0, semophore.availablePermits());            
    }
   
}
