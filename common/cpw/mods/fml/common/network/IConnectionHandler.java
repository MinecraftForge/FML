package cpw.mods.fml.common.network;

import net.minecraft.server.MinecraftServer;
import net.minecraft.src.NetHandler;
import net.minecraft.src.NetLoginHandler;
import net.minecraft.src.INetworkManager;
import net.minecraft.src.Packet1Login;

public interface IConnectionHandler
{
    /**
     * Called when a player logs into the server
     *  SERVER SIDE
     *
     * @param player
     * @param netHandler
     * @param manager
     */
    void playerLoggedIn(Player player, NetHandler netHandler, INetworkManager manager);

    /**
     * If you don't want the connection to continue, return a non-empty string here
     * If you do, you can do other stuff here- note no FML negotiation has occured yet
     * though the client is verified as having FML installed
     *
     * SERVER SIDE
     *
     * @param netHandler
     * @param manager
     */
    String connectionReceived(NetLoginHandler netHandler, INetworkManager manager);

    /**
     * Fired when a remote connection is opened
     * CLIENT SIDE
     *
     * @param netClientHandler
     * @param server
     * @param port
     */
    void connectionOpened(NetHandler netClientHandler, String server, int port, INetworkManager manager);
    /**
     *
     * Fired when a local connection is opened
     *
     * CLIENT SIDE
     *
     * @param netClientHandler
     * @param server
     */
    void connectionOpened(NetHandler netClientHandler, MinecraftServer server, INetworkManager manager);

    /**
     * Fired when a connection closes
     *
     * ALL SIDES
     *
     * @param manager
     */
    void connectionClosed(INetworkManager manager, Player player);

    /**
     * Fired when the client established the connection to the server
     *
     * CLIENT SIDE
     * @param clientHandler
     * @param manager
     * @param login
     */
    void clientLoggedIn(NetHandler clientHandler, INetworkManager manager, Packet1Login login);

}
