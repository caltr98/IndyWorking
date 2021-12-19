package IndyLibraries;

import org.hyperledger.indy.sdk.IndyException;
import org.hyperledger.indy.sdk.crypto.Crypto;
import org.hyperledger.indy.sdk.did.Did;
import org.hyperledger.indy.sdk.pairwise.Pairwise;
import org.hyperledger.indy.sdk.wallet.Wallet;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

public class DID2DIDComm {



    public static boolean sendDID2DIDMSG(Wallet senderWallet, String receiver2SenderDID,
                                          String msg, Socket connectionSocket) throws IndyException, ExecutionException, InterruptedException, IOException {
        //grazie a pairwise
        String sender2receiver =
                Pairwise.getPairwise(senderWallet, receiver2SenderDID).get();
        String sender2receiverVerkey =
                Did.keyForLocalDid(senderWallet,
                        receiver2SenderDID).get();

        String senderDID2receiverVerKeyArray =
                new JSONArray(sender2receiverVerkey).toString();

        String receiver2senderVerkey =
                Did.keyForLocalDid(senderWallet, receiver2SenderDID).get();

        String receiver2senderVerkeyArray =
                new JSONArray(receiver2senderVerkey).toString();


        byte[] packedData = Crypto.packMessage(senderWallet,
                receiver2senderVerkeyArray,
                senderDID2receiverVerKeyArray
                , msg.getBytes()).get();

        sendMSG(packedData, connectionSocket);

        return true;
    }

    public static void sendMSG(byte[] msg, Socket connectionSocket) throws IOException {
        connectionSocket.getChannel().configureBlocking(true);
        OutputStream outputStream = connectionSocket.getOutputStream();
        outputStream.write(msg);
        outputStream.flush();
        connectionSocket.getChannel().configureBlocking(false);

    }
    public static byte[] receiveMSG(Socket connectionSocket) throws IOException {
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
        ArrayList<Byte> growingBuffer = new ArrayList<Byte>();
        SocketChannel channel = connectionSocket.getChannel();
        channel.configureBlocking(true);//wait for data
        byteBuffer.clear();
        int i;
        System.out.println("prewait");
        while (channel.read(byteBuffer) > 0) {//Lettura della richiesta dal Client
            System.out.println("INSIDE METHOD");
            channel.configureBlocking(false);//stop when the channel is empty

            byteBuffer.flip();
            while (byteBuffer.hasRemaining()) {
                growingBuffer.add(byteBuffer.get());//lettura se byteBuffer.dimension()< (dati in arrivo)
            }
            byteBuffer.clear();
        }
        byteBuffer.flip();
        while (byteBuffer.hasRemaining()) {
            growingBuffer.add(byteBuffer.get());//ultima lettura
        }
        byteBuffer.clear();//il byteBuffer viene ripulito per operazioni successive
        if (growingBuffer.size() == 0) {//TCP connection was closed in the other side
            System.out.print("TCP connection with agent Fail: ");
            channel.close();
        }
        System.out.println("post wait");

        byte[] data=new byte[growingBuffer.size()];
        for (i = 0; i < growingBuffer.size(); i++) {
            data[i] = growingBuffer.get(i);
        }
        return data;
    }

    public static byte[] getResponse(Socket receivingSocket) throws IOException {
        InputStream inputStream = receivingSocket.getInputStream();
        byte[] receiverMessage = inputStream.readAllBytes();
        return receiverMessage;
    }

    public static String receiveDID2DIDMSG(Wallet receiverWallet, Socket receivingSocket) throws
            IOException, IndyException, ExecutionException,
            InterruptedException {
        byte[] receivedMessage = getResponse(receivingSocket);
        JSONObject unpackedJSON = new JSONObject(new String(Crypto.unpackMessage(receiverWallet, receivedMessage).get()));
        String unpacked = (String) unpackedJSON.get("message");

        return unpacked;
    }
    public static String readDID2DIDMSG(Wallet receiverWallet,byte[] receivedMessage) throws
            IOException, IndyException, ExecutionException,
            InterruptedException {
        System.out.println("readDID2DIDMSG");
        JSONObject unpackedJSON = new JSONObject(new String(Crypto.unpackMessage(receiverWallet, receivedMessage).get()));
        String unpacked = (String) unpackedJSON.get("message");
        System.out.println("unpacked arrived message"+ unpacked);
        return unpacked;
    }
    public static byte[] writeDID2DIDMSG(Wallet senderWallet,String recipientDID,String message) throws
            IOException, IndyException, ExecutionException,
            InterruptedException {
        System.out.println("receipent did for whome i ask the verkey"+ recipientDID);
        String theirVerKeyInMessageDid=Did.keyForLocalDid(senderWallet,
                recipientDID).get() ;
        String myPairInPairwise = new JSONObject(Pairwise.getPairwise(senderWallet,recipientDID).get()).
                getString("my_did");
        String myVerKeyInMessageDid = Did.keyForLocalDid(senderWallet,myPairInPairwise).get();
        /*System.out.println("theirVerKeyInMessageDid"+ theirVerKeyInMessageDid+" myPairInPairwise"+myPairInPairwise
        +"myVerKeyInMessageDid: "+myVerKeyInMessageDid);*/
        byte[] packedData=Crypto.packMessage(
                senderWallet,
                new JSONArray(new String[]{theirVerKeyInMessageDid}).toString(),
                myVerKeyInMessageDid,
                message.getBytes(StandardCharsets.UTF_8)).get();


        return packedData;
    }

    /*
      Given an Handshake Message and a pair of (meToThemDID,meToThemVerKey) created by the current agent,
      the Handshake message contains the DID of the communicating agent and their verkey.
      Did.storeTheirDid is an IndySDK library call and stores the did of the communicating agent and their verkey
      Pairwise.createPairwise creates a PairWise in the Wallet,PairWise will be then used to find the
      current Agent DID corresponding to the remote Agent when writing a message,
      theirDID and theirVerKey will be used when reading the remote agent message
     */
    public static String createDIDtoDID(Wallet wallet,
                                         String handshakeMSG,String meToThemDID, String meToThemVerKey,
                                         String agentName) throws IOException, IndyException, ExecutionException, InterruptedException {
                System.out.println("Inside method for handshake2");

        String theirName, theirDID, theirVerKey;
        theirName = theirDID = theirVerKey = null;
        String[] MsgFields = handshakeMSG.split("-\r");
        System.out.println(MsgFields.length + handshakeMSG);
        System.out.println(MsgFields.length + " " + MsgFields[0] +" "+ MsgFields[3]);
        if (handshakeMSG != null) {
            //String[] MsgFields = handshakeMSG.split("-\r");
            if (MsgFields != null && MsgFields.length==7) {
                if (MsgFields[0].equals("HANDSHAKE1")) {
                    if (MsgFields[1].equals("IndyLibraries.Agent:") && MsgFields[3].equals("DID:") && MsgFields[5].equals("Verkey:")) {
                        theirName = MsgFields[2];
                        theirDID = MsgFields[4];
                        theirVerKey = MsgFields[6];
                    }
                    else return "wrong fields";
                } else
                    return "Illegal Response format";
            } else
                return "Illegal Response format";
        }
        System.out.println("theirDID"+theirDID);
        System.out.println("theirVerKey"+theirVerKey);
        Did.storeTheirDid(wallet, new JSONObject().put("did", theirDID).
                put("verkey", theirVerKey).toString() ).get();
        Pairwise.createPairwise(wallet, theirDID, meToThemDID, new JSONObject().put("AgentName", theirName).toString()).get();

        String message = "HANDSHAKE2-\rIndyLibraries.Agent:-\r" + agentName + "-\rDID:-\r" + meToThemDID + "-\rVerkey:-\r" + meToThemVerKey;
        //returns theirDID and Message to Send for completing did2did setup
        String toreturn=message;
        //return their did and message to send

        return new JSONObject().put("theirDID",theirDID).put("message",message).toString();
    }
    public static String readMessage(Socket sockConnection) throws IOException {
        int i;
        ByteBuffer byteBuffer;//ByteBuffer with memory non direct allocated for reading
        ArrayList<Byte> growingBuffer = new ArrayList<>();//used for having growing storage in the reading
        byteBuffer=ByteBuffer.allocate(1024);
        SocketChannel channel = sockConnection.getChannel();
        channel.configureBlocking(true);
        byteBuffer.clear();
        System.out.println("pre read");
        while (channel.read(byteBuffer)>0){//reading the message from the sender
            channel.configureBlocking(false);
            byteBuffer.flip();
            System.out.println("in read");
            while (byteBuffer.hasRemaining()) {
                growingBuffer.add(byteBuffer.get());//read only if  byteBuffer.dimension()< (size of incoming data)
            }
            byteBuffer.clear();
        }
        byteBuffer.flip();
        System.out.println("post read");

        while (byteBuffer.hasRemaining()) {
            growingBuffer.add(byteBuffer.get());//last read operation
        }
        byteBuffer.clear();//il byteBuffer viene ripulito per operazioni successive

        if(growingBuffer.size()==0){//if TCP connection fails then bytebuffer read 0 bytes
            //TCP connection fail case
            System.out.print("TCP connection with Failed");

            channel.close();//channel closure
        }

        byte[] tmpTOString = new byte[growingBuffer.size()];//Bytes are trasformed from ArrayList<Byte> to byte[]
        for(i=0;i<growingBuffer.size();i++){
            tmpTOString[i]=growingBuffer.get(i);
        }
        return new String(tmpTOString, Charset.defaultCharset());// byte to string and return it
    }
    public static String askForDID2DIDCommunication(Wallet wallet, Socket connectionSocket,
                                                     String meToThemDID, String meToThemVerKey, String agentName) throws IOException, IndyException, ExecutionException, InterruptedException {

        System.out.println("Inside method for handshake1");
        OutputStream stringWriterToAgent= connectionSocket.getOutputStream();
        InputStream stringReaderFromAgent= (connectionSocket.getInputStream());
        String theirName, theirDID, theirVerKey;
        theirName = theirDID = theirVerKey = null;
        String message = "HANDSHAKE1-\rIndyLibraries.Agent:-\r" + agentName + "-\rDID:-\r" + meToThemDID + "-\rVerkey:-\r" + meToThemVerKey;
        stringWriterToAgent.write(message.getBytes(Charset.defaultCharset()));
        stringWriterToAgent.flush();
        System.out.println("message sended");
        //closes output and returns EOF to the receiver
        String receivedMessage = readMessage(connectionSocket);
        System.out.println("received message"+ receivedMessage);
        if (receivedMessage != null) {
            String[] MsgFields = receivedMessage.split("-\r");
            if (MsgFields != null) {
                if (MsgFields[0].equals("HANDSHAKE2")) {
                    if (MsgFields[1].equals("IndyLibraries.Agent:") && MsgFields[3].equals("DID:") && MsgFields[5].equals("Verkey:")) {
                        theirName = MsgFields[2];
                        theirDID = MsgFields[4];
                        theirVerKey = MsgFields[6];
                    }
                    else return "Wrong Fields";
                } else
                    return "Illegal Response format";
            } else
                return "Illegal Response format";
        }
        System.out.println("theirDID"+theirDID);
        System.out.println("theirVerKey"+theirVerKey);
        Did.storeTheirDid(wallet, new JSONObject().put("did", theirDID).
                put("verkey", theirVerKey).toString()).get();
        Pairwise.createPairwise(wallet, theirDID, meToThemDID, new JSONObject().put("AgentName", theirName).toString()).get();
        return theirDID;//returns their did for further references
    }
    //setup of DID2DID comunicatiotion send of Handshake message with myDID and myVerKey created for comunication with remote Agent
    public static byte[] setupDID2DIDCommunicationAsk(String meToThemDID, String meToThemVerKey, String agentName) throws IOException, IndyException, ExecutionException, InterruptedException {

        String message = "HANDSHAKE1-\rIndyLibraries.Agent:-\r" + agentName + "-\rDID:-\r" + meToThemDID + "-\rVerkey:-\r" + meToThemVerKey;
        System.out.println(message + "mex to send");
        return message.getBytes(Charset.defaultCharset());
    }
    /*setup of DID2DID communication send of myDID and myVerKey created for communication with remote Agent
       if the setup on the remote agent was successful then the current agents receives theirDID and theirVerKey
      and calls:
      Did.storeTheirDid is an IndySDK library call and stores the did of the communicating agent and their verkey
      Pairwise.createPairwise creates a PairWise in the Wallet.
      PairWise will be then used to find the
      current Agent DID that correspond to the remote Agent when writing a message,
      theirDID and theirVerKey will be then used when reading the remote agent message
     */
    public static String setupDID2DIDCommunicationResponse(Wallet wallet,String meToThemDID,byte[] receivedmessage) throws IndyException, ExecutionException, InterruptedException {
        String receivedMessage = new String(receivedmessage,Charset.defaultCharset());
        String theirName, theirDID, theirVerKey;
        theirName = theirDID = theirVerKey = null;
        System.out.println("did to did response");
        System.out.println(receivedMessage + receivedMessage.length());
        if (receivedMessage != null) {
            String[] MsgFields = receivedMessage.split("-\r");
            if (MsgFields != null) {
                if (MsgFields[0].equals("HANDSHAKE2")) {
                    if (MsgFields[1].equals("IndyLibraries.Agent:") && MsgFields[3].equals("DID:") && MsgFields[5].equals("Verkey:")) {
                        theirName = MsgFields[2];
                        theirDID = MsgFields[4];
                        theirVerKey = MsgFields[6];
                    }
                    else return "Wrong Fields";
                } else
                    return "Illegal Response format";
            } else
                return "Illegal Response format";
        }
        System.out.println("theirDID"+theirDID);
        System.out.println("theirVerKey"+theirVerKey);
        Did.storeTheirDid(wallet, new JSONObject().put("did", theirDID).
                put("verkey", theirVerKey).toString()).get();
        Pairwise.createPairwise(wallet, theirDID, meToThemDID, new JSONObject().put("AgentName", theirName).toString()).get();
        return theirDID;//returns their did for further references
    }


}
