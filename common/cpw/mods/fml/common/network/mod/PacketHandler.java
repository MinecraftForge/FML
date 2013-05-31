package cpw.mods.fml.common.network.mod;

import cpw.mods.fml.common.network.Player;

/**
 * 
 * @author Jacob
 *
 */
public interface PacketHandler {

    /**
     * Handles a packet that was read in. References to packets
     *  should not be kept after this method returns
     * 
     * @param connection The connection the packet was sent over
     * @param player The player the packet was sent from
     * @param packet The packet that was just read in
     */
    void handlePacket(ChannelConnection connection, Player player, ModPacket packet);
}
