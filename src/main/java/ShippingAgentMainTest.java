import java.util.Scanner;

public class ShippingAgentMainTest {
    public static void main(String[] args) {
        ShippingAgent shippingAgent = new
                ShippingAgent("INDYSCANPOOL", "DHL", "wall",
                "wall");
        shippingAgent.run();
    }
}
