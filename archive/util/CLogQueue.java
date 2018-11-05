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

import java.util.Vector;

/**
 * Created by kevin on 3/22/2018.
 */

public class CLogQueue {

    protected Boolean traceMode = true;
    protected Boolean logTrace  = false;

    private int logEvtIndex = -1;					// Whats available to be logged
    private int logAckIndex = -1;					// Whats been acknowledged by logger

    private XML logEvents;	   									// XML Event stream
    private Vector<Object> jsonEvents = new Vector<Object>();	// JSON Event stream

    // Playback counters

    private String LogSource;						// Playback can come from either Recorded Events (cached playback) or and XML object (Logged playback)
    private XMLList xmlEvents;						// XML Object holding a recording to be played back

    private int lastAction;							//@@Legacy playback
    private int lastMove;							//@@Legacy playback

    private Boolean fPlayBackDone;					// set when playBackNdx reaches playBackSiz
    private int playBackNdx;						// replay progress counter
    private int playBackSiz;						// size of the current playback object

    // logging flag and bitmapped constants

    private Boolean _queueOpen = false;				// Whether queue acepts data
    private Boolean _queueStreaming = false;		// Whether data is streaming over the wire

    private String _queueMode = CLogManagerType.MODE_JSON;

    public static final int RECLOGNONE    = 0;			// Disable all recording
    public static final int RECORDEVENTS  = 1;			// Record Events
    public static final int LOGEVENTS     = 2;			// Log Events to Server
    public static final int RECLOGEVENTS  = 3;			// Record and Log all events



    public void CLogQueue()
    {
        traceMode = false;

        if(traceMode) trace("CLogQueue:Constructor");

        resetQueue();
    }


//**********************************************************		
//*** Queue state Management


    public String getqueueMode()
    {
        return _queueMode;
    }

    public Boolean  getisStreaming()
    {
        return _queueStreaming;
    }

    public int getlength()
    {
        return logEvtIndex;
    }

    public int getPosition()
    {
        return logAckIndex;
    }

    /**
     *  Open the Queue to accept new data
     */
    public void openQueue()
    {
        _queueOpen = true;

        if(hasEventListener(CLogEvent.QUEUE_STATUS))
            dispatchEvent(new CLogEvent(CLogEvent.QUEUE_STATUS, CLogEvent.QUEUE_OPENED));
    }


    /**
     * Close the queue to new entries - existing data will be flushed when streaming 
     *
     */
    public void closeQueue() : void
    {
        _queueOpen = false;

        if(hasEventListener(CLogEvent.QUEUE_STATUS))
            dispatchEvent(new CLogEvent(CLogEvent.QUEUE_STATUS, CLogEvent.QUEUE_CLOSED));
    }


    public void startQueueStream() : void
    {
        _queueStreaming = true;

        // Send this to kick start the stream if there is any data

        if(hasEventListener(CLogEvent.QUEUE_CHANGED))
            dispatchEvent(new CLogEvent(CLogEvent.QUEUE_CHANGED));

        // General informational message

        if(hasEventListener(CLogEvent.QUEUE_STATUS))
            dispatchEvent(new CLogEvent(CLogEvent.QUEUE_STATUS, CLogEvent.STREAM_OPENED));
    }


    /**
     * Close the queue to new entries - existing data will be flushed 
     *
     */
    public void stopQueueStream() : void
    {
        _queueStreaming = false;

        // General informational message

        if(hasEventListener(CLogEvent.QUEUE_STATUS))
            dispatchEvent(new CLogEvent(CLogEvent.QUEUE_STATUS, CLogEvent.STREAM_CLOSED));
    }


    public void resetQueue() : void
    {
        // Reset Queue

        logEvtIndex = -1;
        logAckIndex = -1;

        playBackSiz = 0;								// size of the current playback object			

        logEvents   = <eventlog/>;					   	// Loggable events
        jsonEvents  = new Vector<Object>();				//

        // Reset Playback 

        LogSource = "";									// Playback can come from either Recorded Events (cached playback) or and XML object (Logged playback) 
        xmlEvents = null;								// XML Object holding a recording to be played back

        lastAction    = -1;								//@@Legacy playback
        lastMove      = -1;								//@@Legacy playback

        fPlayBackDone = false;							// set when playBackNdx reaches playBackSiz
        playBackNdx   = -1;								// replay progress counter
        playBackSiz   = -1;								// size of the current playback object

        if(hasEventListener(CLogEvent.QUEUE_STATUS))
            dispatchEvent(new CLogEvent(CLogEvent.QUEUE_STATUS, CLogEvent.QUEUE_RESET));
    }


    public void restartQueue() : void
    {
        // Resend everything
        //
        logAckIndex = -1;

        if(hasEventListener(CLogEvent.QUEUE_CHANGED))
            dispatchEvent(new CLogEvent(CLogEvent.QUEUE_CHANGED));
    }


    public Boolean isQueueEmpty() 
    {
        Boolean fEmpty;

        // Flush the log as required
        //
        if(logAckIndex != logEvtIndex)
            fEmpty = false;
        else
            fEmpty = true;

        return fEmpty;
    }


    /**
     * Return the next packet to be sent
     *
     * @return
     *
     */
    public Object nextPacket() : *
    {
        if(_queueMode == CLogManagerType.MODE_JSON)
            return jsonEvents[logAckIndex+1];
        else
            return logEvents.children()[logAckIndex+1];
    }


    /**
     * Return the Index of the next packet to be sent
     *
     * @return
     *
     */
    public int get nextNdx() : int
    {
        return logEvtIndex+1;
    }


//*** Queue state Management
//**********************************************************		

//**********************************************************		
//*** Queue data Management

    /**
     * Add the event to the XML recording and send to server if requested.
     *
     * Note that all protocol packets get lumped into the recording buffer.
     * So if you want to resend a specific protocol you would have to create a 
     * filter function to only send those packets.
     *
     */
    public void logEvent(dataEvt:* ) : void
    {
        // After the log is closed it will not accept anymore events

        if(_queueOpen)
        {
            // increment the log index in either case

            logEvtIndex++;

            // If Queueing - Enqueue the event

            if(_queueMode == CLogManagerType.MODE_JSON)
                jsonEvents.push(dataEvt);
            else
                logEvents.appendChild(dataEvt);

            // Emit progress message when we add as well as when we remove from the queue
            // This allows progress updates when the stream is stalled due to connection 
            // problems.

            emitProgress();

            // If Logging - emit change event so LogManager can restart stream if required

            if(_queueStreaming)
            {
                if(hasEventListener(CLogEvent.QUEUE_CHANGED))
                    dispatchEvent(new CLogEvent(CLogEvent.QUEUE_CHANGED));
            }
        }
    }


    /**
     * Check if ack'd packet is for the sent packet
     * If so move to the next packet  
     *
     * @param seqID
     * @return Boolean - packet acknowledged in sequence 
     *
     */
    public Boolean ackPacket(int seqID:int, Boolean reSend = false )
    {
        var fResult = false;

        // If the last packet was correct then we send the next one
        //
        if(seqID == logAckIndex+1)
        {
            if(traceMode) trace("@@@@@@@  PACKET ACK: " + (logAckIndex+1));

            // logAckIndex reflects the last packet successfully sent

            if(!reSend)
            {
                logAckIndex++;

                emitProgress();
            }
            fResult = true;
        }

        return fResult;
    }

    /**
     *  public function so we can query the queue for its status
     */
    public void emitProgress() : void
    {
        if(hasEventListener(CLogEvent.PROG_MSG))
            dispatchEvent(new CLogEvent(CLogEvent.PROG_MSG, null, logAckIndex, logEvtIndex));
    }


//*** Queue data Management
//**********************************************************		


//************************************************************************		
//************************** PLAYBACK SUPPORT		


    // Set where the playback data comes from
    //
    public void setPlayBackSource(logSource:XMLList ) : void
    {
        // null logSource plays back from the live log
        // note: the live log is wrapped in a TutorShop clientmessage wrapper

        if(logSource == null)
        {
            LogSource   = "logCache";
            xmlEvents   = logEvents.clientmessage;
            playBackSiz = logEvtIndex;
        }
        else
        {
            LogSource   = "xmlSource";
            xmlEvents   = logSource;
            playBackSiz = logSource.length();

            if(logTrace) trace("playBackSiz: " + playBackSiz);
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
    public XMLList unWrapLog() : XMLList
    {
        var unWrapped:XML = <unwrapped/>;

        for(var i1:int = 0 ; i1 < logEvtIndex ; i1++)
        {
            unWrapped.appendChild(logEvents.children()[i1].logrecord[0]);
        }

        return unWrapped.children();
    }


    /**
     *  Preprocess the source recording to normalize the times 
     *  Note: Legacy playback normalization
     *
     */
    public void normalizePlayBackTime() : void
    {
        var nBaseTime:Number;
        var nEvent:int;

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
    public void normalizePlayBack() : void
    {
        var xmlEvent:XML;
        var nBaseTime:Number;
        var nBaseState:int;
        var nBaseFrame:int;
        var nEvent:int;

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
    public int getNextEventState() : int
    {
        var xmlEvent:XML;

        xmlEvent = xmlEvents[playBackNdx]; 			// logEvents.children()[playBackNdx].logrecord[0];

        return xmlEvent.@stateID;
    }


    /**
     *  return the next event between lastEvent and the new Frame Time. 
     *
     */
    public XML getNextEvent(stateID:int, frameID:int) : XML
    {
        var xmlEvent:XML;
        var xResult:XML = null;

        if(logTrace) trace("getEvent for State: " + stateID + " : Frame : " + frameID);

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
    public Boolean playBackDone() 
    {
        return fPlayBackDone;
    }


//@@@@@@@@@@@@@@@@@@@ Legacy Playback

    /**
     *  return the next action event between lastAction and the new Frame Time. 
     *
     */
    public XML getActionEvent(frameTime:Number) : XML
    {
        var xResult:XML = null;
        var nAction:int;

        if(logTrace) trace("getActionEvent: " + frameTime);

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
    public void setPlayBackDone(Boolean val) : void
    {
        fPlayBackDone = val;
    }


    /**
     *  Find the first Playback move event at or beyond the current frame time
     *
     *  This is used to interpolate the Playback position at Frame Time
     */
    public XML getMoveEvent(frameTime:Number) : XML
    {
        var xResult:XML = null;
        var nMove:int;

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
//************************************************************************		


//***************** Logging API *******************************		

}
    
}
