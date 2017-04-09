import java.util.ArrayList;
/**
 * A class which represents the receiver transport layer
 * 
 * Arguements: {"input file","average delay", "prob. loss", "prob. corruption", "window size","protocol type","DEBUG"}
 * use 0 for GBN, 1 for TCP
 * test run string: {"test.txt", "5", "0", "0","3","1","0"}
 */
public class SenderTransport
{
    private NetworkLayer nl;
    private Timeline tl;

    private int ack;  // the ack number attached to a packet. This marks the value of what is being expected in the next packet.
    private int seq; // the sequence number attached to a packet. This marks the value of what is being sent in a packet.
    private int lastAck;  // ack number of last packet received in order
    private int lastSeq; // seq number for last segment delivered.
    private int expectedAck; // ack number of the next expected ack
    private int windowSize; // window size must be implemented for tcp and gbn

    private int duplicateAcks; // counts the number of duplicate acks received
    private int lastDuplicate; // the value of the ack that is being duplicated

    private boolean usingTCP; // boolean to indicate if GBN or TCP is being used
    private boolean timerOn; // boolean value marking that a timer has been set

    private ArrayList<String> sentMessages; // list of all messages that sender has ever sent
    private ArrayList<Message> queued; // queue for out of order messages

    public SenderTransport(NetworkLayer nl){
        this.nl=nl;
        initialize();
        seq = 1; // The first segment being sent is number one.
        ack = 0; // The first ack expected is for packet zero.
        lastAck = -1;
        expectedAck = 0;
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
        if (usingTCP){
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
        // Create a new packet with the message enclosed and the next ack and sequence numbers included.
        Packet pkt = new Packet(msg, seq, ack, 0);  
        // Check that message to be sent is between expectedAck and expectedAck + window size.
        if(windowSize == 0){
            ack++;
            sentMessages.add(msg.getMessage());
            nl.sendPacket(pkt, 1);            
        }
        else if(lastAck + windowSize < ack){
            queued.add(msg);            
        }
        else{
            // Add the packet to the list of sent messages.
            sentMessages.add(msg.getMessage());
            System.out.println("Message has been sent: " + pkt.getAcknum());
            // Increase the value of the sequnce number by one to mark that the next packet in order will be sent.
            ack++;
            // Send the packet.
            nl.sendPacket(pkt, 1);
            // Start the timer.
            startTimer();  
        }
    }

    /**
     *  This method decides what action is to be taken upon the arrival of a new ack.
     *  Is packet in order?
     */
    public void tcpReceive(Packet pkt){
        // Extract the value of the ack number.
        int receivedAck = pkt.getAcknum();
        // Determine of the ack received is the one that is expected or greater.
        if (receivedAck > lastAck){
            // If so, good. Increase the value of the next ack expected and reset the number of duplicate acks to zero.
            System.out.println("Ack successfully received for packet: " + receivedAck);
            lastAck = receivedAck;
            expectedAck =  receivedAck + 1;
            duplicateAcks = 0;
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
        // Next determine if the received ack is less than that of the expected ack.
        else if(receivedAck <= lastAck){
            // If the ack is equal to a previous duplicate, increase the number of duplicates received.
            if(receivedAck == lastDuplicate){
                duplicateAcks++;
            }
            // If it a first (not yet a duplicate), set it to the value of the duplicate and increase the number of duplicates to one.
            else{
                System.out.println("New duplicate is: " + receivedAck);
                lastDuplicate = receivedAck;
                duplicateAcks++;
            }
            // If at that point you have four total, or three duplicates, resend the packet for the next expected ack.
            if(duplicateAcks > 3){
                System.out.println("Triple Ack is: " + receivedAck);
                duplicateAcks = 0;
                tcpResend();
            }
        }
    }

    /**
     * This method resends the a packet after either a timeout or in the case of three duplicate acks.
     * Retransmit first time after timeout only, fast retrasnmit after 3 duplicate acks. Sender sends next expected before timeout occurs.
     */
    public void tcpResend(){ 
        boolean wasFirstAck = false;
        if (lastAck == -1)
        {
            lastAck++;
            wasFirstAck = true;
        }        
        for (int i = lastAck; i < lastAck + windowSize; i++)
        {
            if (i == sentMessages.size()){
                Message msg = new Message(sentMessages.get(lastDuplicate));
                Packet pkt = new Packet(msg, seq, lastDuplicate, 0);
                lastDuplicate = 0;
                nl.sendPacket(pkt, 1);
                startTimer(); 
            }             
        }
        if (wasFirstAck)
            lastAck--;        
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
