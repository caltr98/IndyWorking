import org.hyperledger.indy.sdk.pool.Pool;
import org.json.JSONObject;

public class scammerShippingAgent extends  ShippingDeliverer{
    public scammerShippingAgent(String poolName, String shippingAgentName, String walletName, String walletpass) {
        super(poolName, shippingAgentName, walletName, walletpass);
    }

    public scammerShippingAgent(Pool pool, String shippingAgentName, String walletname, String walletpassword) {
        super(pool, shippingAgentName, walletname, walletpassword);
    }
    @Override
    protected String handleMessage(String message){
        System.out.println("RECEIVED MES"+ message);
        JSONObject messageJOBJECT= new JSONObject(message);
        if(messageJOBJECT.get("request").equals("GetShippingAgentID")) {
            byte[] msg = IndyShippingDeliverer.writeMessage(this.shippingAgentName,
                    addresstodid.get(currentTalker));
            sendMSG(msg, currentTalker);
            return "Success giving ShippingAgentID";
        }

        if(messageJOBJECT.get("request").equals("HandleDelivery0")){
            //add this delivery in the list of delivery to handle
            DeliveryObject deliveryObject =new DeliveryObject(
                    messageJOBJECT.getString("itemCredentialOffer"));
            this.requestedDeliveries.put(deliveryObject.deliveryIdentifier,deliveryObject);
            byte [] msg= IndyShippingDeliverer.writeMessage(new JSONObject().put
                    ("deliveryId",deliveryObject.deliveryIdentifier).toString(),addresstodid.get(currentTalker));
            sendMSG(msg, currentTalker);
            msg=deliveryObject.DeliveryCredRequest();
            sendMSG(msg, currentTalker);
            return "Success";
        }
        if(messageJOBJECT.get("request").equals("HandleDelivery1")){
            System.out.println("messageOBJECT is"+messageJOBJECT.toString(4));
            String credential,boxEndpointAddr;
            int boxEndpointPort;
            deliveryIdentifier = messageJOBJECT.getLong("deliveryId");
            if(this.requestedDeliveries.containsKey(deliveryIdentifier)) {
                credential = messageJOBJECT.getString("Credential");
                //String boxEndpointAddr = messageJOBJECT.get("LockerBoxAgentIP"); for non-localhost use
                boxEndpointAddr = "127.0.0.1";// for localhost test
                boxEndpointPort = messageJOBJECT.getInt("LockerBoxAgentPortNo");
                System.out.println("box port number"+ boxEndpointPort);
                DeliveryObject currdeliveryObject=requestedDeliveries.remove(deliveryIdentifier);
                if(messageJOBJECT.has("message")){
                    JSONObject deliveryMessage;
                    deliveryMessage=new JSONObject(messageJOBJECT.getString("message"));
                    if(deliveryMessage.has("credential")) {
                        //retrieve credential, deliver item and collect them as a customer.
                        String customercredential =deliveryMessage.getString("credential");
                        if(currdeliveryObject.DeliveryCred(credential,boxEndpointAddr,boxEndpointPort).equals("Success")) {
                            currdeliveryObject.deliverItem();//deliver an item
                            //store credential in wallet

                            //create a MasterSecret with the same Id of the blinded one.
                            System.out.println("mastersecret inside the metadata:"+ new JSONObject(deliveryMessage.getString("credreqmetadata")).getString("master_secret_name"));
                            this.IndyShippingDeliverer.createMasterSecret(new JSONObject(deliveryMessage.getString("credreqmetadata")).getString("master_secret_name"));
                            this.IndyShippingDeliverer.storeCredentialInWallet(null,deliveryMessage.getString("credeDefId"),

                                    deliveryMessage.getString("credreqmetadata"),customercredential,
                                    IndyShippingDeliverer.getCredentialDefinitionFromLedger(deliveryMessage.getString("credeDefId")),
                                    null);
                            //try to get the item from box, like a customer would
                            Customer2Box handleStealCUstomer=new Customer2Box(this.IndyShippingDeliverer,boxEndpointAddr,boxEndpointPort,
                                    "agent");
                            handleStealCUstomer.askForConnectionToBox();
                            handleStealCUstomer.getItemFromBOX();
                            System.out.println("Successfully stealed item");
                        }

                    }
                }
                else {
                    if (currdeliveryObject.DeliveryCred(credential, boxEndpointAddr, boxEndpointPort).equals("Success")) {
                        System.out.println("delivery is beeing processed");
                        deliveries.add(currdeliveryObject);
                        return "Success";
                    }
                }
                return "Fail";
            }
        }
        return "Wrong Message";
    }

}
