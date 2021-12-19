import org.hyperledger.indy.sdk.IndyException;
import org.json.JSONObject;

import java.util.Scanner;
import java.util.concurrent.ExecutionException;

public class LockerboxMainTest {
    public static void main(String[] args) {
        LockerBoxAgent lockerBoxAgent = new LockerBoxAgent("INDYSCANPOOL","pool");
        //LockerBoxAgent lockerBoxAgent = new LockerBoxAgent("INDYSCANPOOL","pool",
         //       "TQqc731MTDrFfu9QaE5k5v",null,null
        //);
        lockerBoxAgent.run();
    }
}
