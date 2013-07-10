package cpw.mods.fml.common.network.packet;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import net.minecraft.network.packet.Packet250CustomPayload;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;

import cpw.mods.fml.common.FMLLog;

public class ChannelNetworkConnection extends AbstractNetworkConnection{

    private final NetworkManager networkManager;
    private final String channel;
    
    private final PacketSplitter packetSplitter;
    
    private boolean[] sideOpen;
    
    
    ChannelNetworkConnection(NetworkManager networkManager, String channel) {
        this.networkManager = networkManager;
        this.channel = channel;
        
        this.packetSplitter = new PacketSplitter(channel);
        
        this.sideOpen = new boolean[NetworkSide.values().length];
    }
    
    /**
     * Gets this connections channel
     * 
     * @return This connections channel
     */
    public String getChannel() {
        return this.channel;
    }
    
    /**
     * Gets if the given side is open.
     * 
     * @param side The NetworkSide
     * 
     * @return If the side is open
     */
    public boolean isSideOpen(NetworkSide side) {
        return this.sideOpen[side.ordinal()];
    }
    
    /**
     * Gets if this connection is closed. This
     *  connection is deemed closed if all
     *  NetworkSides are closed
     *  
     * When a Channel Connection is closed the
     *  NetworkManager is free to remove if
     *  from internal lists and a new Connection
     *  should be requested from the NetworkManager
     *  
     * @return If this connection is closed
     */
    public boolean isClosed() {
        for(int i=0; i<this.sideOpen.length; i++) {
            if(this.sideOpen[i]) {
                //Atleast one side is open
                return false;
            }
        }
        //No sides are open
        return true;
    }
    
    /**
     * Gets the NetworkManager for this connection
     * 
     * @return The NetworkManager for this connection
     */
    public NetworkManager getNetworkManager() {
        return this.networkManager;
    }

    @Override
    public void sendPacket(ModPacket packet) {
        if(this.isClosed()) {
            throw new IllegalStateException("Network connection is closed on channel: " + this.getChannel());
        }
        if(!this.isSideOpen(NetworkSide.REMOTE)) {
            //Remote side is not listening to packets
            return;
        }
        
        for(Packet250CustomPayload vanillaPacket : this.packetSplitter.getPackets(packet)) {
            this.networkManager.getVanillaConnection().func_74429_a(vanillaPacket);
        }
    }
    
    @Override
    public void setPacketHandler(PacketHandler handler) {
        if(this.isClosed()) {
            throw new IllegalStateException("Network connection is closed on channel: " + this.getChannel());
        }
        super.setPacketHandler(handler);
        
        if(handler == null) {
            this.closeSide(NetworkSide.LOCAL);
        }
        else {
            this.openSide(NetworkSide.LOCAL);
        }
    }
    
    void closeSide(NetworkSide side) {
        if(side == NetworkSide.LOCAL && this.isSideOpen(NetworkSide.LOCAL)) {
            Packet250CustomPayload unregisterPacket = new Packet250CustomPayload(NetworkManager.UNREGISTER_CHANNEL, this.getChannel().getBytes(Charsets.UTF_8));
            this.getNetworkManager().getVanillaConnection().func_74429_a(unregisterPacket);
            
            this.packetSplitter.reset();
        }
        
        this.sideOpen[side.ordinal()] = false;
        
        if(this.isClosed()) {
            this.getNetworkManager().connectionClosed(this);
        }
    }
    
    void openSide(NetworkSide side) {
        if(side == NetworkSide.LOCAL && !this.isSideOpen(NetworkSide.LOCAL)) {
            Packet250CustomPayload registerPacket = new Packet250CustomPayload(NetworkManager.REGISTER_CHANNEL, this.getChannel().getBytes(Charsets.UTF_8));
            this.getNetworkManager().getVanillaConnection().func_74429_a(registerPacket);
        }
        
        this.sideOpen[side.ordinal()] = true;
    }
    
    void processPacket(byte[] packetData) {
        if(!this.isSideOpen(NetworkSide.LOCAL)) {
            //Local side is closed. Ignore all packets.
            
            return;
        }
        
        ModPacket packet = this.packetSplitter.readPacket(packetData, this.getPacketRepository());
        if(packet == null) {
            //Invalid packet data or partial packet
            return;
        }
        
        PacketHandler pHandler = this.getPacketHandler();
        if(pHandler != null) {
            try {
                pHandler.handlePacket(this, packet);
            }
            catch(Throwable t) {
                FMLLog.severe("Error handling packet for handler: %s and packet: %s.", pHandler, packet);
            }
        }
    }
    
    
}
