package cpw.mods.fml.common.network.packet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.packet.Packet250CustomPayload;

import com.google.common.base.Preconditions;

import cpw.mods.fml.common.FMLLog;

/**
 * 
 * @author Jacob
 *
 * A utility class for reading and writing
 *  ModPackets over minecraft channels.
 */
public class PacketSplitter {
<<<<<<< HEAD
    
    public static final int FRAGEMENT_PACKET_ID = 0;
=======
>>>>>>> 3bbf7581015a076c90b1527618475e7378aaa33f

    private final String channel;
    
    //Variables used in the reading of packets
    private int chunksRemaining;
    private ByteArrayOutputStream partialPacket;
    
    public PacketSplitter(String channel) {
        NetworkManager.validateChannelName(channel);
        
        this.channel = channel;
    }
    
    public String getChannel() {
        return this.channel;
    }
    
    public List<Packet250CustomPayload> getPackets(ModPacket packet) {
        Preconditions.checkNotNull(packet, "packet");
        
        List<Packet250CustomPayload> packets = new ArrayList<Packet250CustomPayload>();
        
        try {
            
            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            DataOutputStream dataOut = new DataOutputStream(byteOut);
            
            dataOut.writeByte(packet.getID());
            packet.writePacketData(dataOut);
            
            byte[] data = byteOut.toByteArray();
            
            if(data.length > NetworkManager.MAX_PACKET_SIZE) {
                //Packet is to big. We must fragment it
                
                //We add 5 to the total length for the added space
                // needed for the header
                int totalLength = data.length + 5;
                int totalChunks = totalLength / NetworkManager.MAX_PACKET_SIZE;
                
                if((totalLength % NetworkManager.MAX_PACKET_SIZE) != 0) {
                    //If the length does not evenly fit
                    // we need to send an additional chunk with the remainder
                    totalChunks += 1;
                }
                
                int position = 0;

                for(int i=0; i<totalChunks; i++) {
                    //Gets the length of this chunk
                    int length = Math.min(data.length - position, NetworkManager.MAX_PACKET_SIZE);
                    
                    byte[] chunk = new byte[length];
                    if(i == 0) {
                        //The first chunk needs additional header information
                        
<<<<<<< HEAD
                        //Packet ID of 0 signifies fragmented packet
                        chunk[0] = PacketSplitter.FRAGEMENT_PACKET_ID;
=======
                        //Packet ID of 1 signifies fragmented packet
                        chunk[0] = 0;
>>>>>>> 3bbf7581015a076c90b1527618475e7378aaa33f
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
                    
                    Packet250CustomPayload payload = new Packet250CustomPayload(this.getChannel(), chunk);
                    packets.add(payload);
                }
            }
            else {
                //Small packet it can fit without fragmentation
                Packet250CustomPayload payload = new Packet250CustomPayload(this.getChannel(), data);
                packets.add(payload);
            }
        }
        catch(IOException ioe) {
            //This should never happen.
        }
        return packets;
    }
    
    public ModPacket readPacket(byte[] packetData, PacketRepository repo) {
        if(packetData == null) {
            return null;
        }
        try {
            if(this.chunksRemaining != -1) {
                //We are waiting on more chunks to finish a packet
                
                this.partialPacket.write(packetData);
                
                this.chunksRemaining--;
            }
            
            byte[] data;
            
            if(this.chunksRemaining == 0) {
                //We have read in the last chunk of the packet.
                // We can now read in the full packet.
                data = this.partialPacket.toByteArray();
                
                this.reset();
            }
            else if(this.chunksRemaining > 0){
                //We are still waiting on more packets
                return null;
            }
            else {
                data = packetData;
                
                
<<<<<<< HEAD
                if(data[0] == PacketSplitter.FRAGEMENT_PACKET_ID) {
                    //ID of 0 signifies the start of fragmented packet
=======
                if(data[0] == 1) {
                    //ID of 1 signifies the start of fragmented packet
>>>>>>> 3bbf7581015a076c90b1527618475e7378aaa33f
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
                        
                        this.reset();
                    }
                    else {
                        this.partialPacket.write(data, 5, data.length - 5);
                        this.chunksRemaining--;
                    
                        return null;
                    }
                }
            }
            
            DataInputStream dataIn = new DataInputStream(new ByteArrayInputStream(data));
            
            int packetId = dataIn.readUnsignedByte();
            
            ModPacket packet = repo.createPacket(packetId);
            if(packet == null) {
                FMLLog.warning("Invalid packet id: %d.", packetId);
                return null;
            }
            
            packet.readPacketData(dataIn);
        }
        catch(Throwable t) {
            FMLLog.warning("Error reading in packet on channel: %s.", this.getChannel());
        }
        
        return null;
    }
    
    public void reset() {
        this.chunksRemaining = -1;
        this.partialPacket = null;
    }
}
