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
    public void receiveMessage(Packet pkt)
    {
        if (pkt.getAcknum() > expectedAck)
        {
            if(usingTCP)
            {
            }
            else
            {
                System.out.println("OUT OF ORDER " + expectedAck + " " + pkt.getAcknum());
                Message ack = new Message("Ack");
                Packet temp = new Packet(ack, 0, expectedAck, 0);
                resendGBN(temp);
            }            
        }
        else if (pkt.getAcknum() < expectedAck)
        {
        }
        else if (pkt.isCorrupt())
        {
            if(usingTCP)
            {
            }
            else
            {
                System.out.println("CORRUPTED");
                resendGBN(pkt);
            }
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
    
    public void resendGBN(Packet pkt)
    {
        int ackNum = pkt.getAcknum();
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
