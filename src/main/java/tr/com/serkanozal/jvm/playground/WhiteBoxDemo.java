package tr.com.serkanozal.jvm.playground;

import java.lang.instrument.Instrumentation;
import java.util.jar.JarFile;


import sun.hotspot.WhiteBox;
import sun.hotspot.WhiteBoxGPS;
import tr.com.serkanozal.jillegal.agent.JillegalAgent;

// Run with "-XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI"
public class WhiteBoxDemo {

    public static void main(String[] args) throws Exception {
        JillegalAgent.init();
        Instrumentation inst = JillegalAgent.getInstrumentation();
        inst.appendToBootstrapClassLoaderSearch(new JarFile(WhiteBoxGPS.LOCATION));
        
        WhiteBox wb = WhiteBox.getWhiteBox();
        
        Object obj = new Object();
        System.out.println("Is Object in old gen: " + wb.isObjectInOldGen(obj));
        
        // Object age is represented with 4 bits, so it can be 15 at most.
        // This means that an object can live in young generation during 15 minor GC at most.
        // So 16 minor GC guarantees that object is promoted to old generation.
        for (int i = 0; i < 16; i++) {
            System.out.println("Triggering young gc for " + (i + 1) + ". time ...");
            wb.youngGC();
        }
        
        System.out.println("Is Object in old gen: " + wb.isObjectInOldGen(obj));
    }

}

