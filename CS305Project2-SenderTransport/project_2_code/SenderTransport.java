
import java.util.ArrayList;
/**
 * A class which represents the receiver transport layer
 */
public class SenderTransport
{
    private NetworkLayer nl;
    private Timeline tl;
    private int n;
    private int seqIndex;
    private int ackIndex;
    private boolean usingTCP;

    public SenderTransport(NetworkLayer nl){
        this.nl=nl;
        initialize();
        seqIndex = 0;
        ackIndex = 0;
    }

    public void initialize()
    {
    }

    public void sendMessage(Message msg)
    {
        if (!usingTCP)
        {
            goBackN(msg);
        }
        else
        {
            tcp(msg);
        }
    }

    public void receiveMessage(Packet pkt)
    {
    }

    public void timerExpired()
    { 
    }

    public void setTimeLine(Timeline tl)
    {
        this.tl=tl;
    }

    public void setWindowSize(int n)
    {
        this.n=n;
    }

    public void setProtocol(int n)
    {
        if(n>0)
            usingTCP=true;
        else
            usingTCP=false;
    }

    public void goBackN(Message msg)
    {
        Packet pkt = new Packet(msg, seqIndex++, ackIndex++, 0);
        nl.sendPacket(pkt, 9999);
    }
    
    public void tcp(Message msg)
    {
    }
}
