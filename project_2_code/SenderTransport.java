import java.util.ArrayList;
/**
 * A class which represents the receiver transport layer
 * 
 * test run string: {"test.txt", "5", "0", ".20","0","0","0"}
 */
public class SenderTransport
{
    private NetworkLayer nl;
    private Timeline tl;
    private int ack;  // what is this. How is it different from lastAck?
    private int lastAck;  // ack number of last packet received in order
    private int windowSize; // window size must be implemented somehow for GBN
    private int seq; // first byte in a packet and/or the next byte expected by the receiver
    private int TCPWindow; // current size of the wondow for TCP
    private int threshold; // current threshold size for tcp window
    private int MSS; // a maximum segment size must be added. Possibly put in NetworkSimulator as well
    private boolean usingTCP;
    private boolean timerOn;
    private ArrayList<String> sentMessages;
    private ArrayList<Message> queued;

    public SenderTransport(NetworkLayer nl){
        this.nl=nl;
        initialize();
        seq = 0;
        ack = 0;
        lastAck = -1;
        timerOn = false;
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
        timerOn = false;
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
        if (windowSize == 0)
        {
            Packet pkt = new Packet(msg, seq, ack, 0);
            ack++;
            sentMessages.add(msg.getMessage());
            nl.sendPacket(pkt, 1);
        }
        else if((lastAck + windowSize) < ack)
        {
            queued.add(msg);
        } 
        else
        {
            Packet pkt = new Packet(msg, seq, ack, 0);
            ack++;
            sentMessages.add(msg.getMessage());
            nl.sendPacket(pkt, 1);
            startTimer();
        }    
    }

    public void gbnReceive(Packet pkt)
    {
        int receivedAck = pkt.getAcknum();
        if (receivedAck > lastAck)
        {
            int openWindow = receivedAck - lastAck;
            lastAck = receivedAck;

            for (int i = 0; i < openWindow; i++)
            {
                if (queued.size() > 0)
                {
                    gbn(queued.get(0));
                    queued.remove(0);
                }
            }
        }
    }

    public void gbnResend()
    {
        boolean wasFirstAck = false;
        if (lastAck == -1)
        {
            lastAck++;
            wasFirstAck = true;
        }   
        for (int i = lastAck; i < lastAck + windowSize; i++)
        {
            if (i == sentMessages.size())
                break;
            Message msg = new Message(sentMessages.get(i));
            Packet pkt = new Packet(msg, seq, i, 0);
            nl.sendPacket(pkt, 1);
            startTimer();
        }        
        if (wasFirstAck)
            lastAck--;
    }

    /**
     * Works much the same way as gbn as far as sending. Packet is simply created and sent on. 
     */
    public void tcp(Message msg){
        // send the next possible messages to fill up window size
        Packet pkt = new Packet(msg, seq, ack, 0);
        seq = seq + MSS;
        sentMessages.add(msg.getMessage());
        nl.sendPacket(pkt, 1);
        startTimer();        
    }

    /**
     * 
     */
    public void tcpReceive(Packet pkt){
        // when an ack is received, move up window to last byte acked
        // window size increases exponentially until there is loss/3 duplicate acks, threshold set to half of last value that worked, wondow set back to 1
        // window size increases exponentially until threshold, then goes up linearly. Once there is a loss/three duplicates it resets to one
        int receivedSeq = pkt.getSeqnum();
        int duplicateAcks = 0;
        // are packets received in groups? Do I have to receive all packets I send before incresing window size or moving it forward?
        if(receivedSeq == seq + MSS){
            seq = seq + MSS;
            if(windowSize >= threshold){
                windowSize++;
            }
            else{
                windowSize = windowSize * 2;
            }
        }
        else if(receivedSeq == seq){
            duplicateAcks++;
        }
        else if(receivedSeq != seq || receivedSeq != seq + 1){
            // ignore
        }
        if(duplicateAcks == 2 /** || timeout())*/){
            windowSize = 1;
            tcpResend(seq);
        }
    }

    public void tcpResend(int seqNum){
        // tcpReceive will tell it when to send a packet back
        // go into list of sent messages and resend the one with seq = seqNum
    }

    public void startTimer()
    {
        if (!timerOn)
        {
            tl.startTimer(400);
            timerOn = true;
        }
    }
}
