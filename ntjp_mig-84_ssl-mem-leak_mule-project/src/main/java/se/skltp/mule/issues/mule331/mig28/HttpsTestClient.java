package se.skltp.mule.issues.mule331.mig28;

import java.net.URL;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.contrib.ssl.AuthSSLProtocolSocketFactory;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.protocol.Protocol;

public class HttpsTestClient {

	public static void main(String[] args) {
		try {
			makeHttpsRequest();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void makeHttpsRequest() throws Exception {
		Protocol mutualAuthHttps = new Protocol("https",
				new AuthSSLProtocolSocketFactory(new URL(
						"file:certs/client.jks"), "password", new URL(
						"file:certs/truststore.jks"), "password"), 8082);

		HttpClient httpClient = new HttpClient();
		httpClient.getHostConfiguration().setHost("localhost", 8082,
				mutualAuthHttps);

		GetMethod getMethod = new GetMethod("/");
		try {
			httpClient.executeMethod(getMethod);
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("got exception: " + e.getMessage());
		} finally {
			getMethod.releaseConnection();
		}
	}
}
