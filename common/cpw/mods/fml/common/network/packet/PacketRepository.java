package cpw.mods.fml.common.network.packet;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import cpw.mods.fml.common.FMLLog;

/**
 * 
 * @author Jacob
 *
 * This class is responsible for creating and
 *  managing ModPacket classes
 */
public class PacketRepository {
    
    /**
     * This signifies the starting range of available
     *  packet ids.
     *  
     * Packet ID of 0 is reserved for internal use.
     */
    public static final int STARTING_PACKET_ID = 1;
    
    /**
     * This is the highest packet id that can be used.
     */
    public static final int MAX_PACKET_ID = 255;
    
    
    private final Class<? extends ModPacket>[] packetClasses;
    
    
    
    public PacketRepository() {
        this.packetClasses = new Class[PacketRepository.MAX_PACKET_ID];
    }
    
    /**
     * Registers a packet class with the given id.
<<<<<<< HEAD
     * The ID of 0 is reserved and can not be used
=======
     * The IDs of 1 and 0 are reserved and can not be used
>>>>>>> 3bbf7581015a076c90b1527618475e7378aaa33f
     * 
     * The max id is 255
     * 
     * @param id The id of the packet
     * @param packetClass The class of the packet
     * 
     * @return This instance
     */
    public PacketRepository registerPacket(int id, Class<? extends ModPacket> packetClass) {
        if(id < PacketRepository.STARTING_PACKET_ID || id > PacketRepository.MAX_PACKET_ID) {
            throw new RuntimeException("Invalid packet id: " + id);
        }
        
        if(this.packetClasses[id] != null) {
            throw new RuntimeException(String.format("Packet with id: %d is already registered to: %s", id, this.packetClasses[id]));
        }
        try
        {
            //Verify that the class has a public no-arg constructor
            packetClass.getConstructor();
        }
        catch(Throwable t) {
            throw new RuntimeException(String.format("Invalid packet class: %s. Missing no-arg constructor.", packetClass));
        }
        
        this.packetClasses[id] = packetClass;
        
        return this;
    }
    
    /**
     * Creates a packet with the given id. This may be a
     *  recycled packet. All packets should be recycled once
     *  they are done being used
     *  
     * This will return null if no packet exists with the given id
     *  , if the id is a reserved id, or the id is greater than the
     *  max id.
     *  
     * @param id The id of the packet to create
     * 
     * @return The created packet
     */
    public ModPacket createPacket(int id) {
        if(id < PacketRepository.STARTING_PACKET_ID || id > PacketRepository.MAX_PACKET_ID) {
            return null;
        }
        
        Class<? extends ModPacket> packetClass = packetClasses[id];
        if(packetClass == null) {
            return null;
        }
        
        try
        {
            return packetClass.newInstance();
        }
        catch (InstantiationException e)
        {
            FMLLog.log(Level.SEVERE, e, "Error creating packet class: %s", packetClass);
        }
        catch (IllegalAccessException e) 
        {
            //Should not happen as the class was verified when
            // it was registered
            FMLLog.log(Level.SEVERE, e, "Error creating packet class: %s", packetClass);
        }
        
        return null;
    }
}
