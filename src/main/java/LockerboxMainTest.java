import java.util.Scanner;

public class LockerboxMainTest {
    public static void main(String[] args) {
        System.out.println("Insert a Steward DID");
        Scanner sc = new Scanner(System.in);
        String stewardDID=sc.next();
        //LockerBoxAgent lockerBoxAgent = new LockerBoxAgent("INDYSCANPOOL","pool",stewardDID);
        LockerBoxAgent lockerBoxAgent = new LockerBoxAgent("INDYSCANPOOL","pool",
                "TQqc731MTDrFfu9QaE5k5v",null,null
        );
        lockerBoxAgent.run();
    }
}
