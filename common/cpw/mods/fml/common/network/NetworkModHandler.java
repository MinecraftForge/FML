package cpw.mods.fml.common.network;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Set;
import java.util.logging.Level;

import net.minecraft.src.EntityPlayerMP;
import net.minecraft.src.NetServerHandler;
import net.minecraft.src.NetworkManager;

import org.objectweb.asm.Type;

import com.google.common.base.Strings;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.discovery.ASMDataTable;
import cpw.mods.fml.common.discovery.ASMDataTable.ASMData;
import cpw.mods.fml.common.versioning.DefaultArtifactVersion;
import cpw.mods.fml.common.versioning.InvalidVersionSpecificationException;
import cpw.mods.fml.common.versioning.VersionRange;

public class NetworkModHandler
{
    private static Object connectionHandlerDefaultValue;
    private static Object packetHandlerDefaultValue;
    private static Object clientHandlerDefaultValue;
    private static Object serverHandlerDefaultValue;

    private static int assignedIds = 1;

    private int localId;
    private int networkId;

    private ModContainer container;
    private NetworkMod mod;
    private Method checkHandler;

    private VersionRange acceptableRange;

    public NetworkModHandler(ModContainer container, NetworkMod modAnnotation)
    {
        this.container = container;
        this.mod = modAnnotation;
        this.localId = assignedIds++;
        this.networkId = this.localId;
    }
    public NetworkModHandler(ModContainer container, Class<?> networkModClass, ASMDataTable table)
    {
        this(container, networkModClass.getAnnotation(NetworkMod.class));
        if (this.mod == null)
        {
            return;
        }

        Set<ASMData> versionCheckHandlers = table.getAnnotationsFor(container).get(NetworkMod.VersionCheckHandler.class.getName());
        String versionCheckHandlerMethod = null;
        for (ASMData vch : versionCheckHandlers)
        {
            if (vch.getClassName().equals(networkModClass.getName()))
            {
                versionCheckHandlerMethod = vch.getObjectName();
                break;
            }
        }
        if (versionCheckHandlerMethod != null)
        {
            try
            {
                Method checkHandlerMethod = networkModClass.getDeclaredMethod(versionCheckHandlerMethod, String.class);
                if (checkHandlerMethod.isAnnotationPresent(NetworkMod.VersionCheckHandler.class))
                {
                    this.checkHandler = checkHandlerMethod;
                }
            }
            catch (Exception e)
            {
                FMLLog.log(Level.WARNING, e, "The declared version check handler method %s on network mod id %s is not accessible", versionCheckHandlerMethod, container.getModId());
            }
        }

        if (this.checkHandler == null)
        {
            String versionBounds = mod.versionBounds();
            if (!Strings.isNullOrEmpty(versionBounds))
            {
                try
                {
                    this.acceptableRange = VersionRange.createFromVersionSpec(versionBounds);
                }
                catch (InvalidVersionSpecificationException e)
                {
                    FMLLog.log(Level.WARNING, e, "Invalid bounded range %s specified for network mod id %s", versionBounds, container.getModId());
                }
            }
        }

        tryCreatingPacketHandler(container, mod.packetHandler(), mod.channels());
        if (mod.clientPacketHandlerSpec() != getClientHandlerSpecDefaultValue())
        {
            tryCreatingPacketHandler(container, mod.clientPacketHandlerSpec().packetHandler(), mod.clientPacketHandlerSpec().channels());
        }
        if (mod.serverPacketHandlerSpec() != getServerHandlerSpecDefaultValue())
        {
            tryCreatingPacketHandler(container, mod.serverPacketHandlerSpec().packetHandler(), mod.serverPacketHandlerSpec().channels());
        }

        if (mod.connectionHandler() != getConnectionHandlerDefaultValue())
        {
            IConnectionHandler instance;
            try
            {
                instance = mod.connectionHandler().newInstance();
            }
            catch (Exception e)
            {
                FMLLog.log(Level.SEVERE, e, "Unable to create connection handler instance %s", mod.connectionHandler().getName());
                throw new FMLNetworkException(e);
            }

            NetworkRegistry.instance().registerConnectionHandler(instance);
        }
    }
    /**
     * @param container
     */
    private void tryCreatingPacketHandler(ModContainer container, Class<? extends IPacketHandler> clazz, String[] channels)
    {

        if (clazz!=getPacketHandlerDefaultValue())
        {
            if (channels.length==0)
            {
                FMLLog.log(Level.WARNING, "The mod id %s attempted to register a packet handler without specifying channels for it", container.getModId());
            }
            else
            {
                IPacketHandler instance;
                try
                {
                    instance = clazz.newInstance();
                }
                catch (Exception e)
                {
                    FMLLog.log(Level.SEVERE, e, "Unable to create a packet handler instance %s for mod %s", clazz.getName(), container.getModId());
                    throw new FMLNetworkException(e);
                }

                for (String channel : channels)
                {
                    NetworkRegistry.instance().registerChannel(instance, channel);
                }
            }
        }
        else if (channels.length > 0)
        {
            FMLLog.warning("The mod id %s attempted to register channels without specifying a packet handler", container.getModId());
        }
    }
    /**
     * @return
     */
    private Object getConnectionHandlerDefaultValue()
    {
        try {
            if (connectionHandlerDefaultValue == null)
            {
                connectionHandlerDefaultValue = NetworkMod.class.getMethod("connectionHandler").getDefaultValue();
            }
            return connectionHandlerDefaultValue;
        }
        catch (NoSuchMethodException e)
        {
            throw new RuntimeException("Derp?", e);
        }
    }

    /**
     * @return
     */
    private Object getPacketHandlerDefaultValue()
    {
        try {
            if (packetHandlerDefaultValue == null)
            {
                packetHandlerDefaultValue = NetworkMod.class.getMethod("packetHandler").getDefaultValue();
            }
            return packetHandlerDefaultValue;
        }
        catch (NoSuchMethodException e)
        {
            throw new RuntimeException("Derp?", e);
        }
    }

    /**
     * @return
     */
    private Object getClientHandlerSpecDefaultValue()
    {
        try {
            if (clientHandlerDefaultValue == null)
            {
                clientHandlerDefaultValue = NetworkMod.class.getMethod("clientPacketHandlerSpec").getDefaultValue();
            }
            return clientHandlerDefaultValue;
        }
        catch (NoSuchMethodException e)
        {
            throw new RuntimeException("Derp?", e);
        }
    }
    /**
     * @return
     */
    private Object getServerHandlerSpecDefaultValue()
    {
        try {
            if (serverHandlerDefaultValue == null)
            {
                serverHandlerDefaultValue = NetworkMod.class.getMethod("serverPacketHandlerSpec").getDefaultValue();
            }
            return serverHandlerDefaultValue;
        }
        catch (NoSuchMethodException e)
        {
            throw new RuntimeException("Derp?", e);
        }
    }
    public boolean requiresClientSide()
    {
        return mod.clientSideRequired();
    }

    public boolean requiresServerSide()
    {
        return mod.serverSideRequired();
    }

    public boolean acceptVersion(String version)
    {
        if (checkHandler != null)
        {
            try
            {
                return (Boolean)checkHandler.invoke(container.getMod(), version);
            }
            catch (Exception e)
            {
                FMLLog.log(Level.WARNING, e, "There was a problem invoking the checkhandler method %s for network mod id %s", checkHandler.getName(), container.getModId());
                return false;
            }
        }

        if (acceptableRange!=null)
        {
            return acceptableRange.containsVersion(new DefaultArtifactVersion(version));
        }

        return container.getVersion().equals(version);
    }

    public int getLocalId()
    {
        return localId;
    }

    public int getNetworkId()
    {
        return networkId;
    }

    public ModContainer getContainer()
    {
        return container;
    }

    public NetworkMod getMod()
    {
        return mod;
    }

    public boolean isNetworkMod()
    {
        return mod != null;
    }

    public void setNetworkId(int value)
    {
        this.networkId = value;
    }
}
