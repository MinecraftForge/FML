package cpw.mods.fml.common.network.mod;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.INetworkManager;
import net.minecraft.network.packet.Packet250CustomPayload;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.network.IPacketHandler;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.PacketDispatcher;
import cpw.mods.fml.common.network.Player;

/**
 * 
 * @author Jacob
 *
 * This class manages a connection between
 *  the client and server over a minecraft
 *  channel.
 *  
 * It handles the reading and
 *  writing of packets sent of a channel. Each
 *  instance can be bound to a different channel
 *  and each instance will have it's own sets of
 *  packets
 */
public class ChannelConnection {
    
    private final String channel;
    private final PacketRepository repo;
    
    private PacketHandler handler;
    
    
    //This stores a list of the byte arrays or a fragmented packet
    // If a packet is greater than the maximum size we must fragment
    // it and send it in smaller chunks. Since we can not read a packet
    // until we have received the full packet it gets stored here
    private ByteArrayOutputStream partialPacket;
    //This signifies the remaining chunks of the fragmented packet
    // If the value is -1 then we are not waiting for more chunks
    private int chunksRemaining;
    
    /**
     * Constructs a ConnectionManager on the given channel
     * 
     * The PacketRepository used by this connection can be
     *  used in multiple connections 
     * 
     * @param channel The channel to write packets to
     * @param repo The repository that contains the packets to use in this connection
     */
    public ChannelConnection(String channel, PacketRepository repo) {
        this(channel, repo, null);
    }
    
    /**
     * Constructs a ConnectionManager on the given channel
     *  with the given handler
     *  
     * The PacketRepository used by this connection can be
     *  used in multiple connections
     *  
     * @param channel The channel to write packets to
     * @param repo The repository that contains the packets to use in this connection
     * @param handler The PacketHandler that will receive incoming packets
     */
    public ChannelConnection(String channel, PacketRepository repo, PacketHandler handler) {
        this.channel = channel;
        this.repo = repo;
        
        this.handler = handler;
        
        this.partialPacket = null;
        this.chunksRemaining = -1;
        
        NetworkRegistry.instance().registerChannel(new PacketDelegate(), channel);
    }
    
    /**
     * Gets the channel this connection is using
     * 
     * @return The channel this connection is using
     */
    public String getChannel() {
        return this.channel;
    }
    
    /**
     * Gets the PacketRepository this connection is
     *  using
     *  
     * @return This connections PacketRepository
     */
    public PacketRepository getPacketRepository() {
        return this.repo;
    }
    
    /**
     * Sets the PacketHandler that will receive incoming packets
     * 
     * @param handler The PacketHandler that will receive incoming packets
     * 
     * @return This instance
     */
    public ChannelConnection setPacketHandler(PacketHandler handler) {
        this.handler = handler;
        
        return this;
    }
    
    /**
     * Sends the packet to the given player
     * 
     * @param player The player to send the packet to
     * @param packet The packet to send to the player
     */
    public void sendPacketToPlayer(Player player, ModPacket packet) {
        List<Packet250CustomPayload> packets = this.getPackets(packet);
        for(Packet250CustomPayload p : packets) {
            PacketDispatcher.sendPacketToPlayer(p, player);
        }
    }
    
    /**
     * Sends the packet to all players
     * 
     * @param packet The packet to send
     */
    public void sendPacketToAllPlayers(ModPacket packet) {
        List<Packet250CustomPayload> packets = this.getPackets(packet);
        for(Packet250CustomPayload p : packets) {
            PacketDispatcher.sendPacketToAllPlayers(p);
        }
    }
    
    /**
     * Sends this packet to the server
     * 
     * @param packet The packet to send
     */
    public void sendPacketToServer(ModPacket packet) {
        List<Packet250CustomPayload> packets = this.getPackets(packet);
        for(Packet250CustomPayload p : packets) {
            PacketDispatcher.sendPacketToServer(p);
        }
    }
    
    private List<Packet250CustomPayload> getPackets(ModPacket packet) {
        List<Packet250CustomPayload> packets = new ArrayList<Packet250CustomPayload>();
        
        try {
            
            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            DataOutputStream dataOut = new DataOutputStream(byteOut);
            
            dataOut.writeByte(packet.getID());
            packet.writePacketData(dataOut);
            
            byte[] data = byteOut.toByteArray();
            
            if(data.length > 32767) {
                //Packet is to big. We must fragment it
                
                //We add 5 to the total length for the added space
                // needed for the header
                int totalLength = data.length + 5;
                int totalChunks = totalLength / PacketRepository.MAX_PACKET_SIZE;
                
                if((totalLength % PacketRepository.MAX_PACKET_SIZE) != 0) {
                    //If the length does not evenly fit
                    // we need to send an additional chunk with the remainder
                    totalChunks += 1;
                }
                
                int position = 0;

                for(int i=0; i<totalChunks; i++) {
                    //Gets the length of this chunk
                    int length = Math.min(data.length - position, PacketRepository.MAX_PACKET_SIZE);
                    
                    byte[] chunk = new byte[length];
                    if(i == 0) {
                        //The first chunk needs additional header information
                        
                        //Packet ID of 1 signifies fragmented packet
                        chunk[0] = 1;
                        //The next four bytes are the total amount of chunks
                        // in the fragmented packet stored in Big-Endian order
                        chunk[1] = (byte) ((totalChunks >> 24) & 0xFF);
                        chunk[2] = (byte) ((totalChunks >> 16) & 0xFF);
                        chunk[3] = (byte) ((totalChunks >> 8) & 0xFF);
                        chunk[4] = (byte) (totalChunks & 0xFF);
                        
                        System.arraycopy(data, position, chunk, 5, length - 5);
                        
                        position += length - 5;
                    }
                    else {
                        System.arraycopy(data, position, chunk, 0, length);
                        
                        position += length;
                    }
                    
                    Packet250CustomPayload payload = new Packet250CustomPayload(this.channel, chunk);
                    packets.add(payload);
                }
            }
            else {
                //Small packet it can fit without fragmentation
                Packet250CustomPayload payload = new Packet250CustomPayload(this.channel, data);
                packets.add(payload);
            }
        }
        catch(IOException ioe) {
            //This should never happen.
        }
        return packets;
    }
    
    private void handleReadPacket(Packet250CustomPayload packet, Player player) throws IOException {
        if(this.chunksRemaining != -1) {
            //We are waiting on more chunks to finish a packet
            
            this.partialPacket.write(packet.field_73629_c);
            
            this.chunksRemaining--;
        }
        
        byte[] data;
        
        if(this.chunksRemaining == 0) {
            //We have read in the last chunk of the packet.
            // We can now read in the full packet.
            data = this.partialPacket.toByteArray();
            
            //Null the stream to allow the data to be recollected later
            this.partialPacket = null;
            //Reset chunk remaining count
            this.chunksRemaining = -1;
        }
        else if(this.chunksRemaining > 0){
            //We are still waiting on more packets
            return;
        }
        else {
            data = packet.field_73629_c;
            
            
            if(data[0] == 1) {
                //ID of 1 signifies the start of fragmented packet
                this.partialPacket = new ByteArrayOutputStream();
                
                //The four bytes after the id are the number of chunks
                // of the fragmented packet in Big-Endian order
                this.chunksRemaining = ((data[1] & 0xFF) << 24) | ((data[2] & 0xFF) << 16)
                        | ((data[3] & 0xFF) << 8) | (data[4] & 0xFF);;
                
                
                if(this.chunksRemaining == 1) {
                    //There should never be a fragmented packet with only 1 chunk.
                    // For now log the error and handle the packet as if it wasn't a
                    // fragmented packet
                    FMLLog.warning("Recieved fragmented packet with only 1 chunk.");
                    
                    byte[] newData = new byte[data.length - 5];
                    System.arraycopy(data, 5, newData, 0, newData.length);
                    data = newData;
                    
                    //Reset chunk remaining count
                    this.chunksRemaining = -1;
                }
                else {
                    this.partialPacket.write(data, 5, data.length - 5);
                    this.chunksRemaining--;
                
                    return;
                }
            }
        }
        
        ModPacket mPacket = this.readPacket(data);
        
        if(mPacket != null) {
            if(this.handler != null) {
                //It is possible for the handler to be null. If it is we can
                // ignore the packet.
                this.handler.handlePacket(this, player, mPacket);
            }
            if(mPacket instanceof RecyclablePacket) {
                //Recycle the packet if possible
                PacketRepository.recyclePacket((RecyclablePacket)mPacket);
            }
        }
    }
    
    private ModPacket readPacket(byte[] data) throws IOException {
        ByteArrayInputStream byteIn = new ByteArrayInputStream(data);
        DataInputStream dataIn = new DataInputStream(byteIn);
        
        int id = dataIn.readUnsignedByte();
        
        ModPacket packet = this.repo.createPacket(id);
        if(packet == null) {
            FMLLog.severe("Invalid packet id: %d", id);
            return null;
        }
        
        packet.readPacketData(dataIn);
        
        return packet;
    }
    
    
    private class PacketDelegate implements IPacketHandler {

        @Override
        public void onPacketData(INetworkManager manager, Packet250CustomPayload packet, Player player)
        {
            try
            {
                ChannelConnection.this.handleReadPacket(packet, player);
            }
            catch(Throwable t) {
                //Catch any exceptions that may be thrown from the mod code
                // to prevent it from being sent up the stack and affecting
                // other things
                FMLLog.log(Level.SEVERE, t, "Exception while reading packet: %s. Discarding packet.", t.getMessage());
            }
        }
        
    }
}
