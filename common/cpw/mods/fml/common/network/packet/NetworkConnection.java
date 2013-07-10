package cpw.mods.fml.common.network.packet;

/**
 * 
 * @author Jacob
 *
 * A network connection that can send
 *  and receive packets
 */
public interface NetworkConnection {

    /**
     * Sends a packet over the connection
     * 
     * @param packet The packet to send
     */
    void sendPacket(ModPacket packet);
    
    /**
     * Sets the PacketHandler that handles packets
     *  received over this connection.
     *  
     * A null value is an acceptable value that
     *  means to ignore all packets received over
     *  this connection
     *  
     * @param handler The new PacketHandler
     */
    void setPacketHandler(PacketHandler handler);
    
    /**
     * Gets the PacketHandler that is currently handling
     *  packet received over this connection.
     *  
     * @return This connections PacketHandler
     */
    PacketHandler getPacketHandler();
    
    /**
     * Gets the current PacketRepository used by this
     *  connection to create new packets
     *  
     * @return This connections PacketRepository
     */
    PacketRepository getPacketRepository();
    
    /**
     * Sets the PacketRepository used by this connection
     *  to create new packets
     *  
     * @param repo The new PacketRepository
     */
    void setPacketRepository(PacketRepository repo);
}
