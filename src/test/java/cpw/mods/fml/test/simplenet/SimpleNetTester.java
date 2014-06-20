package cpw.mods.fml.test.simplenet;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;

public class SimpleNetTester {

    private static SimpleNetworkWrapper simpleChannel;

    @Before
    public void setup()
    {
        simpleChannel = NetworkRegistry.INSTANCE.newSimpleChannel("TEST");

        simpleChannel.registerMessage(SimpleNetHandler1.class, SimpleNetTestMessage1.class, 1, Side.SERVER);
        simpleChannel.registerMessage(SimpleNetHandler2.class, SimpleNetTestMessage2.class, 2, Side.CLIENT);
        simpleChannel.registerMessage(SimpleNetHandlerAny.class, SimpleNetTestMessage1.class, 1, Side.CLIENT);
        simpleChannel.registerMessage(SimpleNetHandlerAny.class, SimpleNetTestMessage2.class, 2, Side.SERVER);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testDuplicateChannelException()
    {
        simpleChannel.registerMessage(SimpleNetHandler1.class, SimpleNetTestMessage1.class, 1, Side.SERVER);
    }

    @Test
    public void testMessageSending()
    {
        simpleChannel.sendToAll(new SimpleNetTestMessage1());
        simpleChannel.sendToAll(new SimpleNetTestMessage2());
    }

}
