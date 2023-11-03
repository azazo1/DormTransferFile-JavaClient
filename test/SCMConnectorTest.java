import com.azazo1.dormtransferfile.MsgType;
import com.azazo1.dormtransferfile.SCMConnector;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class SCMConnectorTest {

    @Test
    void fetchAvailableSenders() throws IOException {
        SCMConnector scm = new SCMConnector();

        int codeFetching = scm.fetchAvailableSenders();
        int responseCode = scm.readResponseCode();
        assertEquals(codeFetching, responseCode);
        var fetchContent = MsgType.FetchAvailableSenders.parseMsg(scm.in);
        for (var key : fetchContent.keySet()) {
            System.out.println(key + ":" + fetchContent.get(key));
        }
        scm.close();
    }

    @Test
    void registerSender() throws IOException {
        SCMConnector scm = new SCMConnector();

        int codeFetching = scm.fetchAvailableSenders();
        int responseCode = scm.readResponseCode();
        int msgTypeCode = scm.readMsgTypeCode();
        assertEquals(codeFetching, responseCode);
        var fetchContent = MsgType.FetchAvailableSenders.parseMsg(scm.in);
        for (var key : fetchContent.keySet()) {
            System.out.println(key + ":" + fetchContent.get(key));
        }
        System.out.println("-----------------------------------------------------");

        int codeRegistering = scm.registerSender("hello", 12344);
        responseCode = scm.readResponseCode();
        msgTypeCode = scm.readMsgTypeCode();
        assertEquals(codeRegistering, responseCode);
        var registerContent = MsgType.RegisterSender.parseMsg(scm.in);
        System.out.println(registerContent);
        System.out.println("-----------------------------------------------------");

        codeFetching = scm.fetchAvailableSenders();
        responseCode = scm.readResponseCode();
        msgTypeCode = scm.readMsgTypeCode();
        assertEquals(codeFetching, responseCode);
        fetchContent = MsgType.FetchAvailableSenders.parseMsg(scm.in);
        for (var key : fetchContent.keySet()) {
            System.out.println(key + ":" + fetchContent.get(key));
        }
        System.out.println("-----------------------------------------------------");
        scm.close();
    }

    @Test
    void querySenderServerAddress() throws IOException {
        SCMConnector scm = new SCMConnector();
        SCMConnector scmSenderServer = new SCMConnector();
        int port = 12331;
        String filename = "Hello.py";
        int code = scmSenderServer.registerSender(filename, port);
        int respondCode = scmSenderServer.readResponseCode();
        assertEquals(code, respondCode);
        int msgTypeCode = scmSenderServer.readMsgTypeCode();
        int connCode = MsgType.RegisterSender.parseMsg(scmSenderServer.in);
        System.out.println("-----------------------------------------------------");

        code = scm.fetchAvailableSenders();
        respondCode = scm.readResponseCode();
        assertEquals(code, respondCode);
        msgTypeCode = scm.readMsgTypeCode();
        var fetchRst = MsgType.FetchAvailableSenders.parseMsg(scm.in);
        int connCodeReceived = fetchRst.keySet().stream().toList().get(0);
        assertEquals(connCodeReceived, connCode);
        System.out.println("-----------------------------------------------------");

        code = scm.querySenderServerAddress(connCodeReceived);
        respondCode = scm.readResponseCode();
        assertEquals(respondCode, code);
        msgTypeCode = scm.readMsgTypeCode();
        var address = MsgType.QuerySenderServerAddress.parseMsg(scm.in);
        if (address != null) {
            assertEquals(address.second, port);
            System.out.println(address.first + " " + address.second);
        } else {
            fail();
        }
        System.out.println("-----------------------------------------------------");

    }
}