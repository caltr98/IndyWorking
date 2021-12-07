package OldTests;

import IndyLibraries.DID2DIDComm;
import org.hyperledger.indy.sdk.IndyException;
import org.hyperledger.indy.sdk.wallet.Wallet;
import org.json.JSONObject;

import java.io.IOException;
import java.net.*;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;

public class AgentMain {
    public static void main(String[] args) {
        //This would be a General IndyLibraries.Agent, It will adapt to be a prover/verifier/issuer changing it's capabilities!
        ServerSocketChannel AgentListenChannel;
        InetSocketAddress listeningAdress;
        Scanner sc = new Scanner(System.in);
        System.out.println("1 for flusso server \n 2 for flusso client");
        DID2DIDComm did2did=new DID2DIDComm();
        int ris=sc.nextInt();
        if (ris==1) {
            try {
                Wallet wallet=null;
                //Wallet.createWallet(new JSONObject().put("id", "testWall2").toString(4),
                  //      new JSONObject().put("key", "ci").toString(4)).get();
                wallet=Wallet.openWallet(new JSONObject().put("id", "testWall2").toString(4),
                        new JSONObject().put("key", "ci").toString(4)).get();
                AgentListenChannel =ServerSocketChannel.open();
                AgentListenChannel.socket().bind(null);

                listeningAdress = (InetSocketAddress) AgentListenChannel.getLocalAddress();
                System.out.println("IndyLibraries.Agent Listening Adress:" + listeningAdress);
                System.out.println("pre accept");
                SocketChannel clientSockChannel= AgentListenChannel.accept();
                System.out.println("post accept");

                String msg = DID2DIDComm.readMessage(clientSockChannel.socket());
                clientSockChannel.configureBlocking(true);
                ArrayList<Byte> byteToread= new ArrayList<Byte>();
                System.out.println(msg);
                String sendedmsg=DID2DIDComm.
                        createDIDtoDID(wallet,msg,
                                "SERVERDID","SERVERVERKEY"
                        ,"agentName");
                System.out.println(sendedmsg);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (IndyException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        else{
            Wallet wallet=null;
            try {
                //Wallet.createWallet(new JSONObject().put("id", "testWall").toString(4),
                //        new JSONObject().put("key", "ci").toString(4)).get();
                wallet=Wallet.openWallet(new JSONObject().put("id", "testWall").toString(4),
                        new JSONObject().put("key", "ci").toString(4)).get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (IndyException e) {
                e.printStackTrace();
            }
            System.out.println("INSERT IP");
            String IpToConnect = sc.next() ;
            System.out.println("Insert PORT");
            int PortToConnect= sc.nextInt();
            //Socket socket = new Socket();
            SocketChannel socketChannel = null;
            InetSocketAddress otherAgentSocketAddr = new InetSocketAddress(IpToConnect,PortToConnect);
            try {
                socketChannel =SocketChannel.open();
                socketChannel.connect(otherAgentSocketAddr);
                //socket.connect(otherAgentSocketAddr);
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                System.out.println("connection working");
                String toPRINT=DID2DIDComm.askForDID2DIDCommunication(wallet,
                        socketChannel.socket(),"ILMIOFALSODID",
                        "LAMIAFALSAVERKEY",
                "AGENTECLIENT");

                System.out.println("SOMETHING TO PRINT"+toPRINT);

            } catch (IOException e) {
                e.printStackTrace();
            } catch (IndyException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }
}
