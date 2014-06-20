package cpw.mods.fml.test.simplenet;

import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;

public class SimpleNetHandlerAny implements IMessageHandler<SimpleNetTestMessageBase, SimpleNetTestMessageBase>
{
    private static boolean sentMessage2 = false;
    
    @Override
    public SimpleNetTestMessageBase onMessage(SimpleNetTestMessageBase message, MessageContext context)
    {
        System.out.println(this.getClass().getSimpleName() + " recieved message: " + message.getClass().getSimpleName() + " on side: " + context.side.name());
        
        if (!sentMessage2 && context.side == Side.CLIENT && message instanceof SimpleNetTestMessage1)
        {
            sentMessage2 = true;
            return new SimpleNetTestMessage2();
        }
        else if (context.side == Side.SERVER)
            return new SimpleNetTestMessage1();
        else
            return null;
    }

}
