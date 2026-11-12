package com.bakouan.app.security;

import com.google.common.net.InetAddresses;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ClientIpResolver {

    private final Set<String> trustedProxies;

    public ClientIpResolver(@Value("${app.security.trusted-proxies:}") final String trustedProxies) {
        this.trustedProxies = Arrays.stream(trustedProxies.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(value -> InetAddresses.toAddrString(InetAddresses.forString(value)))
                .collect(Collectors.toUnmodifiableSet());
    }

    public String resolve(final HttpServletRequest request) {
        String remoteAddress = normalize(request.getRemoteAddr());
        if (remoteAddress == null) {
            return "unknown";
        }
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (!trustedProxies.contains(remoteAddress) || forwardedFor == null
                || forwardedFor.isBlank() || forwardedFor.length() > 2048) {
            return remoteAddress;
        }

        String clientAddress = remoteAddress;
        String[] forwardedAddresses = forwardedFor.split(",", -1);
        // Parcourir depuis le proxy le plus proche pour ignorer un prefixe falsifie.
        for (int i = forwardedAddresses.length - 1; i >= 0 && trustedProxies.contains(clientAddress); i--) {
            String address = normalize(forwardedAddresses[i].trim());
            if (address == null) {
                return remoteAddress;
            }
            clientAddress = address;
        }
        return clientAddress;
    }

    private String normalize(final String address) {
        return address != null && InetAddresses.isInetAddress(address)
                ? InetAddresses.toAddrString(InetAddresses.forString(address)) : null;
    }
}