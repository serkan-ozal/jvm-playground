/**
 * @author SERKAN OZAL
 *         
 *         E-Mail: <a href="mailto:serkanozal86@hotmail.com">serkanozal86@hotmail.com</a>
 *         GitHub: <a>https://github.com/serkan-ozal</a>
 */

package tr.com.serkanozal.jvm.playground.util;

import java.lang.reflect.Field;

import sun.misc.Unsafe;

public class UnsafeUtil {

    public static final Unsafe UNSAFE; 
    
    static {
        try {
            Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            UNSAFE = (Unsafe) unsafeField.get(null);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Unable to get unsafe", e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Unable to get unsafe", e);
        }
    }
    
    private UnsafeUtil() {
        
    }
    
}
