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
    private int expectedSeq;

    public ReceiverTransport(NetworkLayer nl){
        ra = new ReceiverApplication();
        this.nl=nl;
        initialize();
        corrupted = false;
        expectedAck = 0;
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
            if(pkt.isCorrupt()){
                System.out.println("CORRUPTED");
                resendTCP(pkt);                
            }            
            else if(pkt.getSeqnum() > expectedSeq){
                // if the next packet is greater than the next expected, it is out of order and a repeat ack is sent
                System.out.println("OUT OF ORDER " + expectedSeq + " " + pkt.getSeqnum());
                Message ack = new Message("Ack");
                Packet temp = new Packet(ack, expectedSeq, 0, 0);
                resendTCP(temp);   
            }
            else if (pkt.getSeqnum() < expectedSeq){
                // do nothing. This is a repeat packet and no response should be sent. Sender will timeout.
            }
            else if(pkt.getSeqnum() == expectedSeq){
                // correct packet received. Send back correct ack for packet.
                ra.receiveMessage(pkt.getMessage());
                Message ack = new Message("Ack");
                Packet ackPkt = new Packet(ack, pkt.getSeqnum(), 0, 0);
                nl.sendPacket(ackPkt, 0);
                expectedSeq = expectedSeq; // + MSS;                
            }
        }
        else{
            if (pkt.getAcknum() > expectedAck){
                System.out.println("OUT OF ORDER " + expectedAck + " " + pkt.getAcknum());
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

    public void resendTCP(Packet pkt){
        int seqNum = pkt.getSeqnum();
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
