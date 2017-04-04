/**
 * A class which represents the receiver transport layer
 */
public class ReceiverTransport
{
    private ReceiverApplication ra;
    private NetworkLayer nl;
    private boolean usingTCP;

    public ReceiverTransport(NetworkLayer nl){
        ra = new ReceiverApplication();
        this.nl=nl;
        initialize();
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
        if (pkt.isCorrupt())
        {
            if(usingTCP)
            {
            }
            else
            {
                resendGBN(pkt);
            }
        }
        else
        {
            ra.receiveMessage(pkt.getMessage());
        }
    }
    
    public void resendGBN(Packet pkt)
    {
        int ackNum = pkt.getAcknum();
        Message ack = new Message("Ack");
        Packet ackPkt = new Packet(ack, ackNum, 0, 0);
        nl.sendPacket(ackPkt, 9999);
    }

    public void setProtocol(int n)
    {
        if(n>0)
            usingTCP=true;
        else
            usingTCP=false;
    }
}
