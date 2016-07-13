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

import java.util.NoSuchElementException;
import java.util.Set;

public class Type {

    private static final Field[] NO_FIELDS = new Field[0];

    public final String name;
    public final String superName;
    public final int size;
    public final boolean isOop;
    public final boolean isInt;
    public final boolean isUnsigned;
    public final Field[] fields;

    Type(String name, String superName, int size, boolean isOop, boolean isInt,
         boolean isUnsigned, Set<Field> fields) {
        this.name = name;
        this.superName = superName;
        this.size = size;
        this.isOop = isOop;
        this.isInt = isInt;
        this.isUnsigned = isUnsigned;
        this.fields = fields == null ? NO_FIELDS : fields.toArray(new Field[fields.size()]);
    }

    public Field field(String name) {
        for (Field field : fields) {
            if (field.name.equals(name)) {
                return field;
            }
        }
        throw new NoSuchElementException("No such field: " + name);
    }

    public long global(String name) {
        Field field = field(name);
        if (field.isStatic) {
            return field.offset;
        }
        throw new IllegalArgumentException("Static field expected");
    }

    public long offset(String name) {
        Field field = field(name);
        if (!field.isStatic) {
            return field.offset;
        }
        throw new IllegalArgumentException("Instance field expected");
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(name);
        if (superName != null)
            sb.append(" extends ").append(superName);
        sb.append(" @ ").append(size).append('\n');
        for (Field field : fields) {
            sb.append("  ").append(field).append('\n');
        }
        return sb.toString();
    }

}
