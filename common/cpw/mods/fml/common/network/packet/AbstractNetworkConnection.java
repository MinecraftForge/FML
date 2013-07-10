package cpw.mods.fml.common.network.packet;

import com.google.common.base.Preconditions;

/**
 * 
 * @author Jacob
 *
 * Base class for NetworkConnections. Provides handling of
 *  PacketHandler and PacketRepository
 */
public abstract class AbstractNetworkConnection implements NetworkConnection {

    private PacketRepository packetRepo;
    private PacketHandler packetHandler;
    
    protected AbstractNetworkConnection() {
        this(new PacketRepository());
    }
    
    protected AbstractNetworkConnection(PacketRepository packetRepo) {
        Preconditions.checkNotNull(packetRepo, "packetRepo");
        
        this.packetRepo = packetRepo;
        this.packetHandler = null;
    }

    @Override
    public void setPacketHandler(PacketHandler handler) {
        this.packetHandler = handler;
    }

    @Override
    public PacketHandler getPacketHandler() {
        return this.packetHandler;
    }

    @Override
    public PacketRepository getPacketRepository() {
        return this.packetRepo;
    }

    @Override
    public void setPacketRepository(PacketRepository repo) {
        Preconditions.checkNotNull(repo, "repo");
        
        this.packetRepo = repo;
    }

}
