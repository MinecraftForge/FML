package cpw.mods.fml.common.network;

import static cpw.mods.fml.common.network.FMLPacket.Type.MOD_IDENTIFIERS;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import net.minecraft.src.NBTBase;
import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.NetHandler;
import net.minecraft.src.NetworkManager;

import com.google.common.collect.Maps;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import cpw.mods.fml.common.registry.GameRegistry;

public class ModIdentifiersPacket extends FMLPacket
{
    private Map<String, Integer> modIds = Maps.newHashMap();
    private NBTTagCompound blockData;

    public ModIdentifiersPacket()
    {
        super(MOD_IDENTIFIERS);
    }

    @Override
    public byte[] generatePacket(Object... data)
    {
        ByteArrayDataOutput dat = ByteStreams.newDataOutput();
        Collection<NetworkModHandler >networkMods = FMLNetworkHandler.instance().getNetworkIdMap().values();

        dat.writeInt(networkMods.size());
        for (NetworkModHandler handler : networkMods)
        {
            dat.writeUTF(handler.getContainer().getModId());
            dat.writeInt(handler.getNetworkId());
        }

        try
        {
            NBTBase.func_74731_a(GameRegistry.getBlockData(), dat);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        // TODO send the other id maps as well
        return dat.toByteArray();
    }

    @Override
    public FMLPacket consumePacket(byte[] data)
    {
        ByteArrayDataInput dat = ByteStreams.newDataInput(data);
        int listSize = dat.readInt();
        for (int i = 0; i < listSize; i++)
        {
            String modId = dat.readUTF();
            int networkId = dat.readInt();
            modIds.put(modId, networkId);
        }

        try
        {
            NBTBase blockData = NBTBase.func_74739_b(dat);
            if (!(blockData instanceof NBTTagCompound)) {
                throw new RuntimeException("Invalid data in packet");
            }
            this.blockData = (NBTTagCompound) blockData;
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        return this;
    }

    @Override
    public void execute(NetworkManager network, FMLNetworkHandler handler, NetHandler netHandler, String userName)
    {
        for (Entry<String,Integer> idEntry : modIds.entrySet())
        {
            handler.bindNetworkId(idEntry.getKey(), idEntry.getValue());
        }
        // TODO other id maps

        GameRegistry.setBlockData(blockData);
    }
}
