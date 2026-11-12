package com.bakouan.app.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.*;

class ClientIpResolverTest {
    @Test
    void untrustedClientCannotOverrideItsAddressAndPortDoesNotChangeKey() {
        MockHttpServletRequest request = request("192.0.2.10", "198.51.100.20");
        request.setRemotePort(30000);
        ClientIpResolver resolver = new ClientIpResolver("");
        assertEquals("192.0.2.10", resolver.resolve(request));
        request.setRemotePort(31000);
        assertEquals("192.0.2.10", resolver.resolve(request));
    }

    @Test
    void trustedProxyChainIgnoresForgedLeftmostAddress() {
        ClientIpResolver resolver = new ClientIpResolver("127.0.0.1,127.0.0.2");
        assertEquals("192.0.2.10", resolver.resolve(
                request("127.0.0.1", "198.51.100.99, 192.0.2.10, 127.0.0.2")));
    }

    @Test
    void malformedForwardedAddressFallsBackToPeer() {
        ClientIpResolver resolver = new ClientIpResolver("127.0.0.1");
        assertEquals("127.0.0.1", resolver.resolve(request("127.0.0.1", "example.org")));
        assertEquals("127.0.0.1", resolver.resolve(request("127.0.0.1", "192.0.2.1,")));
    }

    @Test
    void equivalentIpv6ProxyAddressesAreCanonicalized() {
        assertEquals("2001:db8::1", new ClientIpResolver("::1").resolve(
                request("0:0:0:0:0:0:0:1", " 2001:db8:0:0:0:0:0:1 ")));
    }

    private MockHttpServletRequest request(String remote, String forwarded) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(remote);
        request.addHeader("X-Forwarded-For", forwarded);
        return request;
    }
}