package cpw.mods.fml.common.network;

import net.minecraft.network.INetworkManager;
import net.minecraft.network.packet.NetHandler;
import net.minecraft.network.packet.Packet;

public class PacketListener
{
    /**
     * Registers this packet listener so it can analyze all packets of the specified packet type that the server tries to send.
     *
     * @param packetId The id of the packet type to listen for.
     */
    public final void registerOutgoing(int packetId)
    {
        FMLNetworkHandler.registerOutgoingPacketListener(this, packetId);
    }

    /**
     * Registers this packet listener so it can analyze all packets of the specified packet type that the server receives.
     *
     * @param packetId The id of the packet type to listen for.
     */
    public final void registerIncoming(int packetId)
    {
        FMLNetworkHandler.registerIncomingPacketListener(this, packetId);
    }

    /**
     * This method is called whenever the server tries to send a packet to the client.
     *
     * @returns whether the packet should be sent to the client
     */
    public boolean onOutgoingPacket(Packet packet, INetworkManager networkManager, NetHandler netHandler)
    {
        return true;
    }

    /**
     * This method is called whenever the server receives a packet from the client.
     *
     * @returns whether the packet should be handled by the server
     */
    public boolean onIncomingPacket(Packet packet, INetworkManager networkManager, NetHandler netHandler)
    {
        return true;
    }
}
