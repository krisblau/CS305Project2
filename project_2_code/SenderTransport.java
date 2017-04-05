import java.util.ArrayList;
/**
 * A class which represents the receiver transport layer
 */
public class SenderTransport
{
    private NetworkLayer nl;
    private Timeline tl;
    private int seq;  // sequence number of next byte expected
    private int ack;  // what is this. How is it different from lastAck?
    private int lastAck;  // ack number of last packet received in order
    private int windowSize; // window size must be implemented somehow for GBN
    private int MSS; // a maximum segment size must be added. Possibly put in NetworkSimulator as well
    private boolean usingTCP;
    private ArrayList<String> sentMessages;
    private ArrayList<Message> queued;

    public SenderTransport(NetworkLayer nl){
        this.nl=nl;
        initialize();
        seq = 0;
        ack = 0;
        lastAck = -1;
        sentMessages = new ArrayList<String>();
        queued = new ArrayList<Message>();
    }

    /**
     * This routine will be called once, before any of your other sender routines are called. It can be used to do any required initialization
     */
    public void initialize()
    {
        System.out.print("\f"); //Clear output window on start.
    }

    /**
     * where message contains data to be sent to the B-side. This routine will be called whenever the upper layer at the sending side has a message to send. 
     * It is the job of your protocol to insure that the data in such a message is delivered in-order, and correctly, to the receiving side upper layer
     */
    public void sendMessage(Message msg){
        /**
         * send out a message using either TCP or GBN. Must keep track of last acked message.
         */
        if (usingTCP){
            // as many packets as windowSize can be sent. Each sent message must start a timer so each has its own delay
            tcp(msg);
        }
        else{
            gbn(msg);
        }
    }

    /**
     * where packet is a structure of type pkt. This routine will be called whenever a packet sent from the receiver arrives at the sender.
     * pkt is the (possibly corrupted) packet sent from the sender.
     */
    public void receiveMessage(Packet pkt)
    {
        if (usingTCP)
            tcpReceive(pkt);
        else
            gbnReceive(pkt);
    }

    /**
     * This routine will be called when the sender's timer expires (thus generating a timer interrupt). 
     * You'll probably want to use this routine to control the retransmission of packets. 
     * See starttimer()and stoptimer() below for how the timer is started and stopped
     */
    public void timerExpired()
    { 
        // resends next packet in sequence
        if (usingTCP)
            tcpResend(seq);
        else
            gbnResend();
    }

    public void setTimeLine(Timeline tl)
    {
        this.tl=tl;
    }

    public void setWindowSize(int n)
    {
        windowSize=n;
    }

    public void setProtocol(int n)
    {
        if(n > 0)
            usingTCP=true;
        else
            usingTCP=false;
    }

    public void gbn(Message msg)
    {
        if((lastAck + windowSize) < ack)
        {
            queued.add(msg);
        } 
        else if (queued.size() > 0)
        {
            queued.add(msg);
            Packet pkt = new Packet(queued.get(0), seq, ack, 0);
            ack++;
            sentMessages.add(queued.get(0).getMessage());
            queued.remove(0);
            nl.sendPacket(pkt, 1);
        }
        else
        {
            Packet pkt = new Packet(msg, seq, ack, 0);
            ack++;
            sentMessages.add(msg.getMessage());
            nl.sendPacket(pkt, 1);
        }    
    }

    public void gbnReceive(Packet pkt)
    {
        int receivedAck = pkt.getAcknum();
        if (receivedAck > lastAck)
            lastAck = receivedAck;
        else
            gbnResend();
    }

    public void gbnResend()
    {
        for (int i = lastAck; i < sentMessages.size(); i++)
        {
            Message msg = new Message(sentMessages.get(i));
            Packet pkt = new Packet(msg, seq, i, 0);
            nl.sendPacket(pkt, 1);
        }
    }

    /**
     * Works much the same way as gbn as far as sending. Packet is simply created and sent on. 
     */
    public void tcp(Message msg)
    {
        Packet pkt = new Packet(msg, seq, ack, 0);
        seq++;
        ack++;
        sentMessages.add(msg.getMessage());
        nl.sendPacket(pkt, 1);        
    }

    /**
     * 
     */
    public void tcpReceive(Packet pkt)
    {
        int receivedSeqnum = pkt.getSeqnum();
        //seq = receivedSeqnum + pkt.msg.x.size();
    }

    public void tcpResend(int nextSeq)
    {

    }
}
