/**
 * @author SERKAN OZAL
 *         
 *         E-Mail: <a href="mailto:serkanozal86@hotmail.com">serkanozal86@hotmail.com</a>
 *         GitHub: <a>https://github.com/serkan-ozal</a>
 */

package tr.com.serkanozal.jvm.playground;

import java.util.ArrayList;
import java.util.List;

import sun.misc.Unsafe;
import tr.com.serkanozal.jvm.playground.util.JvmUtil;

@SuppressWarnings("restriction")
public class ObjectCreationWithoutDefaultConstructor {

    private static final Unsafe UNSAFE;
    
    static {
        System.setProperty("disableHotspotSA", "true"); 
        UNSAFE = JvmUtil.getUnsafe();
    }
    
    public static void main(String[] args) throws Exception {
        final int OBJ_COUNT = 1000;
        final int OBJ_HEADER_SIZE = JvmUtil.getHeaderSize();
        final long OBJ_SIZE = (int) JvmUtil.sizeOfWithReflection(Foo.class);
        final long BYTE_ARRAY_INDEX_OFFSET = UNSAFE.arrayBaseOffset(byte[].class);
        
        ////////////////////////////////////////////////////////////////////////
        
        CompressedOops.HotspotJvmClassAddressFinder classAddressFinder = 
                CompressedOops.createClassAddressFinder();
        
        List<Foo> fooList = new ArrayList<Foo>();
        
        for (int i = 0; i < OBJ_COUNT; i++) {
            // We create an on-heap object, so GC knows its address and know that there is an object
            byte[] b = new byte[(int) (OBJ_SIZE - BYTE_ARRAY_INDEX_OFFSET)];
            Object o = (b);
            
            // We change the class definition pointer, 
            // so anymore byte array will be interpreted as "Foo" object
            if (OBJ_HEADER_SIZE == 8) {
                UNSAFE.putInt(b, 4L, (int)classAddressFinder.jvmAddressOfClass(Foo.class));
            }
            else if (OBJ_HEADER_SIZE == 12) {
                UNSAFE.putInt(b, 8L, (int)classAddressFinder.jvmAddressOfClass(Foo.class));
            }
            else if (OBJ_HEADER_SIZE == 16) {
                UNSAFE.putLong(b, 8L, classAddressFinder.jvmAddressOfClass(Foo.class));
            }
            
            Foo foo = (Foo) o;
            fooList.add(foo);
        }
        
        Thread.sleep(3000);
        
        JvmUtil.runGC();
        
        Thread.sleep(3000);
        
        for (Foo foo : fooList) {
            System.out.println(foo);
        }
    }

    public static class Foo {
      
        long l0, l1, l2, l3, l4, l5, l6, l7, l8, l9;
        
        public Foo(String str) {
            System.out.println("Foo constructor with string " + str);
        }

        @Override
        public String toString() {
            return "Foo [l0=" + l0 + ", l1=" + l1 + ", l2=" + l2 + ", l3=" + l3
                    + ", l4=" + l4 + ", l5=" + l5 + ", l6=" + l6 + ", l7=" + l7
                    + ", l8=" + l8 + ", l9=" + l9 + "]";
        }

    }

}
