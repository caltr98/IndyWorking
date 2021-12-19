package IndyLibraries;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
//Backup functionalities
public class JSONUserCredentialStorage {
    Cipher cipher;//per criptare la password da conservare
    SecretKey secretKey;
    File userCredentialFile;
    JSONObject jsonObjCred = new JSONObject();
    byte[] key;

    public JSONUserCredentialStorage(File userCredentialFile) throws NoSuchPaddingException, NoSuchAlgorithmException, IOException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        cipher = Cipher.getInstance("AES");
        this.userCredentialFile = userCredentialFile;
        keyGenerator.init(256);
        if (!userCredentialFile.exists())
            userCredentialFile.createNewFile();
        else {
            try {
                jsonObjCred = FileGetJSON(userCredentialFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (jsonObjCred != null && jsonObjCred.has("SecKey")) {
            List<Object> lis=jsonObjCred.getJSONArray("SecKey").toList();
            byte[] arr = new byte[lis.size()];
            int i=0;
            for (Object o :lis
                 ) {
                arr[i]=o.toString().getBytes()[0];
                System.out.print(o.toString());
            }
            System.out.print("fine");
            this.key=arr;
            this.secretKey = new SecretKeySpec(arr, 0, this.key.length, "AES");
        } else {
            secretKey = keyGenerator.generateKey();
            key = secretKey.getEncoded();
            jsonObjCred.put("SecKey", key);
        }
    }


    private JSONObject FileGetJSON(File userCredentialFile) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(
                new FileInputStream(userCredentialFile)));
        JSONObject jsObject;
        int lineread=0;
        String line;
        StringBuilder resultStringBuilder = new StringBuilder();
        while((line=bufferedReader.readLine())!=null) {
            lineread++;
            resultStringBuilder.append(line).append("\n");
        }
        if(lineread>0) {
             jsObject = new JSONObject(resultStringBuilder.toString());
        }
        else jsObject=new JSONObject();
        return jsObject;

    }

    public JSONUserCredentialStorage(String keyString) throws NoSuchAlgorithmException, NoSuchPaddingException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        cipher = Cipher.getInstance("AES");
        keyGenerator.init(256);
        this.key = keyString.getBytes();
        this.secretKey = new SecretKeySpec(key, 0, this.key.length, "AES");
    }

    private String getKey(String encryptedKey) throws InvalidKeyException, BadPaddingException, IllegalBlockSizeException, InvalidKeyException {
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        String originalPas = new String(cipher.doFinal(), Charset.defaultCharset());
        return originalPas;
    }

    private String getDecryptedKey(String encryptedKey) throws InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        cipher.init(Cipher.DECRYPT_MODE,secretKey);
        String originalKey=new String(cipher.doFinal(encryptedKey.getBytes()), Charset.defaultCharset());
        return originalKey;
    }

    public String getMasterSecret(String agentName){
        //A wallet should store all credentials with just one master secret for all of theme
        JSONObject jAgent;
        String ris;
        synchronized (this) {
            if (jsonObjCred.has(agentName)) {
                if((jAgent = jsonObjCred.optJSONObject(agentName))!=null)
                    if ((ris = jAgent.optString("MasterSecret")) != null) {
                        return ris;
                    }
                else return "";
            }
            else return null;
        }
        return null;
    }
    public String insertMasterSecret(String agentName,String masterSecret){
        //A wallet should store all credentials with just one master secret for all of theme
        JSONObject jAgent;
        String ris;
        synchronized (this) {
            if (jsonObjCred.has(agentName)) {
                jsonObjCred.put(agentName, jsonObjCred.getJSONObject(agentName).put("MasterSecret",masterSecret));
                return "Success";
            }
            else return null;
        }
    }

    public void insertAgentName(String agentName){
        synchronized (this){
            jsonObjCred.put("agentName",agentName);
        }
    }
    public void insertAgentDID(String agentName,String agentDID){
        JSONObject tempJSONAgentObj;
        //chiamata solo dopo aver controllato che l'agent non ha un DID salvato nel JSON
        synchronized (this){
            if( (jsonObjCred.has(agentName))) {
                if((tempJSONAgentObj=jsonObjCred.optJSONObject(agentName))!=null) {
                    if (!tempJSONAgentObj.has("DID")) {

                        tempJSONAgentObj.put("DID", agentDID);
                        //add updated object
                        tempJSONAgentObj.put(agentName, tempJSONAgentObj);
                        jsonObjCred.put(agentName,tempJSONAgentObj);
                    }
                }
            }
        }
    }
    public void insertAgentVerKey(String agentName,String agentVerKey) {
        JSONObject tempJSONAgentObj;
        String verKeyCrypted;
        byte []keybytes=null;
        try {
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            keybytes= cipher.doFinal(agentVerKey.getBytes());
        } catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();
        }
        synchronized (this) {
            if (jsonObjCred.has(agentName)) {
                if((tempJSONAgentObj = jsonObjCred.optJSONObject(agentName))!=null) {
                    if (!tempJSONAgentObj.has("VerKey")) {
                        tempJSONAgentObj.put("VerKey", keybytes.toString());
                        //add updated object
                        jsonObjCred.put(agentName,tempJSONAgentObj);

                    }
                }
            }
        }
    }
    public String getAgentDID(String agentName) {
        JSONObject tempJSONAgentObj;
        synchronized (this) {
            if ((tempJSONAgentObj = jsonObjCred.optJSONObject(agentName)) != null) {
                if(tempJSONAgentObj.has("DID"))
                    return tempJSONAgentObj.optString("DID");
            }
        }
        return null;
    }
    public String getAgentVerKey(String agentName){
        JSONObject tempJSONAgentObj;
        String toRet=null;
        synchronized (this){

            if( (tempJSONAgentObj=jsonObjCred.optJSONObject(agentName))!=null) {
                if(tempJSONAgentObj.has("VerKey")) {
                    toRet = tempJSONAgentObj.optString("VerKey");
                }
            }

        }
        if(toRet!=null) {
            try {
                toRet=getDecryptedKey(toRet);
                return toRet;
            } catch (InvalidKeyException e) {
                e.printStackTrace();
            } catch (BadPaddingException e) {
                e.printStackTrace();
            } catch (IllegalBlockSizeException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
    public void makeBackup() throws IOException {
        FileWriter fileWriter= new FileWriter(userCredentialFile);
        fileWriter.write(jsonObjCred.toString(4));
        fileWriter.flush();
        fileWriter.close();
    }

}
