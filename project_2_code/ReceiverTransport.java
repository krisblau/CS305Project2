import java.util.ArrayList;
/**
 * A class which represents the receiver transport layer
 */
public class ReceiverTransport
{
    private ReceiverApplication ra;
    private NetworkLayer nl;
    private boolean usingTCP;
    private boolean corrupted;
    private int expectedAck; 
    private int expectedSeq; // Next packet expected in order.
    private ArrayList<Packet> buffered; // buffered packets waiting to be delivered in order

    public ReceiverTransport(NetworkLayer nl){
        ra = new ReceiverApplication();
        this.nl=nl;
        initialize();
        expectedSeq = 1;
        expectedAck = 0;
        corrupted = false;
        expectedSeq = 0;
        buffered = new ArrayList<Packet>();
    }

    /**This routine will be called once, before any of your other receiver routines are called. It can be used to do any required initialization
     */
    public void initialize()
    {
    }

    /**
     * This routine will be called whenever a packet sent from the sender arrives at the receiver. 
     * Packet is the (possibly corrupted) packet sent from the sender.
     */
    public void receiveMessage(Packet pkt){
        // Determine action base on is TCP or GBn is being used.
        if(usingTCP){
            // If the packet is corrupted send an ack for last packet received successfully.
            if(pkt.isCorrupt()){
                System.out.println("CORRUPTED");
                resendTCP(pkt);                
            }       
            // If the sequence number of the packet is greater than expected, it must be buffered until it is the next expected ack. 
            else if(pkt.getAcknum() > expectedAck){
                // If the next packet is greater than the next expected, it is out of order and is buffered. An ack for the next expected packet is sent back.
                System.out.println("OUT OF ORDER... expected PKT: " + expectedAck + " actual PKT: " + pkt.getAcknum());
                buffered.add(pkt);
                Message ack = new Message("Ack");
                Packet temp = new Packet(ack, 0, expectedAck, 0);
                resendTCP(temp);   
            }
            // Determine if the packet received has already been received before. 
            else if (pkt.getAcknum() < expectedAck){
                // do nothing. This is a repeat packet and no response should be sent. Sender will timeout.
                // System.out.println("Packet has been delivered already: " + pkt.getAcknum());
            }
            // Finally, if the correct packet is received, send back acks saying it has arrived successfully.
            else if(pkt.getAcknum() == expectedAck){
                // Message is successfully received.
                ra.receiveMessage(pkt.getMessage());
                // Send the confirming ack back to sender.
                Message ack = new Message("Ack");
                Packet ackPkt = new Packet(ack, 0, pkt.getAcknum(), 0);
                nl.sendPacket(ackPkt, 0);
                // increase the value of the next expected seq.  
                expectedAck++;
                // Check if the next expected message is already beffered. If it is, take it out of the buffer.
                for(int i = 0; i < buffered.size(); i++){
                    if(buffered.get(i).getAcknum() == expectedAck){
                        System.out.println("Packet taken from buffer: " + expectedAck);
                        buffered.remove(i);
                        expectedAck++;
                    }
                }            
            }
        }
        else{
            if (pkt.getAcknum() > expectedAck){
                System.out.println("OUT OF ORDER...expected ACK: " + expectedAck + " actual ACK: " + pkt.getAcknum());
                Message ack = new Message("Ack");
                Packet temp = new Packet(ack, 0, expectedAck, 0);
                resendGBN(temp);
            }
            else if (pkt.getAcknum() < expectedAck)
            {
                // do nothing. This is a repeat packet and no response should be sent. Sender will timeout.
            }
            else if (pkt.isCorrupt())
            {
                System.out.println("CORRUPTED");
                resendGBN(pkt);
            }
            else if (pkt.getAcknum() == expectedAck)
            {
                ra.receiveMessage(pkt.getMessage());
                Message ack = new Message("Ack");
                Packet ackPkt = new Packet(ack, 0, pkt.getAcknum(), 0);
                nl.sendPacket(ackPkt, 0);
                expectedAck++;
            }
        }
    }

    public void resendGBN(Packet pkt)
    {
        int ackNum = pkt.getAcknum();
        Message ack = new Message("Ack");
        Packet ackPkt = new Packet(ack, 0, ackNum, 0);
        nl.sendPacket(ackPkt, 0);
    }

    /**
     * This method sends acks for next expected packet after corrupted packets are received.
     */
    public void resendTCP(Packet pkt){
        // a new ack packet is made based off of next packet expected.
        int ackNum = expectedAck - 1; // pkt.getAcknum();
        Message ack = new Message("Ack");
        Packet ackPkt = new Packet(ack, 0, ackNum, 0);
        nl.sendPacket(ackPkt, 0);
    }    

    public void setProtocol(int n)
    {
        if(n>0)
            usingTCP=true;
        else
            usingTCP=false;
    }
}
