package org.ihtsdo.rvf.core.service.duck;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * The locations a pack may be fetched from, and the versions each one offers.
 *
 * <p>A pack is pinned by digest. A channel is a trusted location that publishes an index of
 * what it holds: each version, the URL to fetch and the digest those bytes must have. A
 * caller then asks for {@code example-pack@2026.09.02} and the server resolves the rest, so a
 * digest is discovered rather than copied by hand (release notes carry two different
 * sha256 values, the pack's identity and the asset's byte hash, and only one is right).
 *
 * <p>The channel configuration, plus the credential that reads it, is the trust root; an
 * index at an unconfigured location is not fetched, and a version whose URL is not at the
 * index's own scheme and host (or a host the channel lists in {@code allowedHosts}) is
 * refused. Resolving a version produces a pin and changes nothing a deployment serves: that
 * stays {@code rvf.assertion.packs}.
 *
 * <pre>
 * rvf.assertion.channels[0] = name=example;index=https://.../example-index.json;packs=example-pack;authHeader=Bearer ${TOKEN}
 * </pre>
 *
 * <p>Same grammar as a pin: semicolons inside one entry, commas between. {@code packs} lists
 * the pack names this channel may serve; a shorthand pin for any other name is refused.
 * {@code allowedHosts} lists, separated by {@code |}, further hosts its index may point to.
 */
public class AssertionPackChannels {

	private static final Logger LOGGER = LoggerFactory.getLogger(AssertionPackChannels.class);

	/** {@code name@version}: the only form a validation request may pin a pack with. */
	private static final Pattern SHORTHAND = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]*@[A-Za-z0-9][A-Za-z0-9._-]*");

	private static final String REDACTED = "<redacted>";

	/** One configured location, and the pack names it is allowed to serve. */
	public record Channel(String name, URI index, List<String> packs, String authHeader, List<String> allowedHosts) {

		public Channel {
			if (name == null || name.isBlank()) {
				throw new IllegalArgumentException(
						"a channel needs a name: it is what a caller asks for and what a "
								+ "report records");
			}
			if (index == null) {
				throw new IllegalArgumentException(
						"channel " + name + " has no index - without one the server cannot "
								+ "say which versions exist, and a caller would be back to "
								+ "supplying a digest by hand");
			}
			packs = packs == null || packs.isEmpty() ? List.of(name) : List.copyOf(packs);
			allowedHosts = allowedHosts == null ? List.of() : List.copyOf(allowedHosts);
		}

		/** Whether this channel is allowed to serve a pack of that name. */
		public boolean serves(String pack) {
			return packs.contains(pack);
		}

		/**
		 * Whether a URL its index lists may be fetched: the index's own scheme, and its host
		 * or one of {@link #allowedHosts}.
		 */
		boolean mayFetch(URI uri) {
			if (uri.getScheme() == null || !uri.getScheme().equalsIgnoreCase(index.getScheme())) {
				return false;
			}
			String host = uri.getHost();
			if (host == null || index.getHost() == null) {
				return host == null && index.getHost() == null;
			}
			return host.equalsIgnoreCase(index.getHost())
					|| allowedHosts.stream().anyMatch(host::equalsIgnoreCase);
		}

		/** Without the credential, which must not reach a log. */
		@Override
		public String toString() {
			return "Channel[name=" + name + ", index=" + index + ", packs=" + packs
					+ ", authHeader=" + (authHeader == null ? null : REDACTED) + ", allowedHosts=" + allowedHosts + "]";
		}
	}

	/** One version a channel offers. */
	public record Version(String version, URI uri, String sha256, int assertions,
			List<String> requires, String published, boolean draft) {
	}

	private final List<Channel> channels;
	private final AssertionPackFetcher fetcher;

	public AssertionPackChannels(List<Channel> channels, AssertionPackFetcher fetcher) {
		this.channels = channels == null ? List.of() : List.copyOf(channels);
		this.fetcher = fetcher;
	}

	public List<Channel> channels() {
		return channels;
	}

	public Optional<Channel> channel(String name) {
		return channels.stream().filter(c -> c.name().equalsIgnoreCase(name)).findFirst();
	}

	/** The channel that may serve this pack name, if any is configured to. */
	public Optional<Channel> channelForPack(String pack) {
		return channels.stream().filter(c -> c.serves(pack)).findFirst();
	}

	/**
	 * The versions a channel offers, newest first.
	 *
	 * <p>Fetched every time rather than cached. An index is small, the answer
	 * changes whenever someone publishes, and a cached list that says a version
	 * does not exist when it does is a worse failure than an extra request.
	 *
	 * @throws IOException if the index cannot be read, names another channel, or lists a URL
	 *                     the channel may not fetch
	 */
	public List<Version> versions(Channel channel) throws IOException {
		byte[] body = fetcher.read(channel.index(), channel.authHeader());
		JsonNode index;
		try {
			index = DuckStore.MAPPER.readTree(body);
		} catch (JacksonException e) {
			throw new IOException("the index of channel " + channel.name() + " at " + channel.index()
					+ " is not valid JSON: " + e.getOriginalMessage(), e);
		}
		String declared = index.path("channel").asString("");
		if (!declared.isBlank() && !declared.equalsIgnoreCase(channel.name())) {
			throw new IOException("channel " + channel.name() + " is configured to read "
					+ channel.index() + ", and that index declares itself to be '"
					+ declared + "'. Refusing it: an index naming a different channel is "
					+ "either misconfiguration or substitution, and both end with a "
					+ "release validated against assertions nobody chose.");
		}
		List<Version> out = new ArrayList<>();
		for (JsonNode entry : index.path("versions")) {
			String version = entry.path("version").asString("");
			String uri = entry.path("uri").asString("");
			String sha256 = entry.path("sha256").asString("");
			if (version.isBlank() || uri.isBlank() || sha256.isBlank()) {
				LOGGER.warn("channel {}: skipping an index entry missing version, uri or "
						+ "sha256 - a version that cannot be pinned cannot be offered",
						channel.name());
				continue;
			}
			URI location = URI.create(uri);
			if (!channel.mayFetch(location)) {
				throw new IOException("channel " + channel.name() + " lists version " + version + " at " + location
						+ ", which is not at its index's scheme and host (" + channel.index().getScheme() + "://"
						+ channel.index().getHost() + ") or a host in its allowedHosts " + channel.allowedHosts()
						+ ". Refusing the index: the channel configuration decides where packs come from, "
						+ "not the index.");
			}
			List<String> requires = new ArrayList<>();
			for (JsonNode need : entry.path("requires")) {
				requires.add(need.path("pack").asString("") + " atLeast "
						+ need.path("atLeast").asString(""));
			}
			out.add(new Version(version, location, sha256,
					entry.path("assertions").asInt(0), requires,
					entry.path("published").asString(""), entry.path("draft").asBoolean(false)));
		}
		return out;
	}

	/**
	 * A configured pin ({@code rvf.assertion.packs}) as a full spec: {@code name@version}
	 * resolved through a channel, and a full spec returned untouched.
	 *
	 * <p>The shorthand exists so a pipeline variable can be a version and nothing else. A
	 * full spec, with its own uri and digest, is the deployment owner's to write, so a pin
	 * that names a location no channel serves keeps working for a one-off.
	 */
	public String resolve(String spec) throws IOException {
		String trimmed = spec == null ? "" : spec.trim();
		if (trimmed.isEmpty() || trimmed.contains(";") || trimmed.contains("=")) {
			return trimmed;
		}
		return resolveShorthand(trimmed);
	}

	/** Every configured pin resolved, in order, leaving full specs alone. */
	public List<String> resolveAll(List<String> specs) throws IOException {
		if (specs == null || specs.isEmpty()) {
			return List.of();
		}
		List<String> out = new ArrayList<>(specs.size());
		for (String spec : specs) {
			String resolved = resolve(spec);
			if (!resolved.isBlank()) {
				out.add(resolved);
			}
		}
		return out;
	}

	/**
	 * A validation request's pins, resolved through the configured channels.
	 *
	 * <p>A request may only name a version a channel offers. A full spec, a {@code file:}
	 * location or a credential in a request would let any caller make the server fetch any
	 * URL or read any file, and run SQL of the caller's choosing; those stay in configuration.
	 *
	 * @throws IOException if a pin is not {@code name@version}, or no channel offers it
	 */
	public List<String> resolveRunPins(List<String> pins) throws IOException {
		List<String> invalid = invalidRunPins(pins);
		if (!invalid.isEmpty()) {
			throw new IOException(runPinMessage(invalid));
		}
		List<String> out = new ArrayList<>();
		for (String pin : pins) {
			if (pin != null && !pin.isBlank()) {
				out.add(resolveShorthand(pin.trim()));
			}
		}
		return out;
	}

	/** The pins a validation request may not use: anything but {@code name@version}. */
	public static List<String> invalidRunPins(List<String> pins) {
		List<String> invalid = new ArrayList<>();
		for (String pin : pins == null ? List.<String>of() : pins) {
			if (pin != null && !pin.isBlank() && !SHORTHAND.matcher(pin.trim()).matches()) {
				invalid.add(redact(pin.trim()));
			}
		}
		return invalid;
	}

	/** Why {@code invalid} pins are refused, for the caller who sent them. */
	public static String runPinMessage(List<String> invalid) {
		return "assertionPacks accepts only name@version for a pack a configured channel "
				+ "(rvf.assertion.channels) serves, e.g. example-pack@2026.09.02. Full pack specs, uri=, "
				+ "file: locations and authHeader are accepted only in the deployment's rvf.assertion.packs. "
				+ "Refused: " + invalid;
	}

	/** A spec with any {@code authHeader} value replaced, for logs and messages. */
	public static String redact(String spec) {
		return spec == null ? null : spec.replaceAll("(?i)(authHeader=)[^;]*", "$1" + REDACTED);
	}

	private String resolveShorthand(String trimmed) throws IOException {
		int at = trimmed.lastIndexOf('@');
		if (at <= 0 || at == trimmed.length() - 1) {
			throw new IOException("'" + redact(trimmed) + "' is not a pack spec and not "
					+ "name@version. A shorthand pin is exactly name@version, for example "
					+ "example-pack@2026.09.02.");
		}
		String pack = trimmed.substring(0, at);
		String wanted = trimmed.substring(at + 1);

		Channel channel = channelForPack(pack).orElseThrow(() -> new IOException(
				"no configured channel serves pack '" + pack + "'. Channels are the "
						+ "locations this deployment trusts, so an unlisted pack is "
						+ "refused rather than fetched: " + channels.stream()
						.map(c -> c.name() + "=" + c.packs()).toList()));

		List<Version> available = versions(channel);
		Version match = available.stream()
				.filter(v -> v.version().equals(wanted))
				.findFirst()
				.orElseThrow(() -> new IOException("channel " + channel.name()
						+ " offers no version '" + wanted + "'. It has: "
						+ available.stream().map(Version::version).toList()));

		LOGGER.info("resolved {}@{} from channel {} to {} ({})",
				pack, wanted, channel.name(), match.uri(), match.sha256());
		return "name=" + pack + ";version=" + match.version()
				+ ";uri=" + match.uri() + ";sha256=" + match.sha256()
				+ (channel.authHeader() == null || channel.authHeader().isBlank()
						? "" : ";authHeader=" + channel.authHeader());
	}

	/** Channels parsed from configuration, in the pin grammar. */
	public static List<Channel> parse(List<String> specs) {
		List<Channel> out = new ArrayList<>();
		for (String spec : specs == null ? List.<String>of() : specs) {
			if (spec == null || spec.isBlank()) {
				continue;
			}
			Map<String, String> fields = new LinkedHashMap<>();
			for (String part : spec.split(";")) {
				String[] kv = part.split("=", 2);
				if (kv.length == 2) {
					fields.put(kv[0].trim().toLowerCase(Locale.ROOT), kv[1].trim());
				}
			}
			String packs = fields.getOrDefault("packs", "");
			String hosts = fields.getOrDefault("allowedhosts", "");
			out.add(new Channel(
					fields.getOrDefault("name", ""),
					URI.create(fields.getOrDefault("index", "")),
					packs.isBlank() ? null : List.of(packs.split("\\s*\\|\\s*")),
					fields.get("authheader"),
					hosts.isBlank() ? null : List.of(hosts.split("\\s*\\|\\s*"))));
		}
		return out;
	}
}
