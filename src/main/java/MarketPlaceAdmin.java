import IndyLibraries.JSONUserCredentialStorage;
import IndyLibraries.Trustee;
import org.hyperledger.indy.sdk.IndyException;
import org.hyperledger.indy.sdk.pool.Pool;

import javax.crypto.NoSuchPaddingException;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutionException;

public class MarketPlaceAdmin {
    String trusteeSeed;
    Trustee indyAdmin;
    ServerSocketChannel AgentListenChannel;
    InetSocketAddress listeningAddress;

    public MarketPlaceAdmin(String poolName){//default trustee
        String trusteeSeed = "000000000000000000000000Trustee1";
        Pool pool=null;
        JSONUserCredentialStorage jsonStoredCred = null;
        try {
            Pool.setProtocolVersion(2).get();
            pool = Pool.openPoolLedger(poolName, "{}").get();
            AgentListenChannel = ServerSocketChannel.open();
            AgentListenChannel.socket().bind(null);

            listeningAddress = (InetSocketAddress) AgentListenChannel.getLocalAddress();
            System.out.println("IndyLibraries.Agent IndyLibraries.Trustee Listening Adress:" + listeningAddress);

            File agentsFile = new File("./" + "mainTrustee" + ".json");

            jsonStoredCred = new JSONUserCredentialStorage(agentsFile);
        } catch (IndyException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        indyAdmin = new Trustee(pool,"mainTrustee",jsonStoredCred);
        indyAdmin.CreateWallet("mainTrustee", "pass");
        indyAdmin.OpenWallet("mainTrustee", "pass");
        indyAdmin.createDID(trusteeSeed);

        //add entpoint to NYM Object in Ledger to Make IndyLibraries.Trustee contactable
        indyAdmin.addENdpointToNYM("did2did","127.0.0.1:"+listeningAddress.getPort());

    }

}
