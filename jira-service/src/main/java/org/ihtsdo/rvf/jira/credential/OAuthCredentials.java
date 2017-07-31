package org.ihtsdo.rvf.jira.credential;

import com.google.gdata.client.authn.oauth.OAuthException;
import com.google.gdata.client.authn.oauth.OAuthParameters;
import com.google.gdata.client.authn.oauth.OAuthRsaSha1Signer;
import com.google.gdata.client.authn.oauth.OAuthUtil;
import net.rcarz.jiraclient.ICredentials;
import net.rcarz.jiraclient.JiraException;
import net.rcarz.jiraclient.RestClient;
import org.apache.http.HttpRequest;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.BASE64Decoder;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OAuthCredentials implements ICredentials {

	private static final String BEGIN_PRIVATE_KEY = "-----BEGIN PRIVATE KEY-----";
	private static final String END_PRIVATE_KEY = "-----END PRIVATE KEY-----";
	private static final String UTF_8 = "UTF-8";
	private static final String RSA = "RSA";
	private static final String OAUTH_CONSUMER_KEY = "oauth_consumer_key";
	private static final String OAUTH_TIMESTAMP = "oauth_timestamp";
	private static final String OAUTH_NONCE = "oauth_nonce";
	private static final String USER_ID = "user_id";
	private static final String OAUTH_SIGNATURE_METHOD = "oauth_signature_method";
	private static final String RSA_SHA1 = "RSA-SHA1";
	private static final String OAUTH_TOKEN = "oauth_token";
	private static final String BLANK = "";
	private static final String OAUTH_SIGNATURE = "oauth_signature";

	private final String username;
	private final String consumerKey;
	private final PrivateKey privateKey;

	private static final Logger LOGGER = LoggerFactory.getLogger(OAuthCredentials.class);

	public OAuthCredentials(String username, String consumerKey, PrivateKey privateKey) {
		this.username = username;
		this.consumerKey = consumerKey;
		this.privateKey = privateKey;
	}

	public OAuthCredentials(String username, String consumerKey, String privateKeyPath) throws NoSuchAlgorithmException, IOException, InvalidKeySpecException {
		this(username, consumerKey, getPrivateKey(privateKeyPath));
	}

	@Override
	public void initialize(RestClient restClient) throws JiraException {

	}

	@Override
	public void authenticate(HttpRequest request) {
		if (request instanceof HttpRequestBase) {
			HttpRequestBase requestBase = (HttpRequestBase) request;
			try {
				final String uri = requestBase.getRequestLine().getUri();
				LOGGER.debug("Initial uri {}", uri);

				// Gather OAuth params
				final URIBuilder uriBuilder = new URIBuilder(uri);

				Map<String, String> params = nameValuePairsToMap(uriBuilder.getQueryParams());
				params.put(OAUTH_CONSUMER_KEY, consumerKey);
				params.put(OAUTH_TIMESTAMP, OAuthUtil.getTimestamp());
				params.put(OAUTH_NONCE, OAuthUtil.getNonce());
				params.put(USER_ID, username);
				params.put(OAUTH_SIGNATURE_METHOD, RSA_SHA1);
				params.put(OAUTH_TOKEN, BLANK);

				// Build oauth_signature
				String baseString = OAuthUtil.getSignatureBaseString(uri, requestBase.getRequestLine().getMethod(), params);

				// Force blank oauth_token into the base string
				String timestamp = OAUTH_TIMESTAMP + "%3D" + params.get(OAUTH_TIMESTAMP) + "%26";
				baseString = baseString.replace(timestamp, timestamp + "oauth_token%3D%26");

				LOGGER.debug("baseString {}", baseString);
				final OAuthParameters oauthParameters = new OAuthParameters();
				for (String key : params.keySet()) {
					oauthParameters.addCustomBaseParameter(key, params.get(key));
				}
				OAuthRsaSha1Signer rsaSigner = new OAuthRsaSha1Signer(privateKey);
				String signature = rsaSigner.getSignature(baseString, oauthParameters);
				params.put(OAUTH_SIGNATURE, signature);
				//uriBuilder.setParameters(mapToNameValuePairs(params));

				final URI signedUri = uriBuilder.build();
				LOGGER.debug("Signed uri {}", signedUri);
				requestBase.setURI(signedUri);
			} catch (OAuthException | URISyntaxException e) {
				LOGGER.error("Failed to sign jira http request.", e);
			}
		} else {
			LOGGER.error("Failed to sign jira http request. Can only sign HttpRequestBase requests but got {}", request.getClass().getName());
		}
	}

	@Override
	public String getLogonName() {
		return username;
	}

	@Override
	public void logout(RestClient restClient) throws JiraException {
	}

	private Map<String, String> nameValuePairsToMap(List<NameValuePair> queryParams) {
		Map<String, String> map = new HashMap<>();
		for (NameValuePair queryParam : queryParams) {
			map.put(queryParam.getName(), queryParam.getValue());
		}
		return map;
	}

	private List<NameValuePair> mapToNameValuePairs(Map<String, String> params) {
		List<NameValuePair> pairs = new ArrayList<>();
		for (String key : params.keySet()) {
			pairs.add(new BasicNameValuePair(key, params.get(key)));
		}
		return pairs;
	}

	/*
	 * Creates a RSAPrivateKey from the PEM file.
	 * This method is a little expensive. Building this key once and reusing is most efficient.
	 */
	public static PrivateKey getPrivateKey(String privKeyPath) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
		final File privateKeyFile = new File(privKeyPath);
		if (!privateKeyFile.isFile()) {
			LOGGER.error("Failed to load private key using path {}", privKeyPath);
			throw new FileNotFoundException("Private key not found.");
		}

		String privateKeyString;
		try (InputStream inputStream = new FileInputStream(privateKeyFile)) {
			DataInputStream dataInputStream = new DataInputStream(inputStream);
			byte[] privKeyBytes = new byte[dataInputStream.available()];
			dataInputStream.readFully(privKeyBytes);
			privateKeyString = new String(privKeyBytes, UTF_8);
		}

		// Trim header and footer
		int startIndex = privateKeyString.indexOf(BEGIN_PRIVATE_KEY);
		if (startIndex != -1) {
			int endIndex = privateKeyString.indexOf(END_PRIVATE_KEY);
			privateKeyString = privateKeyString.substring(startIndex + BEGIN_PRIVATE_KEY.length(), endIndex);
			// decode private key
			PKCS8EncodedKeySpec privSpec = new PKCS8EncodedKeySpec((new BASE64Decoder()).decodeBuffer(privateKeyString));
			return KeyFactory.getInstance(RSA).generatePrivate(privSpec);
		} else {
			throw new IOException("Unexpected private key format. File should contain " + BEGIN_PRIVATE_KEY);
		}
	}

}
