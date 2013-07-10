package cpw.mods.fml.common.network.packet;

/**
 * 
 * @author Jacob
 *
 * This interface designates a handler for packets
 *  
 */
public interface PacketHandler {

    /**
     * Handle a packet from the given connection
     * 
     * @param connection The connection that the packet was received on
     * @param packet The packet that was received
     */
    void handlePacket(NetworkConnection connection, ModPacket packet);
}
