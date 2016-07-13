/*
 * Imported from "helfy" (https://github.com/apangin/helfy) by "Andrei Pangin"
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tr.com.serkanozal.jvm.playground.util.introspection;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class Symbols {

    private static Method findNative;
    private static ClassLoader classLoader;

    static {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("windows")) {
            String jre = System.getProperty("java.home");
            if (!loadLibrary(jre + "/bin/server/jvm.dll") && 
                    !loadLibrary(jre + "/bin/client/jvm.dll")) {
                throw new IllegalStateException("Cannot find jvm.dll. Unsupported JVM?");
            }
            classLoader = Symbols.class.getClassLoader();
        }

        try {
            findNative = ClassLoader.class.getDeclaredMethod("findNative", 
                                                             ClassLoader.class, 
                                                             String.class);
            findNative.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Method ClassLoader.findNative not found");
        }
    }

    private static boolean loadLibrary(String dll) {
        try {
            System.load(dll);
            return true;
        } catch (UnsatisfiedLinkError e) {
            return false;
        }
    }

    public static long lookup(String name) {
        try {
            return (Long) findNative.invoke(null, classLoader, name);
        } catch (InvocationTargetException e) {
            throw new RuntimeException("Couldn't find symbol with name " + name,
                                       e.getTargetException());
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Couldn't find symbol with name " + name, e);
        }
    }

}
