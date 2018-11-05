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

/**
 * Created by kevin on 3/22/2018.
 */

public class CLogManagerType {

    public static final int RECLOGNONE    = 0;			// Disable all recording
    public static final int RECORDEVENTS  = 1;			// Record Events
    public static final int LOGEVENTS     = 2;			// Log Events to Server
    public static final int RECLOGEVENTS  = 3;			// Record and Log all events

    public static final String MODE_JSON    = "MODE_JSON";

    public static final String JSON_ACKLOG  = "JSON_ACKLOG";
    public static final String JSON_ACKTERM = "JSON_ACKTERM";

    public CLogManagerType()
    {
    }
}
