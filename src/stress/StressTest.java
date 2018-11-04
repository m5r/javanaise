package stress;

public class StressTest {
    private final static int clientsAmount = 100;
    private final static int objectsAmount = 10;

    public synchronized static void main(String args[]) {
        for (int clientId = 0; clientId < clientsAmount; clientId++) {
            Stresser stresser = new Stresser(clientId, objectsAmount);
            stresser.start();

            if (clientId == 0) {
                try {
                    Thread.sleep(5000);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }
    }
}
