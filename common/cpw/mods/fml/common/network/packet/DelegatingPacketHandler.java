package cpw.mods.fml.common.network.packet;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import cpw.mods.fml.common.FMLLog;

/**
 * 
 * @author Jacob
 *
 * A PacketHandler that delegates handle requests to
 *  sub class methods
 */
public abstract class DelegatingPacketHandler implements PacketHandler {

    private final Map<Integer, Method> packetHandlers;
    
    protected DelegatingPacketHandler() {
        this.packetHandlers = new HashMap<Integer, Method>();
        
        for(Method method : this.getClass().getMethods()) {
            int mod = method.getModifiers();
            
            if(Modifier.isAbstract(mod) || !Modifier.isPublic(mod) || Modifier.isStatic(mod)) {
                //Ignore all static, abstract, or non-public methods
                continue;
            }
            
            DelegatingPacketHandler.Handler handler = method.getAnnotation(DelegatingPacketHandler.Handler.class);
            if(handler == null) {
                //Not a handler method
                continue;
            }
            
            int id = handler.value();
            if(id < PacketRepository.STARTING_PACKET_ID || id > PacketRepository.MAX_PACKET_ID) {
                FMLLog.warning("Invalid packet handler id: %d in packet handler class: %s.", id, this.getClass());
                continue;
            }
            
            Class<?>[] params = method.getParameterTypes();
            if(params.length != 2 || !(NetworkConnection.class.isAssignableFrom(params[0]) && ModPacket.class.isAssignableFrom(params[1]))) {
                FMLLog.warning("Invalid packet handler method: %s.", method);
            }
            
            method.setAccessible(true);
            
            this.packetHandlers.put(id, method);
        }
    }
    
    @Override
    public final void handlePacket(NetworkConnection connection, ModPacket packet) {
        Method handleMethod = this.packetHandlers.get(packet.getID());
        if(handleMethod != null) {
            try {
                handleMethod.invoke(this, connection, packet);
            }
            catch(Throwable t) {
                FMLLog.warning("Error executing handler method: %s.", handleMethod);
            }
        }
        else {
            this.handleUnexpectedPacket(connection, packet);
        }
    }
    
    /**
     * Called when no handler is found for a packet
     * 
     * @param connection The connection the packet was received on
     * @param packet The packet received
     */
    protected abstract void handleUnexpectedPacket(NetworkConnection connection, ModPacket packet);
    
    
    /**
     * 
     * @author Jacob
     * 
     * Marker annotation used to mark methods as packet handler
     *  methods based on packet ID.
     *  
     * The method signature of a Handler method must be
     *  (NetworkConnection, ModPacket)
     *  
     *  or subclasses of them
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public static @interface Handler {
        
        int value();
    }
}
