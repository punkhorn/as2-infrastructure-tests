package org.punkhorn.as2;

import java.security.Provider;
import java.security.Security;

public class ProviderTest {

    public static void main(String[] args) {
        
        for (Provider provider: Security.getProviders()) {
            System.out.println("Provider: " + provider.getName());
        }
        
        Provider provider = Security.getProvider("BC");
        for (Provider.Service service: provider.getServices()) {
            System.out.println("Service: type: " + service.getType() + " algorithm: " +  service.getAlgorithm());
        }
    }
    
}
