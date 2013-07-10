package cpw.mods.fml.common.network.packet;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;

import cpw.mods.fml.common.network.IConnectionHandler;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.Player;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.INetworkManager;
import net.minecraft.network.NetLoginHandler;
import net.minecraft.network.NetServerHandler;
import net.minecraft.network.packet.NetHandler;
import net.minecraft.network.packet.Packet1Login;
import net.minecraft.network.packet.Packet250CustomPayload;
import net.minecraft.server.MinecraftServer;

public class NetworkManager {

    private final Player owner;
    private final INetworkManager vanillaConnection;
    
    private final Map<String, ChannelNetworkConnection> channelConnections;
    
    private NetworkManager(Player owner, INetworkManager vanillaConnection) {
        Preconditions.checkNotNull(owner, "owner");
        Preconditions.checkNotNull(vanillaConnection, "vanillaConnection");
        
        this.owner = owner;
        this.vanillaConnection = vanillaConnection;
        
        this.channelConnections = new HashMap<String, ChannelNetworkConnection>();
    }
    
    /**
     * Gets the owner of this NetworkManager
     * 
     * @return The owner of this NetworkManager
     */
    public Player getOwner() {
        return this.owner;
    }
    
    /**
     * Gets the connection for the given channel or
     *  null if no connection exists
     *     
     * @param channel The channel
     * 
     * @return The connection for the given channel
     */
    public ChannelNetworkConnection getConnection(String channel) {
        NetworkManager.validateChannelName(channel);
        
        return this.channelConnections.get(channel);
    }
    
    /**
     * Opens a new connection if one does not exists.
     * 
     * If a connection exists on the given channel
     *  and NetworkSide.LOCAL is open an exception
     *  is thrown.
     *  
     * @param channel The channel
     * @param handler The initial PacketHandler
     * 
     * @return The new connection if one does not already exist for the given channel
     */
    public ChannelNetworkConnection openConnection(String channel, PacketHandler handler) {
        Preconditions.checkNotNull(handler, "handler");
        
        ChannelNetworkConnection connection = this.getConnection(channel);
        if(connection == null) {
            connection = new ChannelNetworkConnection(this, channel);
            
            this.channelConnections.put(channel, connection);
        }
        if(connection.isSideOpen(NetworkSide.LOCAL)) {
            throw new IllegalStateException(String.format("Channel: %s is already open on NetworkSide.LOCAL", channel));
        }
        
        connection.openSide(NetworkSide.LOCAL);
        connection.setPacketHandler(handler);
        
        return connection;
    }
    
    /**
     * Gets all open channel names. These include channels that are
     *  one-way channels to or from the destination
     *  
     * @return All open channels
     */
    public Set<String> getOpenChannels() {
        return Collections.unmodifiableSet(this.channelConnections.keySet());
    }
    
    INetworkManager getVanillaConnection() {
        return this.vanillaConnection;
    }
    
    void connectionClosed(ChannelNetworkConnection connection) {
        this.channelConnections.remove(connection.getChannel());
    }
    
    //Maps player instances to their NetworkManagers. Ideally this would also be contained in the Player interface
    private static final Map<Player, NetworkManager> networkManagers = new HashMap<Player, NetworkManager>();
    
    
    //Constants to define some commonly used values
    public static final String UNREGISTER_CHANNEL = "UNREGISTER";
    public static final String REGISTER_CHANNEL = "REGISTER";
    
    public static final int MAX_PACKET_SIZE = Short.MAX_VALUE - 1;
    
    /**
     * Gets the NetworkManager for the given player
     * 
     * @param player The player
     * 
     * @return The NetworkManager for the given player
     */
    public static NetworkManager getNetworkManager(Player player) {
        return NetworkManager.networkManagers.get(player);
    }
    
    /**
     * Validates if the given channel is an acceptable
     *  channel. Throws an exception if the channel
     *  is an invalid channel
     *  
     * @param channelName The channel name to validate
     */
    public static void validateChannelName(String channelName) {
        if(channelName == null || channelName.length() == 0 || channelName.length() > 20) {
            throw new IllegalArgumentException("Invalid channel name: " + channelName);
        }
    }
    
    
    public static void playerLoggedIn(Player player, INetworkManager manager) {
        if(NetworkManager.getNetworkManager(player) != null) {
            throw new IllegalStateException("Player already logged in: " + ((EntityPlayerMP)player).func_70005_c_());
        }
        
        NetworkManager.networkManagers.put(player, new NetworkManager(player, manager));
    }
    
    public static void handleCustomPacketData(Player player, Packet250CustomPayload packet) {
        NetworkManager manager = NetworkManager.getNetworkManager(player);
        if(manager == null) {
            throw new IllegalStateException("Player not logged in: " + ((EntityPlayerMP)player).func_70005_c_());
        }
        
        String channel = packet.field_73630_a;
        NetworkManager.validateChannelName(channel);
        
        ChannelNetworkConnection connection = null;
        if(NetworkManager.REGISTER_CHANNEL.equals(channel)) {
            if(packet.field_73629_c != null) {
                for(String channelName : new String(packet.field_73629_c, Charsets.UTF_8).split("\0")) {
                    connection = manager.getConnection(channelName);
                    if(connection == null) {
                        //If the connection has not been opened on the local side
                        connection = new ChannelNetworkConnection(manager, channelName);
                    }
                    connection.openSide(NetworkSide.REMOTE);
                }
            }
            
            return;
        }
        else if(NetworkManager.UNREGISTER_CHANNEL.equals(channel)) {
            if(packet.field_73629_c != null) {
                for(String channelName : new String(packet.field_73629_c, Charsets.UTF_8).split("\0")) {
                    connection = manager.getConnection(channelName);
                    if(connection != null) {
                        connection.closeSide(NetworkSide.REMOTE);
                    }
                }
            }
            
            return;
        }
        
        connection = manager.getConnection(channel);
        if(connection == null) {
            //Connection was never registered for the given channel
            return;
        }
        
        connection.processPacket(packet.field_73629_c);
    }
}
