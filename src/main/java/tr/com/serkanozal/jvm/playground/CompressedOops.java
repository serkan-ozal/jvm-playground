/**
 * @author SERKAN OZAL
 *         
 *         E-Mail: <a href="mailto:serkanozal86@hotmail.com">serkanozal86@hotmail.com</a>
 *         GitHub: <a>https://github.com/serkan-ozal</a>
 */

package tr.com.serkanozal.jvm.playground;

import java.lang.management.ManagementFactory;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import sun.misc.Unsafe;
import tr.com.serkanozal.jvm.playground.util.JvmUtil;

@SuppressWarnings({ "restriction" })
public class CompressedOops {

    private static final Unsafe UNSAFE;
    private static final CompressedOopsInfo COMPRESSED_OOPS_INFO;

    private CompressedOops() {

    }

    static {
        System.setProperty("disableHotspotSA", "true");
        UNSAFE = JvmUtil.getUnsafe();
        COMPRESSED_OOPS_INFO = findCompressedOopsInfo();
    }

    public static void main(String[] args) throws Exception {
        System.out.println(COMPRESSED_OOPS_INFO);
    }

    // ////////////////////////////////////////////////////////////////////////////

    public CompressedOopsInfo getCompressedOopsInfo() {
        return COMPRESSED_OOPS_INFO;
    }

    // ////////////////////////////////////////////////////////////////////////////

    public static class CompressedOopsInfo {

        private boolean enabled;
        private long baseAddressForObjectPointers;
        private int shiftSizeForObjectPointers;
        private long baseAddressForClassPointers;
        private int shiftSizeForClassPointers;

        public CompressedOopsInfo() {

        }

        public CompressedOopsInfo(boolean enabled) {
            this.enabled = enabled;
        }

        public CompressedOopsInfo(long baseAddressForObjectPointers,
                int shiftSizeForObjectPointers) {
            this.baseAddressForObjectPointers = baseAddressForObjectPointers;
            this.shiftSizeForObjectPointers = shiftSizeForObjectPointers;
            this.baseAddressForClassPointers = baseAddressForObjectPointers;
            this.shiftSizeForClassPointers = shiftSizeForObjectPointers;
            this.enabled = true;
        }

        public CompressedOopsInfo(long baseAddressForObjectPointers,
                int shiftSizeForObjectPointers,
                long baseAddressForClassPointers, int shiftSizeForClassPointers) {
            this.baseAddressForObjectPointers = baseAddressForObjectPointers;
            this.shiftSizeForObjectPointers = shiftSizeForObjectPointers;
            this.baseAddressForClassPointers = baseAddressForClassPointers;
            this.shiftSizeForClassPointers = shiftSizeForClassPointers;
            this.enabled = true;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public long getBaseAddressForObjectPointers() {
            return baseAddressForObjectPointers;
        }

        public void setBaseAddressForObjectPointers(
                long baseAddressForObjectPointers) {
            this.baseAddressForObjectPointers = baseAddressForObjectPointers;
        }

        public int getShiftSizeForObjectPointers() {
            return shiftSizeForObjectPointers;
        }

        public void setShiftSizeForObjectPointers(int shiftSizeForObjectPointers) {
            this.shiftSizeForObjectPointers = shiftSizeForObjectPointers;
        }

        public long getBaseAddressForClassPointers() {
            return baseAddressForClassPointers;
        }

        public void setBaseAddressForClassPointers(
                long baseAddressForClassPointers) {
            this.baseAddressForClassPointers = baseAddressForClassPointers;
        }

        public int getShiftSizeForClassPointers() {
            return shiftSizeForClassPointers;
        }

        public void setShiftSizeForClassPointers(int shiftSizeForClassPointers) {
            this.shiftSizeForClassPointers = shiftSizeForClassPointers;
        }

        @Override
        public String toString() {
            if (enabled) {
                return "Compressed-Oops are enabled with " + "base-address: "
                        + COMPRESSED_OOPS_INFO.baseAddressForObjectPointers
                        + " and with " + "shift-size: "
                        + COMPRESSED_OOPS_INFO.shiftSizeForObjectPointers;
            } else {
                return "Compressed-Oops are disabled";
            }
        }

    }

    // ////////////////////////////////////////////////////////////////////////////

    private static CompressedOopsInfo findCompressedOopsInfo() {
        CompressedOopsInfo compressedOopsInfo = createCompressedOopsProvider()
                .getCompressedOopsInfo();
        if (compressedOopsInfo != null) {
            return compressedOopsInfo;
        } else {
            return new DefaultCompressedOopsInfoProvider()
                    .getCompressedOopsInfo();
        }
    }

    private static CompressedOopsInfoProvider createCompressedOopsProvider() {
        if (JvmUtil.isHotspotJvm()) {
            if (JvmUtil.isJava_6()) {
                return new Java6HotspotJvmCompressedOopsInfoProvider();
            } else if (JvmUtil.isJava_7()) {
                return new Java7HotspotJvmCompressedOopsInfoProvider();
            } else if (JvmUtil.isJava_8()) {
                return new Java8HotspotJvmCompressedOopsInfoProvider();
            } else {
                throw new IllegalStateException("Unsupported Java version: "
                        + JvmUtil.JAVA_SPEC_VERSION);
            }
        } else if (JvmUtil.isJRockitJvm()) {
            return new JRockitJvmCompressedOopsInfoProvider();
        } else if (JvmUtil.isIBMJvm()) {
            return new IbmJvmCompressedOopsInfoProvider();
        } else {
            throw new IllegalStateException("Unsupported JVM: "
                    + JvmUtil.JVM_NAME);
        }
    }

    // ////////////////////////////////////////////////////////////////////////////

    private static class CompressedOopsProviderCavy {

        @SuppressWarnings("unused")
        Object obj1;
        @SuppressWarnings("unused")
        Object obj2;

    }

    private interface CompressedOopsInfoProvider {

        CompressedOopsInfo getCompressedOopsInfo();

    }

    private static class DefaultCompressedOopsInfoProvider implements
            CompressedOopsInfoProvider {

        @Override
        public CompressedOopsInfo getCompressedOopsInfo() {
            /*
             * When running with CompressedOops on 64-bit platform, the address
             * size reported by Unsafe is still 8, while the real reference
             * fields are 4 bytes long. Try to guess the reference field size
             * with this naive trick.
             */
            int oopSize = -1;
            try {
                long off1 = UNSAFE
                        .objectFieldOffset(CompressedOopsProviderCavy.class
                                .getField("obj1"));
                long off2 = UNSAFE
                        .objectFieldOffset(CompressedOopsProviderCavy.class
                                .getField("obj2"));
                oopSize = (int) Math.abs(off2 - off1);
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }

            if (oopSize != UNSAFE.addressSize()) {
                switch (oopSize) {
                case JvmUtil.ADDRESSING_8_BYTE:
                    return new CompressedOopsInfo(0,
                            log2p(JvmUtil.getObjectAlignment()));
                case JvmUtil.ADDRESSING_16_BYTE:
                    return new CompressedOopsInfo(0,
                            log2p(JvmUtil.getObjectAlignment()));
                default:
                    throw new IllegalStateException(
                            "Unsupported address size for compressed reference shifting: "
                                    + oopSize);
                }
            } else {
                return new CompressedOopsInfo(false);
            }
        }

    }

    private static class JRockitJvmCompressedOopsInfoProvider implements
            CompressedOopsInfoProvider {

        @Override
        public CompressedOopsInfo getCompressedOopsInfo() {
            try {
                MBeanServer server = ManagementFactory.getPlatformMBeanServer();
                String str = (String) server.invoke(new ObjectName(
                        "oracle.jrockit.management:type=DiagnosticCommand"),
                        "execute", new Object[] { "print_vm_state" },
                        new String[] { "java.lang.String" });
                String[] split = str.split("\n");
                for (String s : split) {
                    if (s.contains("CompRefs")) {
                        Pattern pattern = Pattern
                                .compile("(.*?)References are compressed, with heap base (.*?) and shift (.*?)\\.");
                        Matcher matcher = pattern.matcher(s);
                        if (matcher.matches()) {
                            return new CompressedOopsInfo(
                                    Integer.valueOf(matcher.group(2)),
                                    Integer.valueOf(matcher.group(3)));
                        } else {
                            return new CompressedOopsInfo(false);
                        }
                    }
                }
                return null;
            } catch (Throwable t) {
                t.printStackTrace();
                return null;
            }
        }

    }

    private static class IbmJvmCompressedOopsInfoProvider implements
            CompressedOopsInfoProvider {

        @Override
        public CompressedOopsInfo getCompressedOopsInfo() {
            // TODO Current IBM JVM is not supported
            return null;
        }

    }

    private static int log2p(int x) {
        int r = 0;
        while ((x >>= 1) != 0)
            r++;
        return r;
    }

    private static abstract class AbstractHotspotJvmCompressedOopsInfoProvider
            implements CompressedOopsInfoProvider {

        protected static final int COMPRESSED_OOP_SHIFT_SIZE = log2p(JvmUtil
                .getObjectAlignment());

        protected static final HotspotJvmClassAddressFinder hotspotJvmClassAddressFinder = createClassAddressFinder();
        protected final boolean compressedOopsHandlingStrategyForBbjectAndClassIsDifferent;

        protected AbstractHotspotJvmCompressedOopsInfoProvider(
                boolean compressedOopsHandlingStrategyForBbjectAndClassIsDifferent) {
            this.compressedOopsHandlingStrategyForBbjectAndClassIsDifferent = compressedOopsHandlingStrategyForBbjectAndClassIsDifferent;
        }

        @Override
        public CompressedOopsInfo getCompressedOopsInfo() {
            long nativeAddressOfClass = hotspotJvmClassAddressFinder
                    .nativeAddressOfClass(CompressedOopsProviderCavy.class);
            long jvmAddressOfClass = hotspotJvmClassAddressFinder
                    .jvmAddressOfClass(CompressedOopsProviderCavy.class);
            if (!compressedOopsHandlingStrategyForBbjectAndClassIsDifferent) {
                if (nativeAddressOfClass == jvmAddressOfClass) {
                    return new CompressedOopsInfo(0L, 0);
                } else {
                    long shiftedAddress = jvmAddressOfClass << COMPRESSED_OOP_SHIFT_SIZE;
                    return new CompressedOopsInfo(nativeAddressOfClass
                            - shiftedAddress, COMPRESSED_OOP_SHIFT_SIZE);
                }
            } else {
                long nativeAddressOfClassInstance = hotspotJvmClassAddressFinder
                        .nativeAddressOfClassInstance(CompressedOopsProviderCavy.class);
                long jvmAddressOfClassInstance = hotspotJvmClassAddressFinder
                        .jvmAddressOfClassInstance(CompressedOopsProviderCavy.class);

                long baseAddressForObjectPointers = 0L;
                int shiftSizeForObjectPointers = 0;
                long baseAddressForClassPointers = 0L;
                int shiftSizeForClassPointers = 0;

                if (nativeAddressOfClass != jvmAddressOfClass) {
                    long shiftedAddress = jvmAddressOfClass << COMPRESSED_OOP_SHIFT_SIZE;
                    baseAddressForObjectPointers = nativeAddressOfClass
                            - shiftedAddress;
                    shiftSizeForObjectPointers = COMPRESSED_OOP_SHIFT_SIZE;
                }

                if (nativeAddressOfClassInstance != jvmAddressOfClassInstance) {
                    long shiftedAddress = jvmAddressOfClassInstance << COMPRESSED_OOP_SHIFT_SIZE;
                    baseAddressForClassPointers = nativeAddressOfClassInstance
                            - shiftedAddress;
                    shiftSizeForClassPointers = COMPRESSED_OOP_SHIFT_SIZE;
                }

                return new CompressedOopsInfo(baseAddressForObjectPointers,
                        shiftSizeForObjectPointers,
                        baseAddressForClassPointers, shiftSizeForClassPointers);
            }
        }

    }

    private static class Java6HotspotJvmCompressedOopsInfoProvider extends
            AbstractHotspotJvmCompressedOopsInfoProvider {

        public Java6HotspotJvmCompressedOopsInfoProvider() {
            super(false);
        }

    }

    private static class Java7HotspotJvmCompressedOopsInfoProvider extends
            AbstractHotspotJvmCompressedOopsInfoProvider {

        public Java7HotspotJvmCompressedOopsInfoProvider() {
            super(false);
        }

    }

    private static class Java8HotspotJvmCompressedOopsInfoProvider extends
            AbstractHotspotJvmCompressedOopsInfoProvider {

        public Java8HotspotJvmCompressedOopsInfoProvider() {
            super(true);
        }

    }

    // ////////////////////////////////////////////////////////////////////////////

    private static HotspotJvmClassAddressFinder createClassAddressFinder() {
        if (JvmUtil.isJava_6()) {
            if (JvmUtil.getAddressSize() == JvmUtil.ADDRESSING_4_BYTE) {
                return new Java6On32BitHotspotJvmClassAddressFinder();
            } else if (JvmUtil.getAddressSize() == JvmUtil.ADDRESSING_8_BYTE) {
                if (JvmUtil.isCompressedRef()) {
                    return new Java6On64BitHotspotJvmWithCompressedOopsClassAddressFinder();
                } else {
                    return new Java6On64BitHotspotJvmWithoutCompressedOopsClassAddressFinder();
                }
            } else {
                throw new IllegalStateException("Unsupported address size: "
                        + JvmUtil.getAddressSize() + " !");
            }
        } else if (JvmUtil.isJava_7()) {
            if (JvmUtil.getAddressSize() == JvmUtil.ADDRESSING_4_BYTE) {
                return new Java7On32BitHotspotJvmClassAddressFinder();
            } else if (JvmUtil.getAddressSize() == JvmUtil.ADDRESSING_8_BYTE) {
                if (JvmUtil.isCompressedRef()) {
                    return new Java7On64BitHotspotJvmWithCompressedOopsClassAddressFinder();
                } else {
                    return new Java7On64BitHotspotJvmWithoutCompressedOopsClassAddressFinder();
                }
            } else {
                throw new IllegalStateException("Unsupported address size: "
                        + JvmUtil.getAddressSize() + " !");
            }
        } else if (JvmUtil.isJava_8()) {
            if (JvmUtil.getAddressSize() == JvmUtil.ADDRESSING_4_BYTE) {
                return new Java8On32BitHotspotJvmClassAddressFinder();
            } else if (JvmUtil.getAddressSize() == JvmUtil.ADDRESSING_8_BYTE) {
                if (JvmUtil.isCompressedRef()) {
                    return new Java8On64BitHotspotJvmWithCompressedOopsClassAddressFinder();
                } else {
                    return new Java8On64BitHotspotJvmWithoutCompressedOopsClassAddressFinder();
                }
            } else {
                throw new IllegalStateException("Unsupported address size: "
                        + JvmUtil.getAddressSize() + " !");
            }
        } else {
            throw new IllegalStateException("Unsupported Java version: "
                    + JvmUtil.JAVA_SPEC_VERSION);
        }
    }

    private static interface HotspotJvmClassAddressFinder {

        long CLASS_DEFINITION_POINTER_OFFSET_IN_CLASS_INSTANCE_32_BIT_FOR_JAVA_6 = 8L;
        long CLASS_DEFINITION_POINTER_OFFSET_IN_CLASS_INSTANCE_64_BIT_WITH_COMPRESSED_REF_FOR_JAVA_6 = 12L;
        long CLASS_DEFINITION_POINTER_OFFSET_IN_CLASS_INSTANCE_64_BIT_WITHOUT_COMPRESSED_REF_FOR_JAVA_6 = 16L;

        long CLASS_INSTANCE_POINTER_OFFSET_IN_CLASS_DEFINITION_32_BIT_FOR_JAVA_6 = 60L;
        long CLASS_INSTANCE_POINTER_OFFSET_IN_CLASS_DEFINITION_64_BIT_WITH_COMPRESSED_REF_FOR_JAVA_6 = 112L;
        long CLASS_INSTANCE_POINTER_OFFSET_IN_CLASS_DEFINITION_64_BIT_WITHOUT_COMPRESSED_REF_FOR_JAVA_6 = 112L;

        long CLASS_DEFINITION_POINTER_OFFSET_IN_CLASS_DEFINITION_32_BIT_FOR_JAVA_6 = 32L;
        long CLASS_DEFINITION_POINTER_OFFSET_IN_CLASS_DEFINITION_64_BIT_WITH_COMPRESSED_REF_FOR_JAVA_6 = 56L;
        long CLASS_DEFINITION_POINTER_OFFSET_IN_CLASS_DEFINITION_64_BIT_WITHOUT_COMPRESSED_REF_FOR_JAVA_6 = 56L;

        // ////////////////////////////////////////////////////////////////////////////

        long CLASS_DEFINITION_POINTER_OFFSET_IN_CLASS_INSTANCE_32_BIT_FOR_JAVA_7 = 80L;
        long CLASS_DEFINITION_POINTER_OFFSET_IN_CLASS_INSTANCE_64_BIT_WITH_COMPRESSED_REF_FOR_JAVA_7 = 84L;
        long CLASS_DEFINITION_POINTER_OFFSET_IN_CLASS_INSTANCE_64_BIT_WITHOUT_COMPRESSED_REF_FOR_JAVA_7 = 160L;

        long CLASS_INSTANCE_POINTER_OFFSET_IN_CLASS_DEFINITION_32_BIT_FOR_JAVA_7 = 64L;
        long CLASS_INSTANCE_POINTER_OFFSET_IN_CLASS_DEFINITION_64_BIT_WITH_COMPRESSED_REF_FOR_JAVA_7 = 120L;
        long CLASS_INSTANCE_POINTER_OFFSET_IN_CLASS_DEFINITION_64_BIT_WITHOUT_COMPRESSED_REF_FOR_JAVA_7 = 120L;

        long CLASS_DEFINITION_POINTER_OFFSET_IN_CLASS_DEFINITION_32_BIT_FOR_JAVA_7 = 36L;
        long CLASS_DEFINITION_POINTER_OFFSET_IN_CLASS_DEFINITION_64_BIT_WITH_COMPRESSED_REF_FOR_JAVA_7 = 64L;
        long CLASS_DEFINITION_POINTER_OFFSET_IN_CLASS_DEFINITION_64_BIT_WITHOUT_COMPRESSED_REF_FOR_JAVA_7 = 64L;

        // ////////////////////////////////////////////////////////////////////////////

        long CLASS_DEFINITION_POINTER_OFFSET_IN_CLASS_INSTANCE_32_BIT_FOR_JAVA_8 = 64L;
        long CLASS_DEFINITION_POINTER_OFFSET_IN_CLASS_INSTANCE_64_BIT_WITH_COMPRESSED_REF_FOR_JAVA_8 = 64L;
        long CLASS_DEFINITION_POINTER_OFFSET_IN_CLASS_INSTANCE_64_BIT_WITHOUT_COMPRESSED_REF_FOR_JAVA_8 = 120L;

        long CLASS_INSTANCE_POINTER_OFFSET_IN_CLASS_DEFINITION_32_BIT_FOR_JAVA_8 = 56L;
        long CLASS_INSTANCE_POINTER_OFFSET_IN_CLASS_DEFINITION_64_BIT_WITH_COMPRESSED_REF_FOR_JAVA_8 = 48L;
        long CLASS_INSTANCE_POINTER_OFFSET_IN_CLASS_DEFINITION_64_BIT_WITHOUT_COMPRESSED_REF_FOR_JAVA_8 = 104L;

        long CLASS_DEFINITION_POINTER_OFFSET_IN_CLASS_DEFINITION_32_BIT_FOR_JAVA_8 = 28L;
        long CLASS_DEFINITION_POINTER_OFFSET_IN_CLASS_DEFINITION_64_BIT_WITH_COMPRESSED_REF_FOR_JAVA_8 = 48L;
        long CLASS_DEFINITION_POINTER_OFFSET_IN_CLASS_DEFINITION_64_BIT_WITHOUT_COMPRESSED_REF_FOR_JAVA_8 = 48L;

        // ////////////////////////////////////////////////////////////////////////////

        long jvmAddressOfClassInstance(Class<?> clazz);

        long nativeAddressOfClassInstance(Class<?> clazz);

        long jvmAddressOfClass(Class<?> clazz);

        long nativeAddressOfClass(Class<?> clazz);

    }

    private static abstract class AbstractHotspotJvmClassAddressFinder
            implements HotspotJvmClassAddressFinder {

        protected static final long OBJECT_ARRAY_BASE_OFFSET = UNSAFE
                .arrayBaseOffset(Object[].class);

        protected final Object[] temp = new Object[1];

        protected long classDefPointerOffsetInClassInst;
        protected long classInstPointerOffsetInClassDef;
        protected long classDefPointerOffsetInClassDef;

        @SuppressWarnings("unused")
        protected AbstractHotspotJvmClassAddressFinder() {

        }

        protected AbstractHotspotJvmClassAddressFinder(
                long classDefPointerOffsetInClassInst,
                long classInstPointerOffsetInClassDef,
                long classDefPointerOffsetInClassDef) {
            this.classDefPointerOffsetInClassInst = classDefPointerOffsetInClassInst;
            this.classInstPointerOffsetInClassDef = classInstPointerOffsetInClassDef;
            this.classDefPointerOffsetInClassDef = classDefPointerOffsetInClassDef;
        }

        protected long normalize(int value) {
            return value & 0xFFFFFFFFL;
        }

        @Override
        public synchronized long jvmAddressOfClassInstance(Class<?> clazz) {
            try {
                temp[0] = clazz;
                return normalize(UNSAFE.getInt(temp, OBJECT_ARRAY_BASE_OFFSET));
            } finally {
                temp[0] = null;
            }
        }

        @Override
        public synchronized long nativeAddressOfClassInstance(Class<?> clazz) {
            try {
                UNSAFE.putInt(temp, OBJECT_ARRAY_BASE_OFFSET,
                        (int) jvmAddressOfClass(clazz));
                Object o = temp[0];
                return UNSAFE.getInt(o, classInstPointerOffsetInClassDef);
            } finally {
                temp[0] = null;
            }
        }

        @Override
        public synchronized long jvmAddressOfClass(Class<?> clazz) {
            return normalize(UNSAFE.getInt(clazz,
                    classDefPointerOffsetInClassInst));
        }

        @Override
        public synchronized long nativeAddressOfClass(Class<?> clazz) {
            try {
                UNSAFE.putInt(temp, OBJECT_ARRAY_BASE_OFFSET,
                        (int) jvmAddressOfClass(clazz));
                Object o = temp[0];
                return UNSAFE.getInt(o, classDefPointerOffsetInClassDef);
            } finally {
                temp[0] = null;
            }
        }

    }

    // ////////////////////////////////////////////////////////////////////////////

    private static class Java6On32BitHotspotJvmClassAddressFinder extends
            AbstractHotspotJvmClassAddressFinder {

        private Java6On32BitHotspotJvmClassAddressFinder() {
            super(
                    CLASS_DEFINITION_POINTER_OFFSET_IN_CLASS_INSTANCE_32_BIT_FOR_JAVA_6,
                    CLASS_INSTANCE_POINTER_OFFSET_IN_CLASS_DEFINITION_32_BIT_FOR_JAVA_6,
                    CLASS_DEFINITION_POINTER_OFFSET_IN_CLASS_DEFINITION_32_BIT_FOR_JAVA_6);
        }

    }

    private static class Java6On64BitHotspotJvmWithoutCompressedOopsClassAddressFinder
            extends AbstractHotspotJvmClassAddressFinder {

        private Java6On64BitHotspotJvmWithoutCompressedOopsClassAddressFinder() {
            super(
                    CLASS_DEFINITION_POINTER_OFFSET_IN_CLASS_INSTANCE_64_BIT_WITHOUT_COMPRESSED_REF_FOR_JAVA_6,
                    CLASS_INSTANCE_POINTER_OFFSET_IN_CLASS_DEFINITION_64_BIT_WITHOUT_COMPRESSED_REF_FOR_JAVA_6,
                    CLASS_DEFINITION_POINTER_OFFSET_IN_CLASS_DEFINITION_64_BIT_WITHOUT_COMPRESSED_REF_FOR_JAVA_6);
        }

    }

    private static class Java6On64BitHotspotJvmWithCompressedOopsClassAddressFinder
            extends AbstractHotspotJvmClassAddressFinder {

        private Java6On64BitHotspotJvmWithCompressedOopsClassAddressFinder() {
            super(
                    CLASS_DEFINITION_POINTER_OFFSET_IN_CLASS_INSTANCE_64_BIT_WITH_COMPRESSED_REF_FOR_JAVA_6,
                    CLASS_INSTANCE_POINTER_OFFSET_IN_CLASS_DEFINITION_64_BIT_WITH_COMPRESSED_REF_FOR_JAVA_6,
                    CLASS_DEFINITION_POINTER_OFFSET_IN_CLASS_DEFINITION_64_BIT_WITH_COMPRESSED_REF_FOR_JAVA_6);
        }

    }

    // ////////////////////////////////////////////////////////////////////////////

    private static class Java7On32BitHotspotJvmClassAddressFinder extends
            AbstractHotspotJvmClassAddressFinder {

        private Java7On32BitHotspotJvmClassAddressFinder() {
            super(
                    CLASS_DEFINITION_POINTER_OFFSET_IN_CLASS_INSTANCE_32_BIT_FOR_JAVA_7,
                    CLASS_INSTANCE_POINTER_OFFSET_IN_CLASS_DEFINITION_32_BIT_FOR_JAVA_7,
                    CLASS_DEFINITION_POINTER_OFFSET_IN_CLASS_DEFINITION_32_BIT_FOR_JAVA_7);
        }

    }

    private static class Java7On64BitHotspotJvmWithoutCompressedOopsClassAddressFinder
            extends AbstractHotspotJvmClassAddressFinder {

        private Java7On64BitHotspotJvmWithoutCompressedOopsClassAddressFinder() {
            super(
                    CLASS_DEFINITION_POINTER_OFFSET_IN_CLASS_INSTANCE_64_BIT_WITHOUT_COMPRESSED_REF_FOR_JAVA_7,
                    CLASS_INSTANCE_POINTER_OFFSET_IN_CLASS_DEFINITION_64_BIT_WITHOUT_COMPRESSED_REF_FOR_JAVA_7,
                    CLASS_DEFINITION_POINTER_OFFSET_IN_CLASS_DEFINITION_64_BIT_WITHOUT_COMPRESSED_REF_FOR_JAVA_7);
        }

    }

    private static class Java7On64BitHotspotJvmWithCompressedOopsClassAddressFinder
            extends AbstractHotspotJvmClassAddressFinder {

        private Java7On64BitHotspotJvmWithCompressedOopsClassAddressFinder() {
            super(
                    CLASS_DEFINITION_POINTER_OFFSET_IN_CLASS_INSTANCE_64_BIT_WITH_COMPRESSED_REF_FOR_JAVA_7,
                    CLASS_INSTANCE_POINTER_OFFSET_IN_CLASS_DEFINITION_64_BIT_WITH_COMPRESSED_REF_FOR_JAVA_7,
                    CLASS_DEFINITION_POINTER_OFFSET_IN_CLASS_DEFINITION_64_BIT_WITH_COMPRESSED_REF_FOR_JAVA_7);
        }

    }

    // ////////////////////////////////////////////////////////////////////////////

    private static class Java8On32BitHotspotJvmClassAddressFinder extends
            AbstractHotspotJvmClassAddressFinder {

        private Java8On32BitHotspotJvmClassAddressFinder() {
            super(
                    CLASS_DEFINITION_POINTER_OFFSET_IN_CLASS_INSTANCE_32_BIT_FOR_JAVA_8,
                    CLASS_INSTANCE_POINTER_OFFSET_IN_CLASS_DEFINITION_32_BIT_FOR_JAVA_8,
                    CLASS_DEFINITION_POINTER_OFFSET_IN_CLASS_DEFINITION_32_BIT_FOR_JAVA_8);
        }

    }

    private static class Java8On64BitHotspotJvmWithoutCompressedOopsClassAddressFinder
            extends AbstractHotspotJvmClassAddressFinder {

        private Java8On64BitHotspotJvmWithoutCompressedOopsClassAddressFinder() {
            super(
                    CLASS_DEFINITION_POINTER_OFFSET_IN_CLASS_INSTANCE_64_BIT_WITHOUT_COMPRESSED_REF_FOR_JAVA_8,
                    CLASS_INSTANCE_POINTER_OFFSET_IN_CLASS_DEFINITION_64_BIT_WITHOUT_COMPRESSED_REF_FOR_JAVA_8,
                    CLASS_DEFINITION_POINTER_OFFSET_IN_CLASS_DEFINITION_64_BIT_WITHOUT_COMPRESSED_REF_FOR_JAVA_8);
        }

    }

    private static class Java8On64BitHotspotJvmWithCompressedOopsClassAddressFinder
            extends AbstractHotspotJvmClassAddressFinder {

        private Java8On64BitHotspotJvmWithCompressedOopsClassAddressFinder() {
            super(
                    CLASS_DEFINITION_POINTER_OFFSET_IN_CLASS_INSTANCE_64_BIT_WITH_COMPRESSED_REF_FOR_JAVA_8,
                    CLASS_INSTANCE_POINTER_OFFSET_IN_CLASS_DEFINITION_64_BIT_WITH_COMPRESSED_REF_FOR_JAVA_8,
                    CLASS_DEFINITION_POINTER_OFFSET_IN_CLASS_DEFINITION_64_BIT_WITH_COMPRESSED_REF_FOR_JAVA_8);
        }

    }

    // ////////////////////////////////////////////////////////////////////////////

}
