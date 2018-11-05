//*********************************************************************************
//
//    Copyright(c) 2018  Kevin Willows All Rights Reserved
//
//    Unless required by applicable law or agreed to in writing, software
//    distributed under the License is distributed on an "AS IS" BASIS,
//    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//    See the License for the specific language governing permissions and
//    limitations under the License.
//
//*********************************************************************************
//
package org.edforge.util;

import java.util.Timer;

/**
 * Created by kevin on 3/22/2018.
 */

public class CLogManager {
    
    private Boolean traceMode  = true;
    private Boolean fdebugMode = false;

    private CDDnsLoader dnsLoader;
    private CLogSocket  logSocket;

    private Object  xmlEvents;

    private String _logHostAddress;					// Result of DNS lookup
    private int _logHostPort;						// *
    private String _forcedAddress = "";

    private Boolean _DataStreaming  = false;
    private Boolean _QueStreaming  	= false;
    // Log Waiting indicates that there is not a data network transfer in progress
    private Boolean _logWaiting     = true;			// Need this to be true for when we start initially

    private Boolean _sending   		= false;		// Debug flag
    private Boolean _authenticating = false;

    private Boolean _fReconnect 	= false; 		// indicates when a socket retry is underway

    private Boolean _isConnecting 	= false;
    private Boolean _isConnected 	= false; 		// indicates when a socket is connected

    private Boolean _sessionActive  = false;		// indicates the session is over and the logQueue should closed and flushed to the server
    private String _sessionID		= "";			// server session id - used for reconnect to log files
    private int _sessionTime;

    private String _sessionStatus 	= SESSION_START;
    private Object _sessionAccount:Object;

    public static final String SESSION_START 		= "sessionstart";
    public static final String SESSION_RUNNING 		= "sessionrunning";
    public static final String SESSION_INTERRUPTED 	= "sessioninterrupted";
    public static final String SESSION_COMPLETE		= "sessioncomplete";

    private Timer logEventTimer = new Timer(60);		// queue packets on every tick
    private Timer logTimeout    = new Timer(10000, 1);	// use a 40sec time out on responses from TED service

    private Boolean _useQueue 	= true;

    // Session management 

    public  String _fTutorPart = "test";				// Goes in to the  header to indicate the portion of the tutor the file represents

    // development trace

    public 	Object tracer;

    // Logging parameters

    private int _fLogging;



    // Playback counters

    private String LogSource;						// Playback can come from either Recorded Events (cached playback) or and object (Logged playback)

    private int lastAction;							//@@Legacy playback
    private int lastMove;							//@@Legacy playback

    private Boolean fPlayBackDone;					// set when playBackNdx reaches playBackSiz
    private int playBackNdx;							// replay progress counter
    private int playBackSiz;							// size of the current playback object


    // ** Network message types - Server Response packet contents

    public static final String xmlUSER_AUTH			=	"userAuth";
    public static final String xmlUPDATE_PROGRESS	=	"updateProgress";
    public static final String xmlLOG_STATE			=	"logState";
    public static final String xmlQUERY_STATE		=	"queryState";

    public static final String xmlACKAUTH			=	"ackauth";
    public static final String xmlNAKAUTH			=	"nakauth";

    public static final String xmlACKPROGLOG		=	"ackprogresslog";
    public static final String xmlNAKPROGLOG		=	"nakprogresslog";

    public static final String xmlACKSTATEQUERY		=	"ackstatequery";
    public static final String xmlNAKSTATEQUERY		=	"nakstatequery";

    public static final String xmlACKLATESTSTATEQUERY=	"acklateststatequery";
    public static final String xmlNAKLATESTSTATEQUERY=	"naklateststatequery";

    public static final String xmlACKSTATELOG		=	"ackstatelog";
    public static final String xmlNAKSTATELOG		=	"nakstatelog";

    public static final String xmlERROR				=	"error";
    public static final String xmlMESSAGE			=	"message";
    public static final String xmlSQLERROR			=	"sqlerror";

    public static final String INVALID_USER			=	"INVALID_USERPASS";

    // Singleton implementation

    private static CLogManager _instance;		//
    private static CLogQueue   _logQueue;



    /**
     * Constructor has an optional tracearea object that is used for
     * development purposed only
     *
     * @param _StextArea
     *
     */
    // Singleton
    private static CLogManager ourInstance = new CLogManager();

    public static CLogManager getInstance() { return ourInstance; }

    private CLogManager()
    {
        super();

        // generate the queue associated with this manager

        _logQueue = new CLogQueue();

        // Listen to the Queue and pass on events

        _logQueue.addEventListener(CLogEvent.PROG_MSG, progressListener);
    }


    public void useLocalHost() : void
    {
        fdebugMode = true;
    }


    private void progressListener(e:CLogEvent) : void
    {
        if(hasEventListener(CLogEvent.PROG_MSG))
            dispatchEvent(e);
    }

    public void queryTheQueue() : void
    {
        _logQueue.emitProgress();
    }


    public int get fLogging() :int
    {
        return _fLogging;
    }


    public void set fLogging(int newVal)
    {
        _fLogging = newVal;

        // record them locally

        //Alert.show("_flogging = " + _fLogging.toString(), "Notice");

        if(_fLogging & CLogQueue.RECORDEVENTS) _logQueue.openQueue();
        else _logQueue.closeQueue();
    }


    public void set account(_account:Object): void
    {
        _sessionAccount = _account;
    }

    public String get fTutorPart() : String
    {
        return _fTutorPart;
    }


    public void set fTutorPart(newVal:String) :void
    {
        _fTutorPart = newVal;
    }


    public void setQueueStreamState(startQueue:Boolean) :void
    {
        // Send queued data to remote log 

        if(startQueue && (_fLogging & CLogQueue.LOGEVENTS))
        {
            startQueuedStream();

            trace('Stream now Open');
        }
        else
        {
            stopQueuedStream();

            trace('Stream now Closed');
        }
    }

    public String getQueueStreamState() : String
    {
        result:String;

        if(_logQueue.isStreaming)
            result = CLogEvent.CONNECTION_OPEN;
        else
            result = CLogEvent.CONNECTION_CLOSED;

        return result;
    }


    public String getQueueState() : String
    {
        result:String;

        // If the stream is open then send the next packet.

        if(_QueStreaming)
        {
            // If there is anything buffered send it
            // Queue stream is kept flowing here -

            if(!_logQueue.isQueueEmpty())
                result = CLogEvent.QUEUE_OPENED;
            else
                result = CLogEvent.QUEUE_WAITING;
        }
        else
            result = CLogEvent.QUEUE_CLOSED;

        return result;
    }

//************************************************************************************		
///**** Interface

    /**
     *
     */
    public void connectProtocol(func:Function) : void
    {
        // Connect listeners from the socket

        if(logSocket)
            logSocket.addEventListener(DataEvent.DATA,func);
    }


    /**
     *
     */
    public void disConnectProtocol(func:Function) : void
    {
        // Connect listeners from the socket			

        if(logSocket)
            logSocket.removeEventListener(DataEvent.DATA,func);
    }


    /**
     * Attach a tracer text control to the logmanager - for debug and tracing 
     *
     */
    public void attachTracer(_StextArea:Object ) : void
    {
        tracer = _StextArea;
    }


    public void connectForInterface() : void
    {
        indirectConnectSocket();
    }


    public void connectToAuthenticate() : void
    {
        if(!_authenticating)
        {
            _authenticating = true;

            indirectConnectSocket();
        }
    }


    // When the socket connects CAuthenticationManager:sessionManagementProtocol will manage the
    // reattachment to the running session

    public void connectToReattach() : void
    {
        if(!_authenticating)
        {
            _authenticating = true;

            directConnectSocket();
        }
    }


    /**
     * Connect the socket - This initiates a two step process
     *
     * Step 1 - Does a DDNS lookup to find the current logger location
     * Step 2 - Connect to the logger 
     *
     */
    private void indirectConnectSocket() : void
    {
        // Don't allow overlapped calls to connect 

        if(!(_isConnecting || _isConnected))
        {
            // indicate connection in progress -
            // Used to indicate failure during the initial socket connection

            _isConnecting = true;

            if(hasEventListener(CLogEvent.CONNECT_STATUS))
                dispatchEvent(new CLogEvent(CLogEvent.CONNECT_STATUS, CLogEvent.DDNS_IN_PROGRESS));


            // If we get a good loader then do the DDNS lookup

            if(!dnsLoader)
                dnsLoader = new CDDnsLoader(null, tracer);

            if(dnsLoader)
            {
                dnsLoader.addEventListener(CDnsEvent.COMPLETE, DNSresolved );
                dnsLoader.addEventListener(CDnsEvent.FAILED, DNSfailed );

                dnsLoader.resolveArbiter();
            }
            else
                _isConnecting = false;
        }
    }


    /**
     *  If we have already done a DDNS lookup then we can just connect
     *
     * @param evt
     *
     */
    private void directConnectSocket() : void
    {
        // Create the new socket
        //	Wires logSocket - CLogEvent.CONNECT_STATUS to socketConnectionHdlr
        //	Wires logSocket - DataEvent.DATA,protocolHandlerLGR			

        createSocket();

        if(tracer) tracer.TRACE("Connecting: ", '#000088');

        // when the socket connects that will initiate the authentication protocol

        try
        {
            //#### DEBUG - force socket address

            if(fdebugMode)
            {
                _logHostAddress = "127.0.0.1";
                _logHostPort    = CXMLSocket.PORT_LOGGER;
            }

            logSocket.openSocket(_logHostAddress, _logHostPort);

        }
        catch(error:Error)
        {
            trace("catch all" + error);
        }
    }


    /**
     * Determine if a connection is active or pending
     *
     */
    public Boolean get connectionActive() : Boolean
    {
        return (_isConnected);
    }


    public String getConnectionState() : String
    {
        result:String;

        if(_isConnected)
            result = CLogEvent.CONNECTION_OPEN;
        else
            result = CLogEvent.CONNECTION_CLOSED;

        return result;
    }


    /**
     * Determine if a connection is active or pending
     *
     */
    public Boolean get connectionActiveOrPending() : Boolean
    {
        return (_isConnecting || _isConnected);
    }


    public String get sessionID() : String
    {
        return _sessionID;
    }


    public function get sessionHost() : String
    {
        return _logHostAddress;
    }


    public function set sessionHost(newHost:String) : void
    {
        _logHostAddress = newHost;
    }


    public int get sessionPort() : uint
    {
        return _logHostPort;
    }


    public void set sessionPort(newPort:uint) : void
    {
        _logHostPort = newPort;
    }


    /**
     * Prior to starting a session, decide whether we are using queued data
     *
     */
    public void useQueue(useQ:Boolean) : void
    {
        if(_sessionID == "")
            _useQueue = useQ;
    }


//***************************************************
//*** Session Management

    /**
     * Determine if a session is active
     *
     */
    public Boolean get isSessionActive() : Boolean
    {
        return _sessionActive;
    }


    /**
     * Determine if a session is active
     *
     */
    public String get sessionStatus() : String
    {
        return _sessionStatus;
    }


    /**
     * Manual method to abandon a session while the socket is disconnected
     *
     * Note: Only after a session reset you can reselect whether to use queued data
     *
     * Used by CAuthenticator to force abandon a connection and reset the LogManager
     * 				 - _logManager.abandonSession(true, CLogManager.SESSION_START);
     *
     * Used internally to Abandon the session after Termination Packet is acknowledged
     *
     * #### DEBUG Support - Used in the CConnectionPanel to manually disconnect the LogSocket
     */
    public void abandonSession(abandonData:Boolean = false, newStatus:String = SESSION_START ) : void
    {
        _sessionActive = false;
        _sessionStatus = newStatus;

        _sessionID   = "";
        _sessionTime = 0;

        fLogging = CLogQueue.RECLOGNONE;

        // reset any connections and abandon queued data

        abandonSocket(abandonData);
    }


    /**
     * Force a manual disconnection of the active socket. 
     * Used in the termination phase to close the socket in response to ACKTERM which
     * the server sends once all data has been processed and the session context marked complete.
     *
     * Should be Private other than #### DEBUG Support - Used in the CConnectionPanel to manually crash the LogSocket
     *
     */
    public void abandonSocket(abandonData:Boolean = false ) : void
    {
        if(tracer) tracer.TRACE("Socket Disconnect Requested: ", '#008800');
        if(traceMode) trace("@@@@@@@@@@@@@@@@@@@@@@ ABANDON SOCKET @@@@@@@@@@@@@@@@@@@@@@@@@@@@@");

        if(logSocket)
        {
            if(logSocket.connected)
            {
                if(tracer) tracer.TRACE("Socket closing: ", '#000088');
            }
            else
            {
                if(tracer) tracer.TRACE("Socket Not Connected: ", '#880000');

                // If it is not connected then it won't send anything to socketConnectionHdlr
                // so we need to reset these here

                _isConnected    = false;
                _isConnecting   = false;
            }

            // stop watching the connection status
            // Disconnects the listeners prior to close - so UI will not be informed of operation

            cleanupSocket();

            // Force the stream state back to default - only if currently streaming
            // Note: this is part of the DEBUG test rig

            stopDebugDataStream();

            // Abandon data in the queue only if requested - 

            if(abandonData)
                _logQueue.resetQueue();

            // Let everyone know what has happened.

            if(hasEventListener(CLogEvent.CONNECT_STATUS))
                dispatchEvent(new CLogEvent(CLogEvent.CONNECT_STATUS, CLogEvent.CONNECTION_TERMINATED));
        }
        else
        {
            if(tracer) tracer.TRACE("Socket is NULL: ", '#880000');

            if(hasEventListener(CLogEvent.CONNECT_STATUS))
                dispatchEvent(new CLogEvent(CLogEvent.CONNECT_STATUS, CLogEvent.CONNECTION_TERMINATED));
        }
    }


    private void timeStampSession() : void
    {
        // Init the start time of the session

        _sessionTime = getTimer();
    }


    private String get sessionTime() : String
    {
        curTime:Number;

        curTime = (getTimer() - _sessionTime) / 1000.0;

        return curTime.toString();
    }


    public void submitAuthentication(logData:*) : void
    {
        if(tracer) tracer.TRACE("Sending Authentication Request...", "#000088" );

        // returned data is handled in protocolHandlerLGR

        sendJSONPacket(logData);

        // publish stream status change 

        if(hasEventListener(CLogEvent.SEND_STATUS))
            dispatchEvent(new CLogEvent(CLogEvent.SEND_STATUS));
    }


    public void submitJSONQuery(logData:*) : void
    {
        if(tracer) tracer.TRACE("Sending Interface Request...", "#000088" );

        // returned data is handled in protocolHandlerLGR

        sendJSONPacket(logData);

        // publish stream status change 

        if(hasEventListener(CLogEvent.SEND_STATUS))
            dispatchEvent(new CLogEvent(CLogEvent.SEND_STATUS));
    }


//*** Session Management
//***************************************************


//***************************************************
//*** Data Management

    // push the event onto the stack
    //
    public void flushGlobalStateLocally(name:String) : void
    {
        file:FileReference = new FileReference();


//			file.save(_lastStatePacket, name + _fTutorPart + ".json");
    }


    private Object generateEvent(logData:Object, type:String) : Object
    {
        logData['type']    = type;
        logData['version'] = '1.0';
        logData['time']    = sessionTime;
        logData['seqid']   = _logQueue.nextNdx;
        logData['userid']  = _sessionAccount.userData._id;

        // Generate an mongo insert packet for the log event

        logData = CMongo.insertPacket('logmanager',
                CMongo.LOG_PACKET,
                'unused',
                logData);

        // The seqid is used for receive acknowlegement 
        logData = logData.replace("{", '{"seqid":'+_logQueue.nextNdx +',');

        return logData;
    }


    // push the event onto the stack
    //
    public void logSessionIDEvent() : void
    {
        //if (logTrace) trace("seqid=" + (logEvtIndex + 1) + "   frameID=" + CWOZDoc.gApp.frameID + "   stateID=" + CWOZDoc.gApp.stateID);

        // Set the base time for the session - records are logged relative to this.

        timeStampSession();

        logData:Object = {'event':'sessionID', 'name':_sessionID, 'part':_fTutorPart};

        logData = generateEvent(logData, 'SessionEvent');

        _logQueue.logEvent(logData);
    }


    // push the event onto the stack
    //
    public void logLiveEvent(logData:Object) : void
    {
        //if(logTrace) trace("seqid=" + (_logQueue.nextNdx) + "   frameID=" + CWOZDoc.gApp.frameID + "   stateID=" + CWOZDoc.gApp.stateID);

        // Generate the log record
        //
        logData = generateEvent(logData, 'WOZevent');

        _logQueue.logEvent(logData );
    }


    // push the event onto the stack
    //
    public void logActionEvent(logData:Object) : void
    {
        //if(logTrace) trace("seqid=" + (_logQueue.nextNdx) + "   frameID=" + CWOZDoc.gApp.frameID + "   stateID=" + CWOZDoc.gApp.stateID);

        // Generate the log record
        //
        logData = generateEvent(logData, 'ActionEvent');

        _logQueue.logEvent(logData );
    }


    // push the event onto the stack
    //
    public void logStateEvent(logData:Object) : void
    {
        //if(logTrace) trace("seqid=" + (_logQueue.nextNdx) + "   frameID=" + CWOZDoc.gApp.frameID + "   stateID=" + CWOZDoc.gApp.stateID);

        // Generate the log record
        //
        logData = generateEvent(logData, 'StateEvent');

        _logQueue.logEvent(logData );
    }


    // push the event onto the stack
    //
    public void logNavEvent(logData:Object) : void
    {
        //if(logTrace) trace("seqid=" + (_logQueue.nextNdx) + "   frameID=" + CWOZDoc.gApp.frameID + "   stateID=" + CWOZDoc.gApp.stateID);

        // Generate the log record
        //
        logData = generateEvent(logData, 'NavEvent');

        _logQueue.logEvent(logData );
    }


    // push the event onto the stack
    //
    public void logDurationEvent(logData:*) : void
    {
        // Generate the log record
        //
        logData = generateEvent(logData, 'DurationEvent');

        _logQueue.logEvent(logData );
    }


    // push the event onto the stack
    //
    public void logProgressEvent(logData:*) : void
    {
        // Generate the log record
        //						
        logData = CMongo.updatePacket('logManager',
                CMongo.LOG_PROGRESS,
                'unused',
                {"_id":_sessionAccount.userData._id},
        logData['reify']);

        // The seqid is used for receive acknowlegement
        logData = logData.replace("{", '{"seqid":'+_logQueue.nextNdx +',');

        _logQueue.logEvent(logData );
    }


    /**
     * This enqueues an "End Packet" that initiates the sequence of events that 
     * terminates the session. 
     *
     */
    public void logTerminateEvent() : void
    {
        // Emit the "Terminate" log record - then close the queue to new data
        // Existing data will be flushed and connection failures will result
        // in reconnects until we are successful in flushing the queue.
        //

        // Generate the log record
        //
        termMsg:Object = new Object;

        // should be either CObjects, MObjects or AS3 primitive data types String, Number,int,Boolean,Null,void
        profileNdx:String = _sessionAccount.session.profile_Index;

        termMsg['phases']             = new CObject;
        termMsg['phases'][profileNdx] = new CObject;

        termMsg['phases'][profileNdx]['progress'] = CMongo._COMPLETE;

        termMsg = CMongo.updatePacket('logManager',
                CMongo.LOG_TERMINATE,
                'unused',
                {"_id":_sessionAccount.userData._id},
        termMsg);

        _logQueue.logEvent(termMsg );

        _logQueue.closeQueue();

        /**
         * #Mod Jun 3 2014 - handle non logging modes
         *
         * This is a special case to handle tutor termination when the logging is deactivated - i.e. demo modes
         * This event is handled by the session manager FSM to update the user UI 
         *
         */
        if(_fLogging & CLogQueue.LOGEVENTS)
        {
            // Let anyone interested know we are flushing the queue

            if(hasEventListener(CLogEvent.SESSION_STATUS))
                dispatchEvent(new CLogEvent(CLogEvent.SESSION_STATUS, CLogEvent.SESSION_TERMINATED ));
        }
        else
        {
            // Let everyone know what has happened.

            if(hasEventListener(CLogEvent.SESSION_STATUS))
                dispatchEvent(new CLogEvent(CLogEvent.SESSION_STATUS, CLogEvent.SESSION_FLUSHED ));
        }

    }


    // push the event onto the stack
    //
    public void logDebugEvent(logData:*) : void
    {
        // Generate the log record
        //
        generateEvent(logData, 'DebugEvent');

        _logQueue.logEvent(logData );
    }


    // push the event onto the stack
    //
    public void logErrorEvent(logData:Object) : void
    {
        // Generate the log record
        //
        generateEvent(logData, 'ErrorEvent');

        _logQueue.logEvent(logData );
    }


    /**
     *
     * @param	evtXML
     */
    public Boolean sendPacket(packet:*) : Boolean
    {
        return sendXMLPacket(packet);
    }


    /**
     *
     * @param	evtXML
     */
    private Boolean sendXMLPacket(packet:*) : Boolean
    {
        if(traceMode) trace("@@@@@@@  QUEUEING XML PACKET: \n", packet);

        packetStr:String;
        fResult:Boolean = false;

        // Don't attempt send on dead socket
        //
        if(_isConnected)
        {
            // Do asynchronous send of packet - protocol must manage exceptions
            // Note: We expect either XML or JSON packets

            if(packet is XML)
            packetStr = packet.toXMLString();
				else
            packetStr = packet;

            if(!logSocket.sendData(packetStr))
            {
                //** Send Exception indicates that the socket is not connected

                if(tracer) tracer.TRACE("LSocket: send: Connection Error...", "#880000", logSocket._lastError.toString());
                if(traceMode) trace("@@@@@@@@@@@@@@@@@@@@@@ SOCKET OFFLINE @@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
            }
            else
            {
                fResult = true;

                if(tracer && packet is XML)
                {
                    if(packet.children()[0].name() == "terminatesession")
                    {
                        tracer.TRACE("#END#");
                    }
                }
            }
        }

        return fResult;
    }


    /**
     *
     */
    private Boolean sendJSONPacket(packet:*) : Boolean
    {
        if(traceMode) trace("@@@@@@@  SENDING JSON LOG PACKET: \n", packet);

        packetStr:String;
        fResult:Boolean = false;

        // Don't attempt send on dead socket
        //
        if(_isConnected)
        {
            // Do asynchronous send of packet - protocol must manage exceptions
            // Note: We expect either XML or JSON packets

            if(packet is String)
            packetStr = packet;
				else
            packetStr = JSON.stringify(packet);

            if(!logSocket.sendData(packetStr))
            {
                //** Send Exception indicates that the socket is not connected
                // Note that we expect the socket exception handler to manage the socket cleanup and announcing the  
                // failure to listeners  - See: socketConnectionHdlr

                if(tracer) tracer.TRACE("LogSocket: send: Connection Error...", "#880000", logSocket._lastError.toString());
                if(traceMode) trace("@@@@@@@@@@@@@@@@@@@@@@ SOCKET OFFLINE @@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
            }
            else
            {
                // watch for timeouts receiving responses from service.
                logTimeout.reset();
                logTimeout.addEventListener(TimerEvent.TIMER_COMPLETE, socketTimeout);
                logTimeout.start();

                trace("created Timer : " + logTimeout);

                fResult = true;
            }
        }

        return fResult;
    }


    private void resetSendTimer() : void
    {
        if(traceMode) trace("SOCKET TIMER - Cleaned up ");

        logTimeout.reset();
        logTimeout.removeEventListener(TimerEvent.TIMER_COMPLETE, socketTimeout);
    }


    /**
     *  Manage ackhowledgement timeouts
     *
     * 	We need to recycle the socket in this case
     */
    private void socketTimeout(e:TimerEvent) : void
    {
        if(traceMode) trace("@@@@@@@@@@@@@@@@@@@@@@ SOCKET TIMEOUT @@@@@@@@@@@@@@@@@@@@@@@@@@@@@");

        resetSendTimer();

        // force close the socket as if it had failed and the the system recycle the connection. 

        recycleConnection(false);
    }

//*** Data Management		
//***************************************************




//**********************************************************		
//*****************  START - DEBUG API

//***************************************************
//*** Debug Data Management		

    /**
     * This is the public debug send API - it can be used for either immediate or queued data transfers
     *
     * This is for the Send Button Protocol - simple send/ack 
     *
     */
    public function sendDebugPacket(logData:Object) : void
    {
        if(tracer) tracer.TRACE("Queueing Debug Packet...", "#000088" );

        if(!_sending)
        {
            if(_useQueue)
            {
                logDebugEvent(logData);
            }
            else if(sendXMLPacket(logData))
            {
                logSocket.addEventListener(DataEvent.DATA, ackPacket);

                _sending = true;
            }

            // publish stream status change 

            if(hasEventListener(CLogEvent.SEND_STATUS))
                dispatchEvent(new CLogEvent(CLogEvent.SEND_STATUS));
        }
    }


    private function ackPacket(evt:DataEvent) : void
    {
        data:XML = XML(evt.data);

        if(tracer) tracer.TRACE("LSocket: dataHandler...", "#000088", "Ack type: " + data.ack.@type);

        logSocket.removeEventListener(DataEvent.DATA, ackPacket);

        _sending = false;

        // publish stream status change 

        if(hasEventListener(CLogEvent.SEND_STATUS))
            dispatchEvent(new CLogEvent(CLogEvent.SEND_STATUS));
    }


//*******************************************		
//********* DEBUG stream API

    /**
     *  DEBUG !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
     *
     */
    public function startDebugDataStream() : void
    {
        if(!_DataStreaming)
        {
            _DataStreaming = true;

            logSocket.addEventListener(DataEvent.DATA, ackStream);

            if(sendJSONPacket({'event':'noop'}))
            {
                if(tracer) tracer.TRACE("Data Streaming...", "#000088", "*" );
            }
				else
            {
                _DataStreaming = false;

                logSocket.removeEventListener(DataEvent.DATA, ackStream);
            }

            // publish stream status change 

            if(hasEventListener(CLogEvent.DATASTREAM_STATUS))
                dispatchEvent(new CLogEvent(CLogEvent.DATASTREAM_STATUS, CLogEvent.STREAM_OPENED));
        }
    }


    /**
     *  DEBUG !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
     *
     * Force the data stream to close down - used to force stream into a known state
     * following a socket close.
     */
    public function stopDebugDataStream() : void
    {
        if(_DataStreaming)
        {
            _DataStreaming = false;

            if(tracer) tracer.TRACE("Data Stream Closed...", "#000088" );

            // Stop listening to the queue for data - this keeps the 
            // queueChanged function from restarting the stream

            logSocket.addEventListener(DataEvent.DATA, ackStream);

            // publish stream status change 

            if(hasEventListener(CLogEvent.DATASTREAM_STATUS))
                dispatchEvent(new CLogEvent(CLogEvent.DATASTREAM_STATUS, CLogEvent.STREAM_CLOSED));
        }

    }


    /**
     *  DEBUG !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
     *
     */
    private function ackStream(evt:DataEvent) : void
    {
        data:XML = XML(evt.data);

        if(data.ack.@type == "NOOP")
        tracer.TRACE("*");

        if(_DataStreaming)
            sendXMLPacket({'event':'noop'});
			else
        logSocket.removeEventListener(DataEvent.DATA, ackStream);
    }


    //********* DEBUG stream API
    //*******************************************		
		/*
		 * Note the DataEvent.DATA coming from the logSocket are handled by the   
		 * protocolHandlerLGR just as live packets would be
		 * 
		 */
    private function startQueuedStream() : void
    {
        if(!_QueStreaming)
        {
            _QueStreaming = true;

            if(tracer) tracer.TRACE("Queued Stream Running...", "#000088", "*" );

            // Start listening to the queue for data added - CLogEvent.QUEUE_CHANGED events

            _logQueue.addEventListener(CLogEvent.QUEUE_CHANGED, queueChanged);

            // Start the queue stream
            // Kick start the stream if the queue is not empty

            _logQueue.startQueueStream();

            // publish stream status change 

            if(hasEventListener(CLogEvent.STREAM_STATUS))
                dispatchEvent(new CLogEvent(CLogEvent.STREAM_STATUS, CLogEvent.STREAM_OPENED));
        }
    }


    /**
     * Note the DataEvent.DATA coming from the logSocket are handled by the   
     * protocolHandlerLGR just as live packets would be
     *
     */
    private function stopQueuedStream() : void
    {
        if(_QueStreaming)
        {
            _QueStreaming = false;

            if(tracer) tracer.TRACE("Queued Stream Stopped...", "#000088" );

            // Stop listening to the queue for data - this keeps the 
            // queueChanged function from restarting the stream

            _logQueue.removeEventListener(CLogEvent.QUEUE_CHANGED, queueChanged);

            // Start the queue stream
            // Kick start the stream if the queue is not empty

            _logQueue.stopQueueStream();

            // publish stream status change 

            if(hasEventListener(CLogEvent.STREAM_STATUS))
                dispatchEvent(new CLogEvent(CLogEvent.STREAM_STATUS, CLogEvent.STREAM_CLOSED));
        }
    }


    /**
     * Receives notifications when the Log Queue changes -
     * The queue constantly sends the next packet when one is available or 
     * goes into a waiting state if not - When empty the next packet added 
     * to the queue is sent here to restart the stream
     *
     * @param evt
     *
     */
    private function queueChanged(evt:Event) : void
    {
        // if something is added to the queue and stream is quiescent 
        // restart the stream 

        // Note: must not do anything if log is not waiting for data since it will then be waiting 
        //       for an acknowledgement - if we call nextpacket we'll get a duplicate of the one that 
        //       is waiting for acklog.

        if(_logWaiting && !_logQueue.isQueueEmpty())
        {
            if(sendJSONPacket(_logQueue.nextPacket()))
            {
                if(tracer) tracer.TRACE("#");

                _logWaiting = false;

                if(hasEventListener(CLogEvent.QUEUE_STATUS))
                    dispatchEvent(new CLogEvent(CLogEvent.QUEUE_STATUS, CLogEvent.QUEUE_SENDING));
            }
        }
    }


//********************************************************************
//** Development Queue simulation - timed loop to add to _logQueue 


    /**
     * Start a timer to add noop events to the queue
     */
    public function startQueueing() : void
    {
        trace("start queueing");
        logEventTimer.addEventListener(TimerEvent.TIMER, queueCallBack);
        logEventTimer.start();
    }


    /**
     * Stop the timer that adds noop events to the queue
     */
    public function stopQueueing() : void
    {
        trace("stop queueing");
        logEventTimer.stop();
        logEventTimer.removeEventListener(TimerEvent.TIMER, queueCallBack);
    }


    private function queueCallBack(evt:TimerEvent) : void
    {
        // Generate the log record
        //
        logData:Object = {'event':'noop'};

        logDebugEvent(logData);

        if(traceMode) trace(".");
    }


//** Development Queue simulation - timed loop to add to _logQueue 
//********************************************************************		

//*****************  END - DEBUG API
//**********************************************************				

///**** Interface
//************************************************************************************		



//********************************************************************		
///**** State Management


    public function get isDataStreaming() : Boolean
    {
        return _DataStreaming;
    }


    public function get isQueueStreaming() : Boolean
    {
        return _QueStreaming;
    }


    public function get queueLength() : int
    {
        return _logQueue.length;
    }


    public function get queuePosition() : int
    {
        return _logQueue.Position;
    }


    public function get isSending() : Boolean
    {
        return _sending;
    }


    public function get isConnected() : Boolean
    {
        return (logSocket)? logSocket.connected:false;
    }


///**** State Management
//********************************************************************		



//************************************************************************************		
///**** Socket Management


    /**
     * 	Error modes:
     *
     * 		1:  Connection attempt on non-responsive or non-existant server
     * 			CXMLSocket: openSocket			- attempt to open socket
     * 			CXMLSocket: ioErrorHandler		- connection failure ( ~1  sec delay)
     * 			CXMLSocket: securityErrorHandler- connection failure ( ~20 sec delay)
     *
     * 		2:  Transmission attempt on now non-responsive or non-existant server
     *
     *
     *
     *
     */
    private function socketConnectionHdlr(evt:CLogEvent ) : void
    {
        authMsg:XML;

        if(evt.subType == CLogEvent.SOCKET_OPENED)
        {
            _isConnecting = false;
            _isConnected  = true;

            if(traceMode) trace("############ LogSocket Connected");

            // send STATUS event and allow listeners to handle authentication

            if(hasEventListener(CLogEvent.CONNECT_STATUS))
                dispatchEvent(new CLogEvent(CLogEvent.CONNECT_STATUS, CLogEvent.CONNECTION_OPEN));

            // Note we don't start the stream here - we have to get acknowledgements on authentication
            // and session configuration before the queue stream can run
        }

        //*** otherwise it will be SOCKET_CLOSED or SOCKET_IOERR or SOCKET_SECERR
        // Note that these messages can come from a new socket connection attempt or 
        // a socket that has failed in service - i.e. server shutdown, transimission timeout etc. 

        else
        {
            //!!!! NOTE: This is where we first end up if the network connection fails. 
            //           The socket closes, then there is a write timeout and that dispatches 
            //           a socket_closed event.

            // Move to the interrupted state if the session was currently running
            // We stay in the interrupted state until we successfully reconnect 

            if(_sessionStatus == SESSION_RUNNING)
                _sessionStatus = SESSION_INTERRUPTED;

            // When the socket reports it is closed we make the socket GC'able

            if(!logSocket.connected)
            {
                logSocket.removeEventListener(CLogEvent.CONNECT_STATUS, socketConnectionHdlr);
                logSocket.removeEventListener(DataEvent.DATA,protocolHandlerLGR);

                if(traceMode) trace("############ LogSocket Disconnected - allow GC");
                logSocket = null;

                // Always reset these

                _isConnected    = false;
                _authenticating = false;

                // Set log waiting to kick start the queue when reconnected

                _logWaiting = true;

                if(hasEventListener(CLogEvent.QUEUE_STATUS))
                    dispatchEvent(new CLogEvent(CLogEvent.QUEUE_STATUS, CLogEvent.QUEUE_WAITING));

                // Let listening UI components know the Connection is closed 
                // Consumers of this message make the decision on restarts etc.

                // must be last as it may illicit a reconnect with a newly created logSocket

                if(hasEventListener(CLogEvent.CONNECT_STATUS))
                    dispatchEvent(new CLogEvent(CLogEvent.CONNECT_STATUS, CLogEvent.CONNECTION_CLOSED));
            }

            // Either it succeeded or it failed either way whe're not in the process of connecting any more

            _isConnecting   = false;
        }


        //*************************************************************************
        //** This test is for development mode only -

        if(_sending)
        {
            _sending = false;

            if(hasEventListener(CLogEvent.SEND_STATUS))
                dispatchEvent(new CLogEvent(CLogEvent.SEND_STATUS));
        }

        //** These two flags are for development mode only - 
        //*************************************************************************			
    }


    /**
     * Create and wireup the socket
     *
     */
    private function createSocket() : void
    {
        if(traceMode) trace("@@@@@@@@@@@@@@@@@@@@@@ SOCKET CREATION @@@@@@@@@@@@@@@@@@@@@@@@@@@@@");

        // generate the socket

        logSocket = new CLogSocket(null, 0, tracer);

        // Connect listeners from the socket			

        logSocket.addEventListener(CLogEvent.CONNECT_STATUS, socketConnectionHdlr);
        logSocket.addEventListener(DataEvent.DATA,protocolHandlerLGR);
    }


    /**
     * Close and destroy the socket
     *
     */
    private function cleanupSocket() : void
    {
        if(traceMode) trace("@@@@@@@@@@@@@@@@@@@@@@ SOCKET CLEANUP @@@@@@@@@@@@@@@@@@@@@@@@@@@@@");

        // ensure the Send timer is reset

        resetSendTimer()

        // Disconnect listeners from the socket
        if(logSocket)
        {
            // Ensure socket is closed and collectable - closeSocket manages sockets that are either
            // closed - connected or pending connection - it will maintain listeners until the socket is 
            // good and dead to handle exceptions

            if(logSocket.connected)
            {
                logSocket.closeSocket();
            }
            else
            {
                // Note: If the socket is still connecting it will be managed by the abandon logic in CXMLSocket 

                logSocket.removeEventListener(CLogEvent.CONNECT_STATUS, socketConnectionHdlr);
                logSocket.removeEventListener(DataEvent.DATA,protocolHandlerLGR);
            }
        }

        // Set log waiting to kick start the queue when reconnected

        _logWaiting = true;

        if(hasEventListener(CLogEvent.QUEUE_STATUS))
            dispatchEvent(new CLogEvent(CLogEvent.QUEUE_STATUS, CLogEvent.QUEUE_WAITING));
    }


    /**
     *   This allows you to break down an existing connection and retry it all from scratch
     *
     * 	 It is expected that closing the socket will cause the SessionManager to reattempt the connection
     */
    public function recycleConnection(fRestart:Boolean) : void
    {
        if(traceMode) trace("@@@@@@@@@@@@@@@@@@@@@@ CONNECTION RECYCLING @@@@@@@@@@@@@@@@@@@@@@@@@@@@@");

        if(tracer) tracer.TRACE("Recycling Connection...", "#880000" );

        // Destroy the existing socket
        // Disconnects the listeners prior to close - so UI will not be informed of operation

        cleanupSocket();

        // Resend everything
        if(fRestart)
            _logQueue.restartQueue();
    }


//** Dynamic DNS management

    /**
     * Throw out the loader for GC and cut its wiring 
     *
     */
    private function cleanupDNSLoader() : void
    {
        dnsLoader.removeEventListener(CDnsEvent.COMPLETE, DNSresolved );
        dnsLoader.removeEventListener(CDnsEvent.FAILED, DNSfailed );

        dnsLoader = null;
    }


    /**
     * There are two possible outcomes for the DDNS lookup
     *
     * @param evt
     *
     */
    private function DNSresolved(evt:CDnsEvent ) : void
    {

        if(hasEventListener(CLogEvent.CONNECT_STATUS))
            dispatchEvent(new CLogEvent(CLogEvent.CONNECT_STATUS, CLogEvent.DDNS_RESOLVED));

        // Create the new socket
        //	Wires logSocket - CLogEvent.CONNECT_STATUS to socketConnectionHdlr
        //	Wires logSocket - DataEvent.DATA,protocolHandlerLGR			

        createSocket();

        if(tracer) tracer.TRACE("Connecting: ", '#000088');

        // when the socket connects that will initiate the authentication protocol

        try
        {
            //#### DEBUG - force socket address

            if(fdebugMode)
            {
                _logHostAddress = "127.0.0.1";
                _logHostPort    = CXMLSocket.PORT_LOGGER;
            }

            logSocket.openSocket(_logHostAddress, _logHostPort);

        }
        catch(error:Error)
        {
            trace("catch all" + error);
        }

        // destroy the now unused loader

        cleanupDNSLoader();
    }


    /**
     * Second of two potential outcomes of the DDNS lookup
     *
     * @param evt
     *
     */
    private function DNSfailed(evt:CDnsEvent ) : void
    {
        if(tracer) tracer.TRACE("DDNS failed: ", '#bb0000', evt.dnsData);

        cleanupDNSLoader();

        if(_isConnecting)
        {
            _isConnecting = false;

            if(hasEventListener(CLogEvent.CONNECT_STATUS))
                dispatchEvent(new CLogEvent(CLogEvent.CONNECT_STATUS, CLogEvent.DDNS_FAILED));
        }
    }

//** Dynamic DNS management

//********************************************************************		
///**** Protocol Management - decode responses from service 

    private function protocolHandlerLGR(evt:DataEvent) : void
    {
        servermessage:XML;
        dataPacket:Object;
        seqID:int

        //--------------------------------------------------------------------------------
        // Determine if response if JSON or XML
        if(_logQueue.queueMode == CLogManagerType.MODE_JSON)
        {
            try
            {
                dataPacket = JSON.parse(evt.data);

                if(traceMode) trace("@@@@@@@  JSON PROTOCOL RCV Event:" + dataPacket.command + dataPacket.seqid);

                switch(dataPacket.command)
                {
                    //** Logging Protocol Management
                    //

                    case CMongo.ACKLOG_PACKET:
                    case CMongo.ACKLOG_PROGRESS:

                        // If the last packet was NOT correct then we have a problem 
                        // The server is requesting something out of sequence 
                        // Resend the whole thing

                        if(traceMode) trace("@@@@@@@  JSON LOG PACKET ACKNOWLEDGED:");

                        // We got the ack in time - don't allow the timer to expire

                        resetSendTimer();

                        // Check data sequencing

                        if(!_logQueue.ackPacket(dataPacket.seqid ))
                        {
                            //@@ TODO: This should flush and truncate the log files on the server   

                            //recycleConnection(false);								
                            break;
                        }

                        // If the stream is open then send the next packet.

                        if(_QueStreaming)
                        {
                            // If there is anything buffered send it
                            // Queue stream is kept flowing here -

                            if(!_logQueue.isQueueEmpty())
                            {
                                sendJSONPacket(_logQueue.nextPacket());

                                if(hasEventListener(CLogEvent.QUEUE_STATUS))
                                    dispatchEvent(new CLogEvent(CLogEvent.QUEUE_STATUS, CLogEvent.QUEUE_OPENED));
                            }

                            // Otherwise wait for the next packet to arrive
                            // queueChanged will kick start the stream again

                            else
                            {
                                _logWaiting = true;

                                if(hasEventListener(CLogEvent.QUEUE_STATUS))
                                    dispatchEvent(new CLogEvent(CLogEvent.QUEUE_STATUS, CLogEvent.QUEUE_WAITING));
                            }
                        }
                        else
                        {
                            // Note that if a packet is acknowledged we always want to at least set the logwaiting flag 
                            // so the stream can restart on a queue change event.

                            _logWaiting = true;

                            if(hasEventListener(CLogEvent.QUEUE_STATUS))
                                dispatchEvent(new CLogEvent(CLogEvent.QUEUE_STATUS, CLogEvent.QUEUE_WAITING));
                        }
                        break;

                    case CMongo.ACKLOG_NAK:		// Resend packet

                        // We got the ack in time - don't allow the timer to expire

                        resetSendTimer();

                        //*** Note - We don't call ackPacket here which would increment the queue pointer
                        //           Therefore when we call _logQueue.nextPacket() we get the same packet again to resend.							

                        // If there is anything buffered send it
                        // Queue stream is kept flowing here -


                        packet:* = _logQueue.nextPacket();

                        if(packet != null)
                        {
                            sendJSONPacket(packet);
                        }
                        else
                        {

                        }
                        break;

                    case CMongo.ACKLOG_TERMINATE:

                        // We got the ack in time - don't allow the timer to expire

                        resetSendTimer();

                        if(tracer) tracer.TRACE("*");
                        if(tracer) tracer.TRACE("@@term@@\n");
                        if(traceMode) trace("@@@@@@@@@@@@@@@@@@@@@@ CMongo.ACKLOG_TERMINATE @@@@@@@@@@@@@@@@@@@@@@@@@@@@@");

                        // Abandon the session - 
                        // This resets the queue and closes the socket 
                        // also resets all session flags

                        abandonSession(false, SESSION_COMPLETE);

                        // Let everyone know what has happened.

                        if(hasEventListener(CLogEvent.SESSION_STATUS))
                            dispatchEvent(new CLogEvent(CLogEvent.SESSION_STATUS, CLogEvent.SESSION_FLUSHED ));
                        break;

                    default:
                        // Handle administrative events externally						
                        // We got the ack in time - don't allow the timer to expire

                        resetSendTimer();

                        dispatchEvent(new CLogEvent(CLogEvent.PACKET_FORWARD, CLogEvent.PACKET_DATA, 0, 0, dataPacket));
                        break;
                }

            }
            catch(err:Error)
            {
                trace("protocolHandlerLGR - Message Format Error: " + err.toString());

                _authenticating = false;

                if(hasEventListener(CLogEvent.SESSION_STATUS))
                    dispatchEvent(new CLogEvent(CLogEvent.SESSION_STATUS, CLogEvent.AUTH_FAILED ));
            }
        }

        //--------------------------------------------------------------------------------
        // process XML response			
        else
        {
            servermessage = new XML(evt.data);

            if(traceMode) trace("Logger Responded: " + servermessage.name() + "\n\nFull Packet: \n" + servermessage);

            if(servermessage.name() == CXMLSocket.xmlSERVER_MESSAGE)
            {
                for each (msgClass:XML in servermessage.children())
                {
                    // note: must convert to string - object can't be used in switch			
                    //
                    switch(msgClass.name().toString())
                    {
                        //** Authentication Protocol Management
                        //

                        case CXMLSocket.xmlACKAUTH:
                            if(traceMode) trace("Authentication success: " + msgClass.@type );
                            if(tracer) tracer.TRACE("Authentication Successful...", "#000088" );

                            // Authentication succeeded - return userID in @type => db index 

                            _authenticating = false;

                            // Set session flag - we are now in an active session.
                            // Set the Session ID - used for reconnection if required							

                            _sessionActive = true;
                            _sessionStatus = SESSION_RUNNING;
                            _sessionID     = msgClass.@type;

                            if(hasEventListener(CLogEvent.SESSION_STATUS))
                                dispatchEvent(new CLogEvent(CLogEvent.SESSION_STATUS, CLogEvent.AUTH_SUCCESS, 0, 0, msgClass.@type));
                            break;

                        case CXMLSocket.xmlNAKAUTH:
                            if(traceMode) trace("Authentication failed: " + msgClass.@type );
                            if(tracer) tracer.TRACE("Authentication Failed...", "#880000" );

                            _authenticating = false;

                            if(hasEventListener(CLogEvent.SESSION_STATUS))
                                dispatchEvent(new CLogEvent(CLogEvent.SESSION_STATUS, CLogEvent.AUTH_FAILED ));
                            break;

                        case CXMLSocket.xmlSQLERROR:
                            if(traceMode) trace("Server failure: " + msgClass.@type + " " + msgClass.@message );
                            if(tracer) tracer.TRACE("Authentication - SQL Failed...", "#880000" );

                            _authenticating = false;

                            if(hasEventListener(CLogEvent.SESSION_STATUS))
                                dispatchEvent(new CLogEvent(CLogEvent.SESSION_STATUS, CLogEvent.AUTH_FAILED ));
                            break;

                        //
                        //** Authentication Protocol Management

                        //** Reattach Protocol Management
                        //
                        //  The reattach protocol reconnects the session to the DB or to the session files on the server
                        //  if the session files are not present then the reattach may fail in which case it is neccesary 
                        //  to create an entirely new session.
                        //

                        case CXMLSocket.xmlACKATTACH:

                            if(traceMode) trace("@@@@@@@  SESSION REATTACH ACK: ");

                            // Authentication succeeded - return userID in @type => db index 

                            _authenticating = false;

                            // Get the session ID from the server reponse
                            // transition back to the running state

                            _sessionID     = msgClass.@type;
                            _sessionStatus = SESSION_RUNNING;

                            // A leading # indicates there has been a file error at the server.
                            // i.e. the user files are no longer extant
                            // This requires creation of a new session and recommit of all data 

                            if(sessionID.charAt(0) == '#')
                            {
                                if(tracer) tracer.TRACE("Reauthentication Failed...", "#000088" );

                                if(hasEventListener(CLogEvent.SESSION_STATUS))
                                    dispatchEvent(new CLogEvent(CLogEvent.SESSION_STATUS, CLogEvent.AUTH_FAILED ));
                            }
                            else
                            {
                                if(tracer) tracer.TRACE("Reauthentication Successful...", "#000088" );

                                // Let anyone listening know that the connection is live again. e.g. UI components

                                if(hasEventListener(CLogEvent.SESSION_STATUS))
                                    dispatchEvent(new CLogEvent(CLogEvent.SESSION_STATUS, CLogEvent.SESSION_RESTARTED, 0, 0, msgClass.@type));
                            }
                            break;

                        //
                        //** Reattach Protocol Management


                        //** NOOP Protocol Management
                        //

                        case CXMLSocket.xmlACK:

                            if(traceMode) trace("@@@@@@@  SIMPLE PACKET ACK: ");
                            break;

                        //
                        //** NOOP Protocol Management


                        //** Session Protocol Management
                        //

                        case CXMLSocket.xmlACKSESSION:

                            if(traceMode) trace("@@@@@@@  SESSION PACKET ACK: ");
                            if(tracer) tracer.TRACE("*");

                            // Get the session ID from the server reponse

                            _sessionID = msgClass.@type;

                            // A leading # indicates there has been a file or registration formatting 
                            // error at the server.
                            //
                            // This is most likely an unrecoverable state from the client end. 
                            // i.e. It will require action at the server end to resolve 

                            if(_sessionID.charAt(0) == '#')
                            {
                                // disconnect logger
                                // Disconnects the listeners prior to close - so UI will not be informed of operation
                                // 
                                cleanupSocket();

                                if(hasEventListener(CLogEvent.SERVER_FAILED))
                                    dispatchEvent(new CLogEvent(CLogEvent.SERVER_FAILED, _sessionID));
                            }
                            break;



                        case CXMLSocket.xmlACKTERM:

                            if(tracer) tracer.TRACE("*");
                            if(tracer) tracer.TRACE("@@term@@\n");

                            // Abandon the session - 
                            // This resets the queue and closes the socket 
                            // also resets all session flags

                            abandonSession(false);

                            // Let everyone know what has happened.

                            if(hasEventListener(CLogEvent.SESSION_STATUS))
                                dispatchEvent(new CLogEvent(CLogEvent.SESSION_STATUS, CLogEvent.SESSION_FLUSHED ));
                            break;

                        //
                        //** Session Protocol Management


                        //** Logging Protocol Management
                        //

                        case CXMLSocket.xmlACKLOG:

                            if(tracer) tracer.TRACE("*");

                            seqID = msgClass.@type;

                            // If the last packet was NOT correct then we have a problem 
                            // The server is requesting something out of sequence 
                            // Resend the whole thing

                            if(!_logQueue.ackPacket(seqID ))
                            {
                                //@@ TODO: This should flush and truncate the log files on the server   

                                //recycleConnection(false);								
                                break;
                            }

                            // If the stream is open then send the next packet.

                            if(_QueStreaming)
                            {
                                // If there is anything buffered send it
                                // Queue stream is kept flowing here -

                                if(!_logQueue.isQueueEmpty())
                                {
                                    if(sendXMLPacket(_logQueue.nextPacket()))
                                    {
                                        if(tracer) tracer.TRACE("#");
                                    }

                                    if(hasEventListener(CLogEvent.QUEUE_STATUS))
                                        dispatchEvent(new CLogEvent(CLogEvent.QUEUE_STATUS, CLogEvent.QUEUE_OPENED));
                                }

                                // Otherwise wait for the next packet to arrive
                                // queueChanged will kick start the stream again

                                else
                                {
                                    _logWaiting = true;

                                    if(hasEventListener(CLogEvent.QUEUE_STATUS))
                                        dispatchEvent(new CLogEvent(CLogEvent.QUEUE_STATUS, CLogEvent.QUEUE_WAITING));
                                }
                            }

                            break;

                        //
                        //** Logging Protocol Management


                        // anything else is a protocol failure

                        default:

                            if(traceMode) trace("Protocol Error");

                            break;
                    }
                }
            }
        }
    }


    /**
     *
     */
    public function activateSession(sessionID:String = null) : void
    {
        if(traceMode) trace("Authentication success: " + sessionID );
        if(tracer) tracer.TRACE("Authentication Successful...", "#000088" );

        // Authentication succeeded - return userID in @type => db index 

        _authenticating = false;

        // Set session flag - we are now in an active session.
        // Set the Session ID - used for reconnection if required							

        _sessionActive = true;
        _sessionStatus = SESSION_RUNNING;

        if(sessionID != null)
            _sessionID = sessionID;
    }


    /**
     *
     */
    public function failSession() : void
    {
        if(traceMode) trace("Authentication failed: " );
        if(tracer) tracer.TRACE("Authentication Failed...", "#880000" );

        _authenticating = false;
    }



//**** Protocol Management - decode responses from service 
//********************************************************************		


//*******************************************************************************		
//************************** PLAYBACK		

    // Set where the playback data comes from
    //
    public function setPlayBackSource(logSource:XMLList ) : void
    {
        // null logSource plays back from the live log
        // note: the live log is wrapped in a TutorShop clientmessage wrapper

        if(logSource == null)
        {
            LogSource   = "logCache";
            //	xmlEvents   = logEvents.clientmessage;
            playBackSiz = _logQueue.length;
        }
        else
        {
            LogSource   = "xmlSource";
            xmlEvents   = logSource;
            playBackSiz = logSource.length();

            if(traceMode) trace("playBackSiz: " + playBackSiz);
        }

        // Init playback counters
        //
        fPlayBackDone		 = false;
        playBackNdx          = 0;
//			CWOZDoc.gApp.frameID = 0;
//			CWOZDoc.gApp.stateID = 0;

        //@@ init legacy playback counters

        lastAction = -1;
        lastMove   = 0;
    }


    /**
     *  Remove the <clientmessage> wrapper from the log
     *
     */
    public function unWrapLog() : XMLList
    {
        unWrapped:XML = <unwrapped/>;

        for(i1:int = 0 ; i1 <  _logQueue.length ; i1++)
        {
            //unWrapped.appendChild(logEvents.children()[i1].logrecord[0]);
        }

        return unWrapped.children();
    }


    /**
     *  Preprocess the source recording to normalize the times 
     *  Note: Legacy playback normalization
     *
     */
    public function normalizePlayBackTime() : void
    {
        nBaseTime:Number;
        nEvent:int;

        nBaseTime = xmlEvents[0].@time;

        // If the recording has not already been normalized then process it
        //
        if(nBaseTime != 0)
        {
            for(nEvent = 0 ; nEvent < playBackSiz ; nEvent++)
            {
                xmlEvents[nEvent].@time -= nBaseTime;
                xmlEvents[nEvent].@time *= 1000;
            }
        }
    }


    /**
     *  Preprocess the source recording to normalize the times 
     *
     */
    public function normalizePlayBack() : void
    {
        xmlEvent:XML;
        nBaseTime:Number;
        nBaseState:int;
        nBaseFrame:int;
        nEvent:int;

        // This is playing a event inside a TutorShop wrapper

        xmlEvent = xmlEvents[0];					// for live log use - logEvents.children().logrecord[0]

        nBaseTime  = xmlEvent.@time;
        nBaseState = xmlEvent.@stateID;
        nBaseFrame = xmlEvent.@frameID;

        // If the recording has not already been normalized then process it

        if(nBaseTime != 0)
        {
            for(nEvent = 0 ; nEvent < playBackSiz ; nEvent++)
            {
                xmlEvent = xmlEvents[nEvent];		// for live log use - logEvents.children()[nEvent].logrecord[0]

                xmlEvent.@time    -= nBaseTime;
                xmlEvent.@stateID -= nBaseState;
                xmlEvent.@frameID -= nBaseFrame;
            }
        }
    }


    /**
     *  return the state associated with the next event to be fired
     *
     */
    public function getNextEventState() : int
    {
        xmlEvent:XML;

        xmlEvent = xmlEvents[playBackNdx]; 			// logEvents.children()[playBackNdx].logrecord[0];

        return xmlEvent.@stateID;
    }


    /**
     *  return the next event between lastEvent and the new Frame Time. 
     *
     */
    public function getNextEvent(stateID:int, frameID:int) : XML
    {
        xmlEvent:XML;
        xResult:XML = null;

        if(traceMode) trace("getEvent for State: " + stateID + " : Frame : " + frameID);

        for( ; playBackNdx < playBackSiz ; playBackNdx++)
        {
            xmlEvent = xmlEvents[playBackNdx]; 			// logEvents.children()[playBackNdx].logrecord[0];

            // We only return WOZEvents
            //
            if(xmlEvent.@type != "WOZevent")
            continue;

            // If the state of the interface gets ahead - fire events until we catch up
            // otherwise just fire all the events for the current frame
            //
            // note: States are unordered (i.e. not unique) therefore a specific state
            //       in the playback may not be identical to one in the live version.
            //
            if(xmlEvent.@frameID == frameID)
            {
                // parse mouse events

                if(xmlEvent.CWOZMouseEvent != undefined)
                {
                    xResult = xmlEvent;
                    playBackNdx++;
                    break;
                }

                // parse keyboard events

                else if(xmlEvent.CWOZTextEvent != undefined)
                {
                    xResult = xmlEvent;
                    playBackNdx++;
                    break;
                }
            }

            // otherwise wait until the frame catches up
					
				else break;
        }

        // Set flag if we reach the end of the log

        if(playBackNdx >= playBackSiz)
            fPlayBackDone = true;

        return xResult;
    }


    /**
     *  Query if playback is finished
     *
     */
    public function playBackDone() : Boolean
    {
        return fPlayBackDone;
    }



//@@@@@@@@@@@@@@@@@@@ Legacy Playback

    /**
     *  return the next action event between lastAction and the new Frame Time. 
     *
     */
    public function getActionEvent(frameTime:Number) : XML
    {
        xResult:XML = null;
        nAction:int;

        if(traceMode) trace("getActionEvent: " + frameTime);

        for(nAction = lastAction + 1 ; nAction < playBackSiz ; nAction++)
        {
            // We only return WOZEvents
            //
            if(xmlEvents[nAction].@type != "WOZevent")
            continue;
					
				else if(xmlEvents[nAction].CWOZMouseEvent != undefined)
        {
            if(xmlEvents[nAction].@time <= frameTime)
            {
                if(xmlEvents[nAction].CWOZMouseEvent.CWOZEvent.@type != "WOZMOUSE_MOVE")
                {
                    xResult = xmlEvents[nAction];
                    break;
                }
            }
					else break;
        }
        else if(xmlEvents[nAction].CWOZTextEvent != undefined)
        {
            if(xmlEvents[nAction].@time <= frameTime)
            {
                xResult = xmlEvents[nAction];
                break;
            }
					else break;
        }
        }

        // if either the move or actions are finished then we are done.
        //
        if(nAction >= playBackSiz)
            fPlayBackDone = true;

        // Track last action done
        if(xResult != null)
            lastAction = nAction;

        return xResult;
    }

    /**
     * Support for aborting playback
     */
    public function setPlayBackDone(val:Boolean) : void
    {
        fPlayBackDone = val;
    }


    /**
     *  Find the first Playback move event at or beyond the current frame time
     *
     *  This is used to interpolate the Playback position at Frame Time
     */
    public function getMoveEvent(frameTime:Number) : XML
    {
        xResult:XML = null;
        nMove:int;

        for(nMove = lastMove ; nMove < playBackSiz ; nMove++)
        {
            // We only return WOZEvents
            //
            if(xmlEvents[nMove].@type != "WOZevent")
            continue;

            if(xmlEvents[nMove].@time >= frameTime)
            {
                if(xmlEvents[nMove].CWOZMouseEvent.CWOZEvent.@type == "WOZMOUSE_MOVE")
                {
                    xResult = xmlEvents[nMove];
                    break;
                }
            }
        }

        // if either the move or actions are finished then we are done.
        //
        if(nMove >= playBackSiz)
            fPlayBackDone = true;

        // Track last move done
        lastMove = nMove;

        return xResult;
    }


//@@@@@@@@@@@@@@@@@@@ Legacy Playback


//************************** PLAYBACK
//*******************************************************************************		

}
}
