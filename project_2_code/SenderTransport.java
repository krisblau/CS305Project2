import java.util.ArrayList;
/**
 * A class which represents the receiver transport layer
 * 
 * Arguements: {"input file","average delay", "prob. loss", "prob. corruption", "window size","protocol type","DEBUG"}
 * test run string: {"test.txt", "5", "0", ".20","0","0","0"}
 */
public class SenderTransport
{
    private NetworkLayer nl;
    private Timeline tl;
    private int ack;  // Next ack to be sent
    private int lastAck;  // ack number of last packet received in order
    private int lastResentAck;
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
     * Function called by simulation to send the read message.
     * Sends the message to the correct protocol.
     * 
     * @param Msg Message object that holds the message to be sent over the network.
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
     *  Function called by simulation
     *  Sends the packet to the correct protocol used by the system.
     *  
     *  @param pkt Packet object that is the incoming packet.
     */
    public void receiveMessage(Packet pkt)
    {
        if (usingTCP)
            tcpReceive(pkt);
        else
            gbnReceive(pkt);
    }

    /**
     * Function called by simulation on timeout.
     * Selects the correct resend protocol.
     */
    public void timerExpired()
    { 
        // resends next packet in sequence
        timerOn = false;
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
        windowSize=n;
    }

    public void setProtocol(int n)
    {
        if(n > 0)
            usingTCP=true;
        else
            usingTCP=false;
    }

    /**
     * Send function for Go-Back-N
     * 
     * @param msg Message to be sent coming from the Network Simulator
     */
    public void gbn(Message msg)
    {
        //Window size = 0 just send packets with no wait. (Should never be called)
        if (windowSize == 0)
        {
            Packet pkt = new Packet(msg, seq, ack, 0);
            ack++;
            sentMessages.add(msg.getMessage());
            nl.sendPacket(pkt, 1);
            startTimer();
        }
        //If window is full queue message.
        else if((lastAck + windowSize) < ack)
        {
            queued.add(msg);
        } 
        //If window isn't full send message
        else
        {
            Packet pkt = new Packet(msg, seq, ack, 0);
            ack++;
            sentMessages.add(msg.getMessage());
            nl.sendPacket(pkt, 1);
            startTimer();
        }    
    }

    /**
     * Receive function for Go-Back-N
     * 
     * @param pkt Incoming packet
     */
    public void gbnReceive(Packet pkt)
    {
        int receivedAck = pkt.getAcknum();
        //If received ack is bigger than the last ack received, increment last ack, move window, send any queued packets.
        if (receivedAck > lastAck)
        {
            int openWindow = receivedAck - lastAck;
            lastAck = receivedAck;
            if(openWindow == 0)
                tl.stopTimer();
            //Send queued packets up to window size.
            for (int i = 0; i < openWindow; i++)
            {
                if (queued.size() > 0)
                {
                    gbn(queued.get(0));
                    queued.remove(0);
                }
            }
        } else if (receivedAck < lastAck)
        {
            lastAck = receivedAck;
        }
    }

    /**
     * Resends all packets in the window after the last received ack
     */
    public void gbnResend()
    {
        boolean wasFirstAck = false;
        //If its first ack increase by one to avoid out of bounds exception
        if (lastAck == -1)
        {
            lastAck++;
            wasFirstAck = true;
        }
        //Resend each packet already transmitted
        for (int i = lastAck; i < lastAck + windowSize; i++)
        {
            //Avoids out of bounds exception if there are less acks to be sent than the open window size.
            if (i == sentMessages.size())
                break;
            Message msg = new Message(sentMessages.get(i));
            Packet pkt = new Packet(msg, seq, i, 0);
            nl.sendPacket(pkt, 1);
            startTimer();
            lastResentAck = pkt.getAcknum();
        }       
        if (lastResentAck < lastAck)
            stopTimer();
        
        //Reset last ack to -1
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
     * Receive function for TCP
     * 
     * @param pkt Packet coming in from network.
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
            tcpResend();
        }
    }

    public void tcpResend(){
        // tcpReceive will tell it when to send a packet back
        // go into list of sent messages and resend the one with seq = seqNum
    }

    /**
     * Function to start timer ensuring that a timer is not already running.
     */
    public void startTimer()
    {
        if (!timerOn)
        {
            tl.startTimer(400);
            timerOn = true;
        }
    }
    
    /**
     * Function to stop timer ensuring that a timer is not already running.
     */
    public void stopTimer()
    {
        if (timerOn)
        {
            tl.stopTimer();
            timerOn = false;
        }
    }
}
