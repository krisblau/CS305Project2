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
    private int lastSeq; 
    private int lastResentAck;
    private int lastResentSeq;
    private int windowSize; // window size must be implemented somehow for GBN
    private int seq; // first byte in a packet and/or the next byte expected by the receiver
    private int duplicateSeqs; // counts the number of duplicate acks received
    private int lastDuplicate; // the value of the ack that is being duplicated
    private int expectedAck; // ack number of the next expected ack
    private int expectedSeq; //
    
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
        lastSeq = -1;
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
        // Check that message to be sent is between expectedAck and expectedAck + window size.
        if(windowSize == 0){
            // Create a new packet with the message enclosed and the next ack and sequence numbers included. 
            Packet pkt = new Packet(msg, seq, ack, 0); 
            seq++;
            sentMessages.add(msg.getMessage());
            nl.sendPacket(pkt, 1);            
        }
        else if((lastSeq + windowSize) < seq){
            queued.add(msg);            
        }
        else{
            Packet pkt = new Packet(msg, seq, ack, 0); 
            // Add the packet to the list of sent messages.
            sentMessages.add(msg.getMessage());
            // Increase the value of the sequnce number by one to mark that the next packet in order will be sent.
            seq++;
            // Send the packet.
            nl.sendPacket(pkt, 1);
            // Start the timer.
            startTimer();  
        }       
    }

    /**
     * Receive function for TCP
     * 
     * @param pkt Packet coming in from network.
     */
    public void tcpReceive(Packet pkt){
        // Extract the value of the ack number.
        int receivedSeq = pkt.getSeqnum();
        // Determine of the ack received is the one that is expected or greater.
        if (receivedSeq > lastSeq){
            // If so, good. Increase the value of the next ack expected and reset the number of duplicate acks to zero.
            duplicateSeqs = 1;
            int openWindow = receivedSeq - lastSeq;
            lastSeq = receivedSeq;
            expectedSeq =  receivedSeq + 1;

            for (int i = 0; i < openWindow; i++)
            {
                if (queued.size() > 0)
                {
                    tcp(queued.get(0));
                    queued.remove(0);
                }
            }            
        }    
        // Next determine if the received ack is less than that of the expected ack.
        else
        {
            // If the ack is equal to a previous duplicate, increase the number of duplicates received.
            if(receivedSeq == lastDuplicate){
                duplicateSeqs++;
            }
            // If it a first (not yet a duplicate), set it to the value of the duplicate and increase the number of duplicates to one.
            else{
                lastDuplicate = receivedSeq;
                duplicateSeqs++;
            }
            // If at that point you have four total, or three duplicates, resend the packet for the next expected ack.
            if(duplicateSeqs > 3){
                duplicateSeqs = 0;
                tcpResend();
            }
        }
    }

    public void tcpResend(){
        boolean wasFirstSeq = false;
        if (lastSeq == -1)
        {
            lastSeq++;
            wasFirstSeq = true;
        }       
        if (lastDuplicate < sentMessages.size()){
            Message msg = new Message(sentMessages.get(lastDuplicate));
            Packet pkt = new Packet(msg, lastDuplicate, ack, 0);
            lastDuplicate = 0;
            nl.sendPacket(pkt, 1);
            startTimer();   
        }
        if (wasFirstSeq)
            lastSeq--; 
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
