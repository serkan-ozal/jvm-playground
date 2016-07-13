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

public class Field implements Comparable<Field> {

    public final String name;
    public final String typeName;
    public final long offset;
    public final boolean isStatic;

    Field(String name, String typeName, long offset, boolean isStatic) {
        this.name = name;
        this.typeName = typeName;
        this.offset = offset;
        this.isStatic = isStatic;
    }

    @Override
    public int compareTo(Field o) {
        if (isStatic != o.isStatic) {
            return isStatic ? -1 : 1;
        }
        if (offset < o.offset) {
            return -1;
        } else if (offset > o.offset) {
            return +1;
        } else {
            return 0;
        }
    }

    @Override
    public String toString() {
        if (isStatic) {
            return "static " + typeName + ' ' + name + " @ 0x" + 
                   Long.toHexString(offset);
        } else {
            return typeName + ' ' + name + " @ " + offset;
        }
    }

}
