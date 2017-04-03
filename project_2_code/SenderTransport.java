
import java.util.ArrayList;
/**
 * A class which represents the receiver transport layer
 */
public class SenderTransport
{
    private NetworkLayer nl;
    private Timeline tl;
    private int n;
    private int seq;
    private int ack;
    private int lastAck;
    private boolean usingTCP;
    private ArrayList<Packet> sentPkts;

    public SenderTransport(NetworkLayer nl){
        this.nl=nl;
        initialize();
        seq = 0;
        ack = 0;
        lastAck = 0;
        sentPkts = new ArrayList<Packet>();
    }

    public void initialize()
    {
    }

    public void sendMessage(Message msg)
    {
        if (usingTCP)
            tcp(msg);
        else
            gbn(msg);
    }

    public void receiveMessage(Packet pkt)
    {
        if (usingTCP)
            tcpReceive(pkt);
        else
            gbnReceive(pkt);
    }

    public void timerExpired()
    { 
        if (usingTCP)
            tcpResend();
        else
            gbnResend();
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

    public void gbn(Message msg)
    {
        Packet pkt = new Packet(msg, seq++, ack++, 0);
        sentPkts.add(pkt);
        nl.sendPacket(pkt, 9999);
    }
    
    public void gbnReceive(Packet pkt)
    {
        int receivedAck = pkt.getAcknum();
        if (receivedAck > lastAck)
        {
            
            lastAck = receivedAck;
        }
    }
    
    public void gbnResend()
    {
    }
    
    public void tcp(Message msg)
    {
    }
    
    public void tcpReceive(Packet pkt)
    {
    }
    
    public void tcpResend()
    {
    }
}
