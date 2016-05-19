package tr.com.serkanozal.jvm.playground;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

import javax.management.MBeanServer;

import com.sun.management.HotSpotDiagnosticMXBean;

import tr.com.serkanozal.jvm.playground.util.introspection.Field;
import tr.com.serkanozal.jvm.playground.util.introspection.JVM;
import tr.com.serkanozal.jvm.playground.util.introspection.Type;

public class ChangeImmutableVmFlagDemo {

    public static void main(String[] args) throws IOException {
        MBeanServer mbserver = ManagementFactory.getPlatformMBeanServer();
        HotSpotDiagnosticMXBean mxbean = 
                ManagementFactory.newPlatformMXBeanProxy(
                        mbserver, 
                        "com.sun.management:type=HotSpotDiagnostic", 
                        HotSpotDiagnosticMXBean.class);
        
        System.out.println("UnlockDiagnosticVMOptions: " + mxbean.getVMOption("UnlockDiagnosticVMOptions"));
        
        JVM jvm = new JVM();

        Type flagType = jvm.type("Flag");
        int flagSize = flagType.size;
        
        Field flagsField = flagType.field("flags");
        long flagsAddress = jvm.getAddress(flagsField.offset);
        
        Field numFlagsField = flagType.field("numFlags");
        int numFlagsValue = jvm.getInt(numFlagsField.offset);
        
        Field _nameField = flagType.field("_name");
        int _nameFieldOffset = (int) _nameField.offset;
        
        Field _addrField = flagType.field("_addr");
        int _addrFieldOffset = (int) _addrField.offset;
        
        for (int i = 0; i < numFlagsValue - 1; i++) {
            long flagAddress = flagsAddress + (i * flagSize);
            long flagNameAddress = jvm.getAddress(flagAddress + _nameFieldOffset);

            List<Byte> flagNameData = new ArrayList<Byte>();
            byte flagNameByte;
            for (int j = 0; (flagNameByte = jvm.getByte(flagNameAddress + j)) != 0; j++) {
                flagNameData.add(flagNameByte);
            }
            byte[] flagNameBytes = new byte[flagNameData.size()];
            for (int j = 0; j < flagNameData.size(); j++) {
                flagNameBytes[(int) j] = ((Byte) flagNameData.get((int) j)).byteValue();
            }
            String flagName = new String(flagNameBytes);
            
            long flagAddrAddress = jvm.getAddress(flagAddress + _addrFieldOffset);
            if ("UnlockDiagnosticVMOptions".equals(flagName)) {
                if (jvm.getByte(flagAddrAddress) == 0) {
                    jvm.putByte(flagAddrAddress, (byte) 0x01);
                    System.out.println(flagName + " has been enabled!");
                } else {
                    System.out.println(flagName + " is already enabled");
                }
            }
        }
        
        System.out.println("UnlockDiagnosticVMOptions: " + mxbean.getVMOption("UnlockDiagnosticVMOptions"));
    }

}
