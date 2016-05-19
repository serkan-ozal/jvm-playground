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

import java.lang.reflect.Field;

public class Unsafe {

    public static final sun.misc.Unsafe INSTANCE;

    static {
        try {
            Field f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            INSTANCE = (sun.misc.Unsafe) f.get(null);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to get Unsafe", e);
        }
    }

}
