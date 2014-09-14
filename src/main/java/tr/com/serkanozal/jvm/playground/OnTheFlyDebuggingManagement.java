/**
 * @author SERKAN OZAL
 *         
 *         E-Mail: <a href="mailto:serkanozal86@hotmail.com">serkanozal86@hotmail.com</a>
 *         GitHub: <a>https://github.com/serkan-ozal</a>
 */

package tr.com.serkanozal.jvm.playground;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.sun.jdi.VirtualMachineManager;
import com.sun.jdi.connect.Connector.Argument;
import com.sun.jdi.connect.ListeningConnector;
import com.sun.tools.jdi.SocketListeningConnector;

public class OnTheFlyDebuggingManagement {

    public static final int DEFAULT_DEBUGGING_PORT = 9999;
    public static final int DEFAULT_DEBUGGING_TIMEOUT = 5000; // 5 seconds

    private static VirtualMachineManager vmm;
    private static Class<?> stringArgumentImplClass;
    private static Class<?> integerArgumentImplClass;
    private static Constructor<?> stringArgumentImplClassCons;
    private static Constructor<?> integerArgumentImplClassCons;
    private static Map<String, Map<String, Object>> listenerMap = new ConcurrentHashMap<String, Map<String, Object>>();

    static {
        try {
            init();
        } catch (Throwable t) {
            t.printStackTrace();
            throw new IllegalStateException(t);
        }
    }

    public static void main(String[] args) throws Exception {
        final String debugId = startDebugging(); // Start debugging with default
                                                 // parameters

        System.out.println("Waiting before stopping debugging ...");

        Thread.sleep(DEFAULT_DEBUGGING_TIMEOUT);

        stopDebugging(debugId); // Stop debugging with its specific debug id
    }

    private static void init() throws Exception {
        vmm = com.sun.tools.jdi.VirtualMachineManagerImpl
                .virtualMachineManager();

        System.out.println("================================================");
        System.out.println("ALL       CONNECTORS >>> " + vmm.allConnectors());
        System.out.println("LISTENING CONNECTORS >>> "
                + vmm.listeningConnectors());
        System.out.println("ATTACHING CONNECTORS >>> "
                + vmm.attachingConnectors());
        System.out
                .println("================================================\n");

        stringArgumentImplClass = Class
                .forName("com.sun.tools.jdi.ConnectorImpl$StringArgumentImpl");
        integerArgumentImplClass = Class
                .forName("com.sun.tools.jdi.ConnectorImpl$IntegerArgumentImpl");

        // String name, String label, String description, String value, boolean
        // mustSpecify
        stringArgumentImplClassCons = stringArgumentImplClass
                .getDeclaredConstructor(
                        Class.forName("com.sun.tools.jdi.ConnectorImpl"),
                        String.class, String.class, String.class, String.class,
                        boolean.class);
        stringArgumentImplClassCons.setAccessible(true);

        // String name, String label, String description, String value, boolean
        // mustSpecify, int min, int max
        integerArgumentImplClassCons = integerArgumentImplClass
                .getDeclaredConstructor(
                        Class.forName("com.sun.tools.jdi.ConnectorImpl"),
                        String.class, String.class, String.class, String.class,
                        boolean.class, int.class, int.class);
        integerArgumentImplClassCons.setAccessible(true);
    }

    public static String startDebugging() throws Exception {
        return startDebugging(DEFAULT_DEBUGGING_PORT, DEFAULT_DEBUGGING_TIMEOUT);
    }

    @SuppressWarnings("unchecked")
    public static String startDebugging(int listeningPort, int timeout)
            throws Exception {
        String debugId = null;
        for (ListeningConnector lc : vmm.listeningConnectors()) {
            if (lc instanceof SocketListeningConnector) {
                SocketListeningConnector slc = (SocketListeningConnector) lc;
                Map<String, Object> arguments = new HashMap<String, Object>();
                arguments.put("timeout", integerArgumentImplClassCons
                        .newInstance(slc, "timeout",
                                "generic_attaching.timeout.label",
                                "generic_attaching.timeout",
                                String.valueOf(timeout), true, 0,
                                Integer.MAX_VALUE));
                arguments.put("port", stringArgumentImplClassCons.newInstance(
                        slc, "port", "socket_listening.port.label",
                        "socket_listening.port", String.valueOf(listeningPort),
                        true));
                arguments.put("localAddress",
                        stringArgumentImplClassCons
                                .newInstance(slc, "localAddress",
                                        "socket_listening.localaddr.label",
                                        "socket_listening.localaddr",
                                        "localhost", true));

                debugId = UUID.randomUUID().toString();

                slc.startListening((Map<String, ? extends Argument>) arguments);

                System.out
                        .println("Started listening for debugging with debug id "
                                + debugId);

                listenerMap.put(debugId, arguments);

                break;
            }
        }
        return debugId;
    }

    @SuppressWarnings("unchecked")
    public static void stopDebugging(String debugId) throws Exception {
        Map<String, Object> arguments = listenerMap.remove(debugId);
        if (arguments == null) {
            throw new IllegalArgumentException(
                    "Couldn't be found debugger for debug id " + debugId);
        }
        for (ListeningConnector lc : vmm.listeningConnectors()) {
            if (lc instanceof SocketListeningConnector) {
                SocketListeningConnector slc = (SocketListeningConnector) lc;

                slc.stopListening((Map<String, ? extends Argument>) arguments);

                System.out
                        .println("Stopped listening for debugging with debug id "
                                + debugId);

                break;
            }
        }
    }

}
