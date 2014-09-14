/**
 * @author SERKAN OZAL
 *         
 *         E-Mail: <a href="mailto:serkanozal86@hotmail.com">serkanozal86@hotmail.com</a>
 *         GitHub: <a>https://github.com/serkan-ozal</a>
 */

package tr.com.serkanozal.jvm.playground;

import java.util.Random;

import sun.misc.Unsafe;
import tr.com.serkanozal.jvm.playground.util.JvmUtil;

@SuppressWarnings("restriction")
public class UnsafeCopyMemory {

    private static final Unsafe UNSAFE;
    private static final Random RANDOM;
    
    static {
        System.setProperty("disableHotspotSA", "true"); 
        UNSAFE = JvmUtil.getUnsafe();
        RANDOM = new Random();
    }
    
    public static void main(String[] args) throws Exception {
        final int OBJ_COUNT = 1000;
        final byte[] IO_BUFFER = new byte[1024 * 1024]; // 1 MB buffer
        final byte[] HELPER_ARRAY = new byte[0];
        final Foo HELPER_OBJ = (Foo) UNSAFE.allocateInstance(Foo.class);
        final int OBJ_HEADER_SIZE = JvmUtil.getHeaderSize();
        final long OBJ_SIZE = (int) JvmUtil.sizeOfWithReflection(Foo.class);
        final long BYTE_ARRAY_INDEX_OFFSET = UNSAFE.arrayBaseOffset(byte[].class);
        final long SINGLE_OBJ_COPY_SIZE = OBJ_SIZE - OBJ_HEADER_SIZE;
        
        ////////////////////////////////////////////////////////////////////////
        
        long l1a = UNSAFE.getLong(HELPER_OBJ, 0L);
        long l1b = 0L;
        long l2a = UNSAFE.getLong(HELPER_ARRAY, 0L);
        long l2b = 0L;
        if (OBJ_HEADER_SIZE == 12) {
            l1b = UNSAFE.getInt(HELPER_OBJ, 8L);
            l2b = UNSAFE.getInt(HELPER_ARRAY, 8L);
        }
        else if (OBJ_HEADER_SIZE == 16) {
            l1b = UNSAFE.getLong(HELPER_OBJ, 8L);
            l2b = UNSAFE.getLong(HELPER_ARRAY, 8L);
        }
       
        ////////////////////////////////////////////////////////////////////////
        
        long copyOffset = BYTE_ARRAY_INDEX_OFFSET;
        for (int i = 0; i < OBJ_COUNT; i++) {
            Foo foo = createRandomFoo();
            UNSAFE.copyMemory(foo, OBJ_HEADER_SIZE, IO_BUFFER, copyOffset, SINGLE_OBJ_COPY_SIZE);
            copyOffset += SINGLE_OBJ_COPY_SIZE;
        }
        
        ////////////////////////////////////////////////////////////////////////
        
        copyOffset = BYTE_ARRAY_INDEX_OFFSET;
        for (int i = 0; i < OBJ_COUNT; i++) {
            Foo foo = (Foo) UNSAFE.allocateInstance(Foo.class);
            
            ////////////////////////////////////////////////
            
            // Change object header to byte[] header, 
            // so JVM doesn't throw "IllegalArgumentException".
            // Because for "Unsafe.copyMemory" is not supported in case of that target object is not an array
   
            // If you want to be sure, just comment-out these lines and try :)
            // So, you will get "IllegalArgumentException".
            // ******************************************* //
            UNSAFE.putLong(foo, 0L, l2a);
            if (OBJ_HEADER_SIZE == 12) {
                UNSAFE.putInt(foo, 8L, (int) l2b);
            } 
            else if (OBJ_HEADER_SIZE == 16) {
                UNSAFE.putLong(foo, 8L, l2b);
            }
            // ******************************************* //
            
            ////////////////////////////////////////////////
            
            UNSAFE.copyMemory(IO_BUFFER, copyOffset, foo, OBJ_HEADER_SIZE, SINGLE_OBJ_COPY_SIZE);
            
            ////////////////////////////////////////////////
            
            // Change back object header
            
            UNSAFE.putLong(foo, 0L, l1a);
            if (OBJ_HEADER_SIZE == 12) {
                UNSAFE.putInt(foo, 8L, (int) l1b);
            } 
            else if (OBJ_HEADER_SIZE == 16) {
                UNSAFE.putLong(foo, 8L, l1b);
            }
            
            ////////////////////////////////////////////////
            
            copyOffset += SINGLE_OBJ_COPY_SIZE;
            
            ////////////////////////////////////////////////
            
            System.out.println(foo);
        }
    }
    
    private static Foo createRandomFoo() {
        Foo foo = new Foo();
        foo.l0 = RANDOM.nextLong();
        foo.l1 = RANDOM.nextLong();
        foo.l2 = RANDOM.nextLong();
        foo.l3 = RANDOM.nextLong();
        foo.l4 = RANDOM.nextLong();
        foo.l5 = RANDOM.nextLong();
        foo.l6 = RANDOM.nextLong();
        foo.l7 = RANDOM.nextLong();
        foo.l8 = RANDOM.nextLong();
        foo.l9 = RANDOM.nextLong();
        return foo;
    }
    
    public static class Foo {
      
        long l0, l1, l2, l3, l4, l5, l6, l7, l8, l9;

        @Override
        public String toString() {
            return "Foo [l0=" + l0 + ", l1=" + l1 + ", l2=" + l2 + ", l3=" + l3
                    + ", l4=" + l4 + ", l5=" + l5 + ", l6=" + l6 + ", l7=" + l7
                    + ", l8=" + l8 + ", l9=" + l9 + "]";
        }

    }

}
