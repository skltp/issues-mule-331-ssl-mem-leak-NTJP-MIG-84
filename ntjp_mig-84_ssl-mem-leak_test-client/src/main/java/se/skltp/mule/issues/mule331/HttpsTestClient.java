package se.skltp.mule.issues.mule331;

import java.io.FileInputStream;
import java.net.URL;
import java.security.KeyStore;
import java.util.Random;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.xml.ws.BindingProvider;

import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.w3.wsaddressing10.AttributedURIType;

import se.riv.itinfra.tp.ping.v1.rivtabp20.PingResponderInterface;
import se.riv.itinfra.tp.ping.v1.rivtabp20.PingResponderService;
import se.riv.itinfra.tp.pingresponder.v1.PingRequestType;
import se.riv.itinfra.tp.pingresponder.v1.PingResponseType;

/**
 * Code largely copied from <code>se.riv.itinfra.tp.ping.PingConsumer</code> to
 * re-use SSL-setup.
 */
public class HttpsTestClient {

	private static String logicalAddress = "Ping";
	private static long sleepTime = 5000L;
	private static String endpointAddress = "https://localhost:20000/vp/Ping/1/rivtabp20";

	private static PingResponderInterface service = null;

	public static void main(String[] args) throws Exception {

		if (args.length > 0) {
			sleepTime = Long.parseLong(args[0]);
			endpointAddress = args[1];
			logicalAddress = args[2];
		}

		long randomness = sleepTime / 3L;
		Random random = new Random();
		System.out.println("Consumer connecting to " + endpointAddress);
		service = setupService(endpointAddress, logicalAddress);
		long requestCount = 0;
		long errorCount = 0;
		long timeStart = System.currentTimeMillis();
		while (true) {
			long start = System.nanoTime();
			try {
				requestCount++;
				String p = callService(logicalAddress);
				//System.out.println("Returned: " + p);
			} catch (Exception e) {
				//System.out.println("Error: " + e.toString());
				errorCount++;
			} finally {
				//System.out.println("Total time: " + (System.nanoTime() - start)
				//		/ 1000000L + " ms");
			}
			//Thread.sleep((long) (sleepTime - randomness + 2L * randomness
			//		* random.nextDouble()));
			long timeRunningSecs = (System.currentTimeMillis() - timeStart) / 1000; 
			System.out.println("Request count: " + requestCount + ", error count: " + errorCount + ", time running [s]: " + timeRunningSecs);
			Thread.sleep(100); // 100ms --> roughly 10 requests/sec
		}

	}

	public static String callService(String logicalAddresss) {

		AttributedURIType logicalAddressHeader = new AttributedURIType();
		logicalAddressHeader.setValue(logicalAddresss);

		PingRequestType request = new PingRequestType();

		// The below request is created to mimic the sample xml-message
		request.setPingIn("Pinging!");

		//try {
			PingResponseType result = service.ping(logicalAddressHeader,
					request);

			return ("Ping response=" + result.getPingUt());

		//} catch (Exception ex) {
			//System.out.println("Exception=" + ex.getMessage());
			//return null;
		//}
	}

	public static PingResponderInterface setupService(String serviceAddress,
			String logicalAddresss) {
		try {
			ClassLoader loader = Thread.currentThread().getContextClassLoader();
			URL wsdlUrl = loader
					.getResource("PingInteraction_1.0_rivtabp20.wsdl");

			PingResponderService service = new PingResponderService(wsdlUrl);
			PingResponderInterface serviceInterface = service
					.getPingResponderPort();

			BindingProvider provider = (BindingProvider) serviceInterface;
			provider.getRequestContext().put(
					"javax.xml.ws.service.endpoint.address", serviceAddress);

			Client client = ClientProxy.getClient(serviceInterface);
			HTTPConduit http = (HTTPConduit) client.getConduit();

			HTTPClientPolicy httpClientPolicy = new HTTPClientPolicy();
			httpClientPolicy.setConnectionTimeout(36000L);
			httpClientPolicy.setAllowChunking(false);
			httpClientPolicy.setReceiveTimeout(32000L);
			http.setClient(httpClientPolicy);

			if (serviceAddress.startsWith("https")) {
				TLSClientParameters tlsCP = setUpTlsClientParams();
				http.setTlsClientParameters(tlsCP);
			}

			return serviceInterface;
		} catch (Throwable ex) {
			System.out.println("Exception={}" + ex.getMessage());
			throw new RuntimeException(ex);
		}
	}

	private static TLSClientParameters setUpTlsClientParams() throws Exception {

		KeyStore trustStore = KeyStore.getInstance("JKS");
		//String trustStoreLoc = "certs/truststore.jks";
		String trustStoreLoc = "certs/MIG-72_BAD_truststore.jks";
		String trustPassword = "password";
		trustStore.load(new FileInputStream(trustStoreLoc),
				trustPassword.toCharArray());

		String keyPassword = "password";
		KeyStore keyStore = KeyStore.getInstance("jks");
		String keyStoreLoc = "certs/client.jks";		
		keyStore.load(new FileInputStream(keyStoreLoc),
				keyPassword.toCharArray());

		TrustManagerFactory tmf = TrustManagerFactory
				.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		tmf.init(trustStore);

		KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory
				.getDefaultAlgorithm());
		kmf.init(keyStore, keyPassword.toCharArray());

		TLSClientParameters tlsCP = new TLSClientParameters();
		tlsCP.setTrustManagers(tmf.getTrustManagers());
		tlsCP.setKeyManagers(kmf.getKeyManagers());

		// The following is not recommended and would not be done in a
		// prodcution environment,
		// this is just for illustrative purpose
		tlsCP.setDisableCNCheck(true);

		return tlsCP;

	}
}
