package sysrqb.ortt;

import org.junit.Test;

import java.util.Hashtable;

import static org.junit.Assert.*;

public class TorControllerTest extends TorController {
    @Test
    public void authenticateSuccessful_Test() throws Exception {
        byte[] res = "".getBytes();
        assertEquals(false, authenticateSuccessful(res.length, res));
        res = "100".getBytes();
        assertEquals(false, authenticateSuccessful(res.length, res));
        res = "515".getBytes();
        assertEquals(false, authenticateSuccessful(res.length, res));
        res = "515 Bad Authentication".getBytes();
        assertEquals(false, authenticateSuccessful(res.length, res));
        res = "250 OK".getBytes();
        assertEquals(true, authenticateSuccessful(res.length, res));
        res = "250  OK".getBytes();
        assertEquals(false, authenticateSuccessful(res.length, res));
        res = "250 OK\r\n".getBytes();
        assertEquals(true, authenticateSuccessful(res.length, res));
        res = "250 OK BAD".getBytes();
        assertEquals(true, authenticateSuccessful(res.length, res));
    }

    @Test
    public void parseProtocolInfoResponse_Test() throws Exception {
        Hashtable<String, String> expectedResult;
        Hashtable<String, String> ret;
        String result = null;
        assertEquals(null, parseProtocolInfoResponse(result));
        result = "";
        assertEquals(null, parseProtocolInfoResponse(result));

        result = "\r\n";
        assertEquals(null, parseProtocolInfoResponse(result));

        result = " \r\n";
        assertEquals(null, parseProtocolInfoResponse(result));

        result = " 250-PROTOCOLINFO";
        assertEquals(null, parseProtocolInfoResponse(result));

        /* TODO
           The following tests do not conform to the spec, because all
           responses should end with "250 OK".
         */
        expectedResult = new Hashtable<>();
        expectedResult.put("PROTOCOLINFO", "");
        result = "250-PROTOCOLINFO";
        ret = parseProtocolInfoResponse(result);
        assertNotEquals(null, ret);
        assertEquals(expectedResult, ret);

        expectedResult = new Hashtable<>();
        expectedResult.put("PROTOCOLINFO", "");
        result = "250-PROTOCOLINFO\r\n";
        ret = parseProtocolInfoResponse(result);
        assertNotEquals(null, ret);
        assertEquals(expectedResult, ret);

        result = "250-PROTOCOLINFO\r\n 250-AUTH ";
        ret = parseProtocolInfoResponse(result);
        assertEquals(null, ret);

        expectedResult = new Hashtable<>();
        expectedResult.put("PROTOCOLINFO", "");
        /* Strictly this does not follow the spec */
        expectedResult.put("AUTH", "");
        result = "250-PROTOCOLINFO\r\n250-AUTH";
        ret = parseProtocolInfoResponse(result);
        assertNotEquals(null, ret);
        assertEquals(expectedResult, ret);

        expectedResult = new Hashtable<>();
        expectedResult.put("PROTOCOLINFO", "");
        /* Strictly this does not follow the spec */
        expectedResult.put("AUTH", "METHOD=NULL");
        result = "250-PROTOCOLINFO\r\n250-AUTH METHOD=NULL";
        ret = parseProtocolInfoResponse(result);
        assertNotEquals(null, ret);
        assertEquals(expectedResult, ret);

        expectedResult = new Hashtable<>();
        expectedResult.put("PROTOCOLINFO", "");
        /* Strictly this does not follow the spec */
        expectedResult.put("AUTH", "METHOD=NULL C");
        result = "250-PROTOCOLINFO\r\n250-AUTH METHOD=NULL C";
        ret = parseProtocolInfoResponse(result);
        assertNotEquals(null, ret);
        assertEquals(expectedResult, ret);

        expectedResult = new Hashtable<>();
        expectedResult.put("PROTOCOLINFO", "");
        /* Strictly this does not follow the spec */
        expectedResult.put("AUTH", "METHOD=NULL C");
        result = "250-PROTOCOLINFO\r\n250-AUTH METHOD=NULL C\r\n";
        ret = parseProtocolInfoResponse(result);
        assertNotEquals(null, ret);
        assertEquals(expectedResult, ret);

        expectedResult = new Hashtable<>();
        expectedResult.put("PROTOCOLINFO", "");
        /* Strictly this does not follow the spec */
        expectedResult.put("AUTH", "METHOD=NULL C");
        result = "250-PROTOCOLINFO\r\n250-AUTH METHOD=NULL C\r\n";
        ret = parseProtocolInfoResponse(result);
        assertNotEquals(null, ret);
        assertEquals(expectedResult, ret);

        expectedResult = new Hashtable<>();
        expectedResult.put("PROTOCOLINFO", "");
        /* Strictly this does not follow the spec */
        expectedResult.put("AUTH", "METHOD=NULL C");
        expectedResult.put("OK", "");
        result = "250-PROTOCOLINFO\r\n250-AUTH METHOD=NULL C\r\n250 OK";
        ret = parseProtocolInfoResponse(result);
        assertNotEquals(null, ret);
        assertEquals(expectedResult, ret);

        expectedResult = new Hashtable<>();
        expectedResult.put("PROTOCOLINFO", "");
        /* Strictly this does not follow the spec */
        expectedResult.put("AUTH", "METHOD=NULL C");
        expectedResult.put("OK", "");
        result = "250-PROTOCOLINFO\r\n250-AUTH METHOD=NULL C\r\n250 OK\r\n";
        ret = parseProtocolInfoResponse(result);
        assertNotEquals(null, ret);
        assertEquals(expectedResult, ret);
    }

    @Test
    public void chooseAuthMethod_Test() throws Exception {
        Hashtable<String, String> input;
        AuthMethod result;
        AuthMethod expected;

        input = new Hashtable<>();
        input.put("PROTOCOLINFO", "");
        /* Strictly this does not follow the spec */
        expected = AuthMethod.UNKNOWN;
        assertEquals(expected, chooseAuthMethod(input));

        input = new Hashtable<>();
        input.put("AUTH", "");
        expected = AuthMethod.UNKNOWN;
        assertEquals(expected, chooseAuthMethod(input));

        input = new Hashtable<>();
        input.put("AUTH", "Methd");
        expected = AuthMethod.UNKNOWN;
        assertEquals(expected, chooseAuthMethod(input));

        input = new Hashtable<>();
        input.put("AUTH", "METHOD");
        expected = AuthMethod.UNKNOWN;
        assertEquals(expected, chooseAuthMethod(input));

        input = new Hashtable<>();
        input.put("AUTH", "METHOD=");
        expected = AuthMethod.UNKNOWN;
        assertEquals(expected, chooseAuthMethod(input));

        input = new Hashtable<>();
        input.put("AUTH", "METHOD=FOO");
        expected = AuthMethod.UNKNOWN;
        assertEquals(expected, chooseAuthMethod(input));

        input = new Hashtable<>();
        input.put("AUTH", "METHOD=NULL");
        expected = AuthMethod.NULL;
        assertEquals(expected, chooseAuthMethod(input));

        input = new Hashtable<>();
        input.put("AUTH", " METHOD=NULL");
        expected = AuthMethod.UNKNOWN;
        assertEquals(expected, chooseAuthMethod(input));

        input = new Hashtable<>();
        input.put("AUTH", " METHOD=NULL,BAR");
        expected = AuthMethod.UNKNOWN;
        assertEquals(expected, chooseAuthMethod(input));

        input = new Hashtable<>();
        input.put("AUTH", " METHOD=NULL,COOKIE");
        expected = AuthMethod.UNKNOWN;
        assertEquals(expected, chooseAuthMethod(input));

        input = new Hashtable<>();
        input.put("AUTH", "METHOD=NULL,COOKIE");
        expected = AuthMethod.NULL;
        assertEquals(expected, chooseAuthMethod(input));

        input = new Hashtable<>();
        input.put("AUTH", "METHOD=NULL,SAFECOOKIE");
        expected = AuthMethod.NULL;
        assertEquals(expected, chooseAuthMethod(input));

        input = new Hashtable<>();
        input.put("AUTH", "METHOD=SAFECOOKIE,NULL");
        expected = AuthMethod.NULL;
        assertEquals(expected, chooseAuthMethod(input));

        input = new Hashtable<>();
        input.put("AUTH", "METHOD=SAFECOOKIE");
        expected = AuthMethod.SAFECOOKIE;
        assertEquals(expected, chooseAuthMethod(input));

        input = new Hashtable<>();
        input.put("AUTH", "METHOD=SAFECOOKIE,FOO");
        expected = AuthMethod.UNKNOWN;
        assertEquals(expected, chooseAuthMethod(input));

        input = new Hashtable<>();
        input.put("AUTH", "METHOD=SAFECOOKIE,COOKIE,FOO");
        expected = AuthMethod.UNKNOWN;
        assertEquals(expected, chooseAuthMethod(input));

        input = new Hashtable<>();
        input.put("AUTH", "METHOD=SAFECOOKIE,HASHEDPASSWORD");
        expected = AuthMethod.SAFECOOKIE;
        assertEquals(expected, chooseAuthMethod(input));

        input = new Hashtable<>();
        input.put("AUTH", "METHOD=SAFECOOKIE,HASHEDPASSWORD");
        expected = AuthMethod.SAFECOOKIE;
        assertEquals(expected, chooseAuthMethod(input));

        input = new Hashtable<>();
        input.put("AUTH", "METHOD=HASHEDPASSWORD,SAFECOOKIE");
        expected = AuthMethod.SAFECOOKIE;
        assertEquals(expected, chooseAuthMethod(input));

        input = new Hashtable<>();
        input.put("AUTH", "METHOD=COOKIE,SAFECOOKIE COOKIEFILE");
        expected = AuthMethod.SAFECOOKIE;
        assertEquals(expected, chooseAuthMethod(input));
        assertEquals(null, controlPortCookiePath);

        input = new Hashtable<>();
        input.put("AUTH", "METHOD=COOKIE,SAFECOOKIE COOKIEFILE=");
        expected = AuthMethod.SAFECOOKIE;
        assertEquals(expected, chooseAuthMethod(input));
        assertEquals("", controlPortCookiePath);

        input = new Hashtable<>();
        input.put("AUTH", "METHOD=COOKIE,SAFECOOKIE COOKIEFILE=\"");
        expected = AuthMethod.SAFECOOKIE;
        assertEquals(expected, chooseAuthMethod(input));
        assertEquals("", controlPortCookiePath);

        input = new Hashtable<>();
        input.put("AUTH", "METHOD=COOKIE,SAFECOOKIE COOKIEFILE=\"foo");
        expected = AuthMethod.SAFECOOKIE;
        assertEquals(expected, chooseAuthMethod(input));
        assertEquals("foo", controlPortCookiePath);

        input = new Hashtable<>();
        input.put("AUTH", "METHOD=COOKIE,SAFECOOKIE COOKIEFILE=\"/path/to/auth\"");
        expected = AuthMethod.SAFECOOKIE;
        assertEquals(expected, chooseAuthMethod(input));
        assertEquals("/path/to/auth", controlPortCookiePath);
    }
}
