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
        corrupted = false;
        expectedAck = 0;
        expectedSeq = 0;
        buffered = new ArrayList<Packet>();
    }

    /**This routine will be called once, before any of your other receiver routines are called. It can be used to do any required initialization
     */
    public void initialize()
    {
    }

    /**This routine will be called whenever a packet sent from the sender arrives at the receiver. 
     *packet is the (possibly corrupted) packet sent from the sender
     */
    public void receiveMessage(Packet pkt){
        // 
        if(usingTCP){
            // If the packet is corrupted send an ack for last packet received successfully.
            if(pkt.isCorrupt()){
                System.out.println("CORRUPTED");
                resendTCP(pkt);                
            }       
            // If the sequence number of the packet is greater than expected, it must be buffered until it is the next expected ack. 
            else if(pkt.getSeqnum() > expectedSeq){
                // If the next packet is greater than the next expected, it is out of order and is buffered. An ack for the next expected packet is sent back.
                System.out.println("OUT OF ORDER... expected PKT: " + expectedSeq + " actual PKT: " + pkt.getSeqnum());
                buffered.add(pkt);
                Message ack = new Message("Ack");
                Packet temp = new Packet(ack, expectedSeq, 0, 0);
                resendTCP(temp);   
            }
            // Determine if the packet received has already been received before. 
            else if (pkt.getSeqnum() < expectedSeq){
                Message ack = new Message("Ack");
                Packet temp = new Packet(ack, expectedSeq, 0, 0);
                resendTCP(temp);
            }
            // Finally, if the correct packet is received, send back acks saying it has arrived successfully.
            else if(pkt.getSeqnum() == expectedSeq){
                // Message is successfully received.
                ra.receiveMessage(pkt.getMessage());
                // Send the confirming ack back to sender.
                Message ack = new Message("Ack");
                Packet ackPkt = new Packet(ack, pkt.getSeqnum(), 0, 0);
                nl.sendPacket(ackPkt, 0);
                // increase the value of the next expected seq.  
                expectedSeq++;
                // Check if the next expected message is already buffered. If it is, take it out of the buffer.
                for(int i = 0; i < buffered.size(); i++){
                    if(buffered.get(i).getSeqnum() == expectedSeq){
                        ra.receiveMessage(buffered.get(i).getMessage());
                        buffered.remove(i);
                        ackPkt = new Packet(ack, expectedSeq, 0, 0);                        
                        nl.sendPacket(ackPkt, 0);
                        expectedSeq++;
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
                Message ack = new Message("Ack");
                Packet temp = new Packet(ack, 0, expectedAck, 0);
                resendGBN(temp);
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

    /**
     * Function to ask the sender to resend a given packet for Go-Back-N.
     * 
     * @param pkt Packet object of the packet being requested.
     */    
    public void resendGBN(Packet pkt)
    {
        int ackNum = pkt.getAcknum();
        Message ack = new Message("Ack");
        Packet ackPkt = new Packet(ack, 0, ackNum, 0);
        nl.sendPacket(ackPkt, 0);
    }

    /**
     * Function to ask the sender to resend a given packet for TCP.
     * 
     * @param pkt Packet object of the packet being requested.
     */  
    public void resendTCP(Packet pkt){
        // a new ack packet is made based off of next packet expected.
        int seqNum = expectedSeq; // pkt.getSeqnum();
        Message ack = new Message("Ack");
        Packet ackPkt = new Packet(ack, seqNum, 0, 0);
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
