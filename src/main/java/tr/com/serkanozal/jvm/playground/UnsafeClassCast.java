/**
 * @author SERKAN OZAL
 *         
 *         E-Mail: <a href="mailto:serkanozal86@hotmail.com">serkanozal86@hotmail.com</a>
 *         GitHub: <a>https://github.com/serkan-ozal</a>
 */

package tr.com.serkanozal.jvm.playground;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectStreamClass;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import tr.com.serkanozal.jvm.playground.util.ClasspathUtil;
import tr.com.serkanozal.jvm.playground.util.JvmUtil;

public class UnsafeClassCast {

    private static final String FOO_CLASS_NAME = "tr.com.serkanozal.jvm.playground.UnsafeClassCast$Foo";
    private static final Method HAS_STATIC_INITIALIZER_METHOD;
    private static final Map<Class<?>, Long> UID_MAP = new ConcurrentHashMap<Class<?>, Long>();

    static {
        try {
            HAS_STATIC_INITIALIZER_METHOD = ObjectStreamClass.class
                    .getDeclaredMethod("hasStaticInitializer", Class.class);
            HAS_STATIC_INITIALIZER_METHOD.setAccessible(true);
        } catch (Throwable t) {
            t.printStackTrace();
            throw new IllegalStateException(t);
        }
    }

    public static void main(String[] args) throws Exception {
        FooClassLoader cl = new FooClassLoader(ClasspathUtil.getClasspathUrls()
                .toArray(new URL[0]));
        Foo foo = null;

        try {
            try {
                foo = (Foo) cl.loadClass(FOO_CLASS_NAME).newInstance();
            } catch (ClassCastException e) {
                e.printStackTrace();
            }

            foo = unsafeCast(cl.loadClass(FOO_CLASS_NAME).newInstance(),
                    Foo.class);

            System.out.println(foo);
        } finally {
            cl.close();
        }
    }

    @SuppressWarnings({ "unchecked", "restriction", "deprecation" })
    private static <T> T unsafeCast(Object obj, Class<T> clazz) {
        if (obj == null) {
            return null;
        }
        // If classes have same name
        if (obj.getClass().getName().equals(clazz.getName())) {
            // Find serial version uid, which represents signature of class, of
            // class of object
            Long uid1 = UID_MAP.get(obj.getClass());
            if (uid1 == null) {
                uid1 = generateSerialVersionUID(obj.getClass());
                UID_MAP.put(obj.getClass(), uid1);
            }

            // Find serial version uid, which represents signature of class, of
            // target class to cast
            Long uid2 = UID_MAP.get(clazz);
            if (uid2 == null) {
                uid2 = generateSerialVersionUID(clazz);
                UID_MAP.put(clazz, uid2);
            }

            // If their serial version uids are different, this means that they
            // have different signatures
            // and so they cannot be same and cannot be cast to eachother
            if (uid1 != uid2) {
                new ClassCastException(obj.getClass().getName()
                        + " cannot be cast to " + clazz.getName()
                        + " since they have different signature !");
            }

            // Update class definition pointer of object to target class
            switch (JvmUtil.getAddressSize()) {
            case JvmUtil.ADDRESSING_4_BYTE:
                JvmUtil.getUnsafe().putInt(
                        obj,
                        JvmUtil.getClassDefPointerOffsetInObject(),
                        (int) JvmUtil.toJvmAddress(JvmUtil
                                .addressOfClass(clazz)));
                break;
            case JvmUtil.ADDRESSING_8_BYTE:
                JvmUtil.getUnsafe().putLong(obj,
                        JvmUtil.getClassDefPointerOffsetInObject(),
                        JvmUtil.toJvmAddress(JvmUtil.addressOfClass(clazz)));
                break;
            default:
                throw new IllegalStateException("Unsupported address size: "
                        + JvmUtil.getAddressSize() + " !");
            }
            return (T) obj;
        } else {
            if (clazz.isAssignableFrom(obj.getClass())) {
                return (T) obj;
            } else {
                throw new ClassCastException(obj.getClass().getName()
                        + " cannot be cast to " + clazz.getName() + " !");
            }
        }
    }

    @SuppressWarnings("rawtypes")
    private static long generateSerialVersionUID(Class<?> cl) {
        try {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            DataOutputStream dout = new DataOutputStream(bout);

            dout.writeUTF(cl.getName());

            int classMods = cl.getModifiers()
                    & (Modifier.PUBLIC | Modifier.FINAL | Modifier.INTERFACE | Modifier.ABSTRACT);

            /*
             * compensate for javac bug in which ABSTRACT bit was set for an
             * interface only if the interface declared methods
             */
            Method[] methods = cl.getDeclaredMethods();
            if ((classMods & Modifier.INTERFACE) != 0) {
                classMods = (methods.length > 0) ? (classMods | Modifier.ABSTRACT)
                        : (classMods & ~Modifier.ABSTRACT);
            }
            dout.writeInt(classMods);

            if (!cl.isArray()) {
                /*
                 * compensate for change in 1.2FCS in which
                 * Class.getInterfaces() was modified to return Cloneable and
                 * Serializable for array classes.
                 */
                Class<?>[] interfaces = cl.getInterfaces();
                String[] ifaceNames = new String[interfaces.length];
                for (int i = 0; i < interfaces.length; i++) {
                    ifaceNames[i] = interfaces[i].getName();
                }
                Arrays.sort(ifaceNames);
                for (int i = 0; i < ifaceNames.length; i++) {
                    dout.writeUTF(ifaceNames[i]);
                }
            }

            Field[] fields = cl.getDeclaredFields();
            MemberSignature[] fieldSigs = new MemberSignature[fields.length];
            for (int i = 0; i < fields.length; i++) {
                fieldSigs[i] = new MemberSignature(fields[i]);
            }
            Arrays.sort(fieldSigs, new Comparator<MemberSignature>() {
                public int compare(MemberSignature ms1, MemberSignature ms2) {
                    return ms1.name.compareTo(ms2.name);
                }
            });
            for (int i = 0; i < fieldSigs.length; i++) {
                MemberSignature sig = fieldSigs[i];
                int mods = sig.member.getModifiers()
                        & (Modifier.PUBLIC | Modifier.PRIVATE
                                | Modifier.PROTECTED | Modifier.STATIC
                                | Modifier.FINAL | Modifier.VOLATILE | Modifier.TRANSIENT);
                if (((mods & Modifier.PRIVATE) == 0)
                        || ((mods & (Modifier.STATIC | Modifier.TRANSIENT)) == 0)) {
                    dout.writeUTF(sig.name);
                    dout.writeInt(mods);
                    dout.writeUTF(sig.signature);
                }
            }

            if (hasStaticInitializer(cl)) {
                dout.writeUTF("<clinit>");
                dout.writeInt(Modifier.STATIC);
                dout.writeUTF("()V");
            }

            Constructor[] cons = cl.getDeclaredConstructors();
            MemberSignature[] consSigs = new MemberSignature[cons.length];
            for (int i = 0; i < cons.length; i++) {
                consSigs[i] = new MemberSignature(cons[i]);
            }
            Arrays.sort(consSigs, new Comparator<MemberSignature>() {
                public int compare(MemberSignature ms1, MemberSignature ms2) {
                    return ms1.signature.compareTo(ms2.signature);
                }
            });
            for (int i = 0; i < consSigs.length; i++) {
                MemberSignature sig = consSigs[i];
                int mods = sig.member.getModifiers()
                        & (Modifier.PUBLIC | Modifier.PRIVATE
                                | Modifier.PROTECTED | Modifier.STATIC
                                | Modifier.FINAL | Modifier.SYNCHRONIZED
                                | Modifier.NATIVE | Modifier.ABSTRACT | Modifier.STRICT);
                if ((mods & Modifier.PRIVATE) == 0) {
                    dout.writeUTF("<init>");
                    dout.writeInt(mods);
                    dout.writeUTF(sig.signature.replace('/', '.'));
                }
            }

            MemberSignature[] methSigs = new MemberSignature[methods.length];
            for (int i = 0; i < methods.length; i++) {
                methSigs[i] = new MemberSignature(methods[i]);
            }
            Arrays.sort(methSigs, new Comparator<MemberSignature>() {
                public int compare(MemberSignature ms1, MemberSignature ms2) {
                    int comp = ms1.name.compareTo(ms2.name);
                    if (comp == 0) {
                        comp = ms1.signature.compareTo(ms2.signature);
                    }
                    return comp;
                }
            });
            for (int i = 0; i < methSigs.length; i++) {
                MemberSignature sig = methSigs[i];
                int mods = sig.member.getModifiers()
                        & (Modifier.PUBLIC | Modifier.PRIVATE
                                | Modifier.PROTECTED | Modifier.STATIC
                                | Modifier.FINAL | Modifier.SYNCHRONIZED
                                | Modifier.NATIVE | Modifier.ABSTRACT | Modifier.STRICT);
                if ((mods & Modifier.PRIVATE) == 0) {
                    dout.writeUTF(sig.name);
                    dout.writeInt(mods);
                    dout.writeUTF(sig.signature.replace('/', '.'));
                }
            }

            dout.flush();

            MessageDigest md = MessageDigest.getInstance("SHA");
            byte[] hashBytes = md.digest(bout.toByteArray());
            long hash = 0;
            for (int i = Math.min(hashBytes.length, 8) - 1; i >= 0; i--) {
                hash = (hash << 8) | (hashBytes[i] & 0xFF);
            }
            return hash;
        } catch (IOException ex) {
            throw new InternalError();
        } catch (NoSuchAlgorithmException ex) {
            throw new SecurityException(ex.getMessage());
        }
    }

    /**
     * Returns true if the given class defines a static initializer method,
     * false otherwise.
     */
    private static boolean hasStaticInitializer(Class<?> cl) {
        try {
            return (Boolean) HAS_STATIC_INITIALIZER_METHOD.invoke(null, cl);
        } catch (Throwable t) {
            t.printStackTrace();
            throw new IllegalStateException(t);
        }
    }

    /**
     * Returns JVM type signature for given class.
     */
    private static String getClassSignature(Class<?> cl) {
        StringBuilder sbuf = new StringBuilder();
        while (cl.isArray()) {
            sbuf.append('[');
            cl = cl.getComponentType();
        }
        if (cl.isPrimitive()) {
            if (cl == Integer.TYPE) {
                sbuf.append('I');
            } else if (cl == Byte.TYPE) {
                sbuf.append('B');
            } else if (cl == Long.TYPE) {
                sbuf.append('J');
            } else if (cl == Float.TYPE) {
                sbuf.append('F');
            } else if (cl == Double.TYPE) {
                sbuf.append('D');
            } else if (cl == Short.TYPE) {
                sbuf.append('S');
            } else if (cl == Character.TYPE) {
                sbuf.append('C');
            } else if (cl == Boolean.TYPE) {
                sbuf.append('Z');
            } else if (cl == Void.TYPE) {
                sbuf.append('V');
            } else {
                throw new InternalError();
            }
        } else {
            sbuf.append('L' + cl.getName().replace('.', '/') + ';');
        }
        return sbuf.toString();
    }

    /**
     * Returns JVM type signature for given list of parameters and return type.
     */
    private static String getMethodSignature(Class<?>[] paramTypes,
            Class<?> retType) {
        StringBuilder sbuf = new StringBuilder();
        sbuf.append('(');
        for (int i = 0; i < paramTypes.length; i++) {
            sbuf.append(getClassSignature(paramTypes[i]));
        }
        sbuf.append(')');
        sbuf.append(getClassSignature(retType));
        return sbuf.toString();
    }

    /**
     * Class for computing and caching field/constructor/method signatures
     * during serialVersionUID calculation.
     */
    private static class MemberSignature {

        public final Member member;
        public final String name;
        public final String signature;

        public MemberSignature(Field field) {
            member = field;
            name = field.getName();
            signature = getClassSignature(field.getType());
        }

        public MemberSignature(Constructor<?> cons) {
            member = cons;
            name = cons.getName();
            signature = getMethodSignature(cons.getParameterTypes(), Void.TYPE);
        }

        public MemberSignature(Method meth) {
            member = meth;
            name = meth.getName();
            signature = getMethodSignature(meth.getParameterTypes(),
                    meth.getReturnType());
        }

    }

    private static class FooClassLoader extends URLClassLoader {

        private static Class<?> FOO_CLASS = null;

        public FooClassLoader(URL[] urls) {
            super(urls);
        }

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            if (name.equals(FOO_CLASS_NAME)) {
                if (FOO_CLASS == null) {
                    FOO_CLASS = findClass(name);
                }
                return FOO_CLASS;
            } else {
                return super.loadClass(name);
            }
        }

    }

    public static class Foo {

        @Override
        public String toString() {
            return "Hello, I am Foo !!!";
        }

    }

}
