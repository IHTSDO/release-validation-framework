package org.ihtsdo.rvf.core.service.duck;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/**
 * Fetches assertion packs by URL and verifies each against a pinned digest.
 *
 * <p>A pack is executable SQL arriving over a network, which is remote code
 * execution by design. Three things follow, and all three are enforced here
 * rather than left to a deployment:
 *
 * <ul>
 * <li><b>The digest is required and checked before the bytes are parsed.</b>
 *     Not after: a store is JSON, and parsing attacker-controlled JSON to find
 *     out whether it is the right JSON has the order backwards.
 * <li><b>No {@code latest}.</b> A pinned digest is what makes a run
 *     reproducible; a mutable tag makes "which assertions produced this report"
 *     unanswerable, which is the whole cost packs are meant to avoid.
 * <li><b>A token is never in the URL.</b> Private packs take an Authorization
 *     header supplied by configuration, because a URL ends up in logs.
 * </ul>
 *
 * <p>{@code file:} URLs are accepted from configuration: a pack on a volume
 * mounted into the container, a pack under test before it is published, and
 * this class's tests without a network. A validation request cannot name a
 * location at all; see {@link AssertionPackChannels#resolveRunPins}.
 *
 * <p>Two details are what a GitHub release asset needs:
 *
 * <ul>
 * <li><b>{@code Accept: application/octet-stream}.</b> Asked for
 *     {@code application/json}, the GitHub API returns the asset's METADATA -
 *     a JSON object describing the file - rather than the file. That fails the
 *     digest check, so it fails safe, but it never succeeds either.
 * <li><b>Redirects are followed.</b> The API answers 302 to a
 *     release-assets host, and {@code HttpClient}'s default is
 *     {@code Redirect.NEVER}, under which the fetch fails with "returned
 *     HTTP 302". The Authorization header is carried across, which that host
 *     requires.
 * </ul>
 */
public class AssertionPackFetcher {

	private static final Logger LOGGER = LoggerFactory.getLogger(AssertionPackFetcher.class);

	/** One configured pack: where it is, and what it must hash to. */
	public record Source(String name, String version, URI uri, String sha256, String authHeader) {

		public Source {
			if (name == null || name.isBlank()) {
				throw new IllegalArgumentException("a pack needs a name to appear in a report");
			}
			if (sha256 == null || sha256.isBlank()) {
				throw new IllegalArgumentException(
						"pack " + name + " has no pinned sha256 - an unpinned pack cannot be "
								+ "reproduced, so a report naming it would mean nothing");
			}
		}

		/** Without the credential, which must not reach a log. */
		@Override
		public String toString() {
			return "Source[name=" + name + ", version=" + version + ", uri=" + uri + ", sha256=" + sha256
					+ ", authHeader=" + (authHeader == null ? null : "<redacted>") + "]";
		}
	}

	private final HttpClient http;

	public AssertionPackFetcher() {
		this(HttpClient.newBuilder()
				.connectTimeout(Duration.ofSeconds(20))
				.followRedirects(HttpClient.Redirect.NORMAL)
				.build());
	}

	AssertionPackFetcher(HttpClient http) {
		this.http = http;
	}

	/**
	 * Fetches and verifies every source, in order.
	 *
	 * @throws IOException if any fetch fails or any digest does not match. All
	 *     or nothing on purpose: a partial pack set is a different assertion
	 *     corpus, and swapping to one silently would change what a validation
	 *     covers without saying so.
	 */
	public List<DuckStorePacks.Pack> fetch(List<Source> sources) throws IOException {
		List<DuckStorePacks.Pack> packs = new ArrayList<>();
		for (Source source : sources) {
			byte[] body = read(source);
			String actual = sha256(body);
			if (!actual.equalsIgnoreCase(strip(source.sha256()))) {
				throw new IOException("pack " + source.name() + " from " + source.uri()
						+ " has digest " + actual + ", pinned " + strip(source.sha256())
						+ " - refusing to load SQL that is not what was pinned");
			}
			DuckStore store = DuckStore.parse(new String(body, StandardCharsets.UTF_8));
			packs.add(new DuckStorePacks.Pack(source.name(), source.version(),
					"sha256:" + actual, store));
			LOGGER.info("assertion pack {}@{}: {} assertions, digest verified",
					source.name(), source.version(), store.assertions().size());
		}
		return packs;
	}

	private byte[] read(Source source) throws IOException {
		return read(source.uri(), source.authHeader());
	}

	/**
	 * Bytes from a location, with the same rules a pack is fetched under.
	 *
	 * <p>Public because a channel index is fetched the same way and must be: it
	 * lives in the same place, behind the same credential, and is subject to the
	 * same Accept and redirect behaviour. An index read by some other code path
	 * would be the obvious place for those rules to drift apart, and the digests
	 * it publishes would then describe bytes nobody fetches.
	 */
	public byte[] read(URI uri, String authHeader) throws IOException {
		if ("file".equals(uri.getScheme())) {
			return Files.readAllBytes(Path.of(uri));
		}
		HttpRequest.Builder request = HttpRequest.newBuilder(uri)
				.timeout(Duration.ofMinutes(5))
				// ONLY octet-stream. Offered "application/octet-stream,
				// application/json", the GitHub API picks json and returns the
				// asset's METADATA - 1.8 KB describing the file - which then
				// fails the digest check. Worse, that metadata carries
				// download_count, so it hashes differently on every fetch and
				// the error looks like a moving target rather than a wrong
				// Accept header. A file: URL ignores this entirely.
				.header("Accept", "application/octet-stream");
		if (authHeader != null && !authHeader.isBlank()) {
			request.header("Authorization", authHeader);
		}
		try {
			HttpResponse<InputStream> response =
					http.send(request.build(), HttpResponse.BodyHandlers.ofInputStream());
			if (response.statusCode() != 200) {
				throw new IOException(uri + " returned HTTP " + response.statusCode());
			}
			try (InputStream in = response.body()) {
				return in.readAllBytes();
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IOException("interrupted fetching " + uri, e);
		}
	}

	/** Accepts a bare hex digest or the {@code sha256:} prefixed form. */
	private static String strip(String digest) {
		String trimmed = digest.trim();
		return trimmed.startsWith("sha256:") ? trimmed.substring(7) : trimmed;
	}

	private static String sha256(byte[] bytes) {
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
		} catch (Exception e) {
			throw new IllegalStateException("no SHA-256", e);
		}
	}
}
