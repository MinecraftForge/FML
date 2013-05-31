package cpw.mods.fml.common.network.mod;

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
     *  packet ids. Packet ids of 0 and 1 are reserved
     *  for internal use.
     *  
     * ID 0: Reserved
     * ID 1: Is used to signify the start of a fragmented
     *          packet. The 4 bytes following the id are
     *          the number of chunks including the current
     *          chunk that make up the full packet in BigEndian
     *          order
     */
    public static final int STARTING_PACKET_ID = 2;
    
    /**
     * This is the highest packet id that can be used.
     */
    public static final int MAX_PACKET_ID = 255;
    
    /**
     * This is a constant representing the max size that
     *  can be sent in Packet250CustomPayload.
     *  
     * The minus one is due to Issue MC-16910
     *  
     */
    public static final int MAX_PACKET_SIZE = 32767 - 1;
    
    //Arbitrary number to specify how many packets of each
    // id to keep when recycling. Anything more than this
    // will get discarded.
    private static final int MAX_RECYCLED_PACKETS = 100;
    
    //We use a global map because Packet classes can be used
    // across multiple PacketRepositories
    //Even though the generics state any class that extends ModPacket
    // can be used as a key. Only classes that extend RecyclablePacket
    // should be used. This is done to reduce the amount of casting
    // needed
    private static final Map<Class<? extends ModPacket>, List<RecyclablePacket>> recycledPackets;
    
    
    private final Class<? extends ModPacket>[] packetClasses;
    
    
    
    public PacketRepository() {
        this.packetClasses = new Class[PacketRepository.MAX_PACKET_ID];
    }
    
    /**
     * Registers a packet class with the given id.
     * The IDs of 1 and 0 are reserved and can not be used
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
        if(packetClass.isAssignableFrom(RecyclablePacket.class)) {
            if(!PacketRepository.recycledPackets.containsKey(packetClass)) {
                PacketRepository.recycledPackets.put(packetClass, new LinkedList<RecyclablePacket>());
            }
        }
        
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
        List<RecyclablePacket> recycledPackets = PacketRepository.recycledPackets.get(packetClass);
        if(recycledPackets != null && recycledPackets.size() > 0) {
            //We have recycled packets. Return the next available one
            // instead of creating a new one.
            return recycledPackets.remove(0);
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
    
    /**
     * Recycles the packet for future use. Once this method
     *  is called the packet is available for use in
     *  any PacketRepository and references should not be 
     *  kept to the packet after this call completes.
     *  
     * @param packet The packet to recycle
     */
    public static void recyclePacket(RecyclablePacket packet) {
        //Clean up the packet's resources
        packet.recycle();

        Class<? extends ModPacket> packetClass = packet.getClass();
        List<RecyclablePacket> packets = PacketRepository.recycledPackets.get(packetClass);
        if(packets == null) {
            throw new IllegalArgumentException("Packet is not registered in any PacketRepository: " + packetClass);
        }
        if(packets.size() < PacketRepository.MAX_RECYCLED_PACKETS) {
            //There is still room left to recycle the packet
            
            packets.add(packet);
        }
    }
    
    static {
        recycledPackets = new HashMap<Class<? extends ModPacket>, List<RecyclablePacket>>();
    }
}
