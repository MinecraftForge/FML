package cpw.mods.fml.test.simplenet;

import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

public class SimpleNetHandler1 implements IMessageHandler<SimpleNetTestMessage1, SimpleNetTestMessage2>
{
    @Override
    public SimpleNetTestMessage2 onMessage(SimpleNetTestMessage1 message, MessageContext context)
    {
        System.out.println(this.getClass().getSimpleName() + " recieved message: " + message.getClass().getSimpleName() + " on side: " + context.side.name());
        return null;
    }

}
