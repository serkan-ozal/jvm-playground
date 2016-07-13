package tr.com.serkanozal.jvm.playground;

import tr.com.serkanozal.jvm.playground.util.introspection.Field;
import tr.com.serkanozal.jvm.playground.util.introspection.JVM;
import tr.com.serkanozal.jvm.playground.util.introspection.Type;
import static tr.com.serkanozal.jvm.playground.util.UnsafeUtil.UNSAFE;

public class IdenticalTwinObjectsDemo {

    /*
     *  **********************************************************
     *  ************************** NOTE **************************
     *  **********************************************************
     *  
     * There are some assumptions for the sake of demo 
     * by ignoring some cases these might happen rarely 
     * if you are unlucky :
     *      - Assume that allocations for the objects ("sampleArray1" and "sampleArray2")
     *        are done in the same TLAB
     *      - Assume that none of the objects ("sampleArray1" and "sampleArray2")
     *        are moved to neither survivor spaces nor tenured space by minor GC
     */
    public static void main(String[] args) throws Exception {
        JVM jvm = new JVM();

        Thread thread = Thread.currentThread();

        long threadAddr = UNSAFE.getLong(thread, 
                                         UNSAFE.objectFieldOffset(Thread.class.getDeclaredField("eetop")));

        Type threadType = jvm.type("Thread");
        Type threadLocalAllocBufferType = jvm.type("ThreadLocalAllocBuffer");
        
        Field _tlabField = threadType.field("_tlab");
        Field _startField = threadLocalAllocBufferType.field("_start");
        Field _endField = threadLocalAllocBufferType.field("_end");
        Field _topField = threadLocalAllocBufferType.field("_top");
        
        // "ThreadLocalAllocBuffer" is laid out in the "Thread" as embedded.
        // Therefore we sum offset of "_tlab" and "start/end/top" fields 
        // to find the related field address.
        long _startFieldAddress = threadAddr + _tlabField.offset + _startField.offset;
        long _endFieldAddress = threadAddr + _tlabField.offset + _endField.offset;
        long _topFieldAddress = threadAddr + _tlabField.offset + _topField.offset;
        
        /////////////////////////////////////////////////////////////////////////////////////

        // Save the TLAB bump-pointer before creation of "sampleArray1"
        long tlabBumpPointerOld = UNSAFE.getLong(_topFieldAddress);
        
        long start1 = UNSAFE.getLong(_startFieldAddress);
        long end1 = UNSAFE.getLong(_endFieldAddress);
        long top1 = UNSAFE.getLong(_topFieldAddress);
        
        // "sampleArray1" will be allocated at the location 
        // which is pointed by TLAB bump-pointer which has been found above
        byte[] sampleArray1 = new byte[4];
        
        long start2 = UNSAFE.getLong(_startFieldAddress);
        long end2 = UNSAFE.getLong(_endFieldAddress);
        long top2 = UNSAFE.getLong(_topFieldAddress);
        
        /////////////////////////////////////////////////////////////////////////////////////

        System.out.println("TLAB values before creation of 'sampleArray1':");
        System.out.println("\t - TLAB Start : " + "0x" + Long.toHexString(start1));
        System.out.println("\t - TLAB End   : " + "0x" + Long.toHexString(end1));
        System.out.println("\t - TLAB Top   : " + "0x" + Long.toHexString(top1));
        
        System.out.println("TLAB values after creation of 'sampleArray1':");
        System.out.println("\t - TLAB Start : " + "0x" + Long.toHexString(start2));
        System.out.println("\t - TLAB End   : " + "0x" + Long.toHexString(end2));
        System.out.println("\t - TLAB Top   : " + "0x" + Long.toHexString(top2));
        
        System.out.println("There is " + (top2 - top1) + " bytes increase for the TLAB bump-pointer.");
        System.out.println("This shows us the size of memory region allocated for 'sampleArray1'.");
        
        /////////////////////////////////////////////////////////////////////////////////////
        
        System.out.println("Initial array content for 'sampleArray1': ");
        for (int i = 0; i < sampleArray1.length; i++) {
            System.out.println("\t - [" + i + "]: " + sampleArray1[i]);
        }
        
        // Update content of "sampleArray1"
        System.out.println("Update content of 'sampleArray1'");
        for (int i = 0; i < sampleArray1.length; i++) {
            sampleArray1[i] = (byte) i;
        }

        System.out.println("Content of 'sampleArray1' after update: ");
        for (int i = 0; i < sampleArray1.length; i++) {
            System.out.println("\t - [" + i + "]: " + sampleArray1[i]);
        }

        /////////////////////////////////////////////////////////////////////////////////////
        
        // Save the current TLAB bump-pointer 
        long tlabBumpPointerCurrent = UNSAFE.getLong(_topFieldAddress);
        
        // Reset the TLAB bump-pointer to the old one
        UNSAFE.putLong(_topFieldAddress, tlabBumpPointerOld);
        
        // Since we reset the TLAB bump-pointer to the old one (points to "sampleArray1"),
        // "sampleArray2" will be allocated at the same location with "sampleArray1"
        byte[] sampleArray2 = new byte[4];
        
        // Restore the TLAB bump-pointer from the saved current TLAB bump-pointer
        UNSAFE.putLong(_topFieldAddress, tlabBumpPointerCurrent);
        
        /////////////////////////////////////////////////////////////////////////////////////

        // We will see that content of "sampleArray1" is cleared.
        // Because memory region for elements of "sampleArray2" is cleared on allocation of "sampleArray2".
        // Since "sampleArray1" and "sampleArray2" shares the same location,
        // this clear on "sampleArray2" effects also "sampleArray1".
        System.out.println("Content of 'sampleArray1' after creation of 'sampleArray2': ");
        for (int i = 0; i < sampleArray1.length; i++) {
            System.out.println("\t - [" + i + "]: " + sampleArray1[i]);
        }
        
        // Update content of "sampleArray1"
        System.out.println("Update content of 'sampleArray1'");
        for (int i = 0; i < sampleArray1.length; i++) {
            sampleArray1[i] = (byte) i;
        }
        
        // We will see that content of "sampleArray2" is also updated 
        // regarding to regarding to update of "sampleArray1".
        // Because "sampleArray1" and "sampleArray2" share the same location,
        // so this update on "sampleArray1" effects also "sampleArray2".
        System.out.println("Content of 'sampleArray2' after update of 'sampleArray1': ");
        for (int i = 0; i < sampleArray2.length; i++) {
            System.out.println("\t - [" + i + "]: " + sampleArray2[i]);
        }

    }

}
