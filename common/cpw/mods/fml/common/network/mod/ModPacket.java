package cpw.mods.fml.common.network.mod;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * 
 * @author Jacob
 *
 * The base class for all packets that mods
 *  will create
 *  
 * Any packet that extends this class must provide
 *  a public no-arg constructor that will be used
 *  to create new packets for reading.
 *  
 * A packet may be read multiple times without creating
 *  a new instance
 */
public abstract class ModPacket {

    private int id;
    
    protected ModPacket(int id) {
        this.id = id;
    }
    
    public final int getID() {
        return this.id;
    }
    
    protected abstract void writePacketData(DataOutput out) throws IOException;
    
    protected abstract void readPacketData(DataInput in) throws IOException;
}
