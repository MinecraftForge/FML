package cpw.mods.fml.common.network.mod;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * 
 * @author Jacob
 *
 * This class is the base class for packets
 *  that support being recycled. Recycled
 *  packets are used to reduce object churn
 *  when being used on an active network.
 *  
 * All packets should extend this class unless
 *  there is a good reason not to.
 */
public abstract class RecyclablePacket extends ModPacket {

    protected RecyclablePacket(int id)
    {
        super(id);
    }

    /**
     * Recycles the packet. This is called when a packet
     *  is being recycled. It should clean up any resources
     *  it has to free memory and to prepare it to be read
     *  in again
     */
    protected abstract void recycle();
}
