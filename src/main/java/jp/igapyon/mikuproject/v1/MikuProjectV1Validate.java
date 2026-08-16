/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuproject.v1;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * P5-C1 implementation of the isolated v1 {@code validate} command service.
 *
 * <p>The legacy {@code MikuprojectCli} intentionally does not delegate here:
 * its command grammar and its silent-overwrite history are not the v1 contract.
 * A later P5-E launcher must verify the installed runtime manifest and then
 * supply a {@link VerifiedRuntime}; this class never fabricates that proof.</p>
 */
public final class MikuProjectV1Validate {
    private static final String PROJECT_NAMESPACE = "http://schemas.microsoft.com/project";
    private static final String PROFILE = "miku-project-ms-project-xml-subset/v1";
    private static final String CAPABILITY = "miku-project-cli-core/v1";
    private static final String FIXTURE_SUITE_VERSION = "1";
    private static final Pattern IDENTITY = Pattern.compile("(?:0|[1-9][0-9]*)");
    private static final Pattern INTEGER = Pattern.compile("(?:0|[1-9][0-9]*)");
    /** JavaScript's Number.isSafeInteger upper bound used by the Node contract. */
    private static final long MAX_SAFE_INTEGER = 9007199254740991L;
    private static final Pattern DURATION = Pattern.compile("PT[0-9]+H(?:[0-5]?[0-9])M(?:[0-5]?[0-9])S");
    private static final Pattern UNITS = Pattern.compile("(?:0|[1-9][0-9]*)(?:\\.[0-9]+)?");
    private static final Pattern DATETIME = Pattern.compile("(\\d{4})-(\\d{2})-(\\d{2})T(\\d{2}):(\\d{2}):(\\d{2})");
    private static final Set<String> PROJECT_CHILDREN = setOf("Name", "StartDate", "FinishDate", "CurrentDate",
            "ScheduleFromStart", "CalendarUID", "Tasks", "Resources", "Assignments", "Calendars");
    private static final Set<String> TASK_CHILDREN = setOf("UID", "ID", "Name", "OutlineLevel", "OutlineNumber",
            "Start", "Finish", "Duration", "Milestone", "Summary", "PercentComplete", "CalendarUID",
            "PredecessorLink");
    private static final Set<String> PREDECESSOR_CHILDREN = setOf("PredecessorUID", "Type", "LinkLag", "LagFormat");
    private static final Set<String> RESOURCE_CHILDREN = setOf("UID", "ID", "Name", "Type", "CalendarUID");
    private static final Set<String> ASSIGNMENT_CHILDREN = setOf("UID", "TaskUID", "ResourceUID", "Start", "Finish",
            "Units", "Work");
    private static final Set<String> CALENDAR_CHILDREN = setOf("UID", "Name", "IsBaseCalendar");

    private MikuProjectV1Validate() {
    }

    /**
     * Executes only the v1 validate grammar. The caller owns process wiring;
     * stdout always receives a canonical LF-terminated result when
     * {@code --result -} is used.
     */
    public static int run(String[] argv, InputStream stdin, OutputStream stdout, OutputStream stderr,
            VerifiedRuntime runtime) {
        if (argv == null) {
            argv = new String[0];
        }
        if (stdin == null || stdout == null) {
            throw new IllegalArgumentException("stdin and stdout are required");
        }

        Invocation invocation;
        try {
            invocation = parseInvocation(argv);
        } catch (V1Failure failure) {
            return write(stdout, errorResult("cli", failure, RuntimeBinding.unverified(runtime), emptyIo(stdoutTarget()), null));
        }

        RuntimeBinding binding = RuntimeBinding.verifiedOrUnverified(runtime);
        if (!binding.verified) {
            V1Failure failure = failure("runtime.manifest-invalid", "runtime",
                    "The v1 Java runtime binding was not verified before command execution.", null, null, null,
                    "not-retryable", Collections.<String, Object>emptyMap(), "runtime-error");
            return write(stdout, errorResult("validate", failure, binding, unreadProjectIo(invocation.project, stdoutTarget()), null));
        }

        ResultTransport transport;
        try {
            transport = reserveResult(invocation.result, stdout);
        } catch (V1Failure failure) {
            return write(stdout, errorResult("validate", failure, binding, unreadProjectIo(invocation.project, stdoutTarget()),
                    "rejected".equals(failure.status) ? validationData(false, null, null) : null));
        }

        InputData input = null;
        try {
            input = readProject(invocation.project, stdin);
            DecodedProject decoded = decode(input.bytes);
            Validation validation = validate(decoded.state, decoded.issues);
            Map<String, Object> data = validation.valid
                    ? validationData(true, PROFILE, digestSemanticState(decoded.state))
                    : validationData(false, PROFILE, null);
            Map<String, Object> result;
            if (validation.valid) {
                result = result("validate", "succeeded", binding, projectIo(input, transport.target),
                        observations(decoded.normalizations, Collections.<Map<String, Object>>emptyList()),
                        Collections.<Map<String, Object>>emptyList(), data);
            } else {
                List<Map<String, Object>> diagnostics = new ArrayList<Map<String, Object>>();
                List<Map<String, Object>> unsupported = new ArrayList<Map<String, Object>>();
                for (Issue issue : validation.issues) {
                    diagnostics.add(diagnostic(issue.code, issue.message, "semantic", issue.path, "--project",
                            "external_project", issue.ruleId, "after-input-change", issue.details));
                    if ("semantic.unsupported".equals(issue.code)) {
                        unsupported.add(observation(issue.code, issue.path, issue.message));
                    }
                }
                result = result("validate", "rejected", binding, projectIo(input, transport.target),
                        observations(decoded.normalizations, unsupported), diagnostics, data);
            }
            return transport.write(result);
        } catch (V1Failure failure) {
            Map<String, Object> data = "rejected".equals(failure.status) ? validationData(false, null, null) : null;
            return transport.write(errorResult("validate", failure, binding,
                    input == null ? unreadProjectIo(invocation.project, transport.target) : projectIo(input, transport.target), data));
        } catch (RuntimeException failure) {
            V1Failure wrapped = failure("internal.unexpected-error", "internal",
                    "The v1 validate service encountered an unexpected internal error.", null, null, null,
                    "not-retryable", Collections.<String, Object>emptyMap(), "runtime-error");
            return transport.write(errorResult("validate", wrapped, binding,
                    input == null ? unreadProjectIo(invocation.project, transport.target) : projectIo(input, transport.target), null));
        }
    }

    /** A runtime manifest verifier must create this value after digest checks. */
    public static final class VerifiedRuntime {
        public final String version;
        public final String artifactDigest;
        public final String manifestDigest;

        public VerifiedRuntime(String version, String artifactDigest, String manifestDigest) {
            this.version = version;
            this.artifactDigest = artifactDigest;
            this.manifestDigest = manifestDigest;
        }
    }

    private static Invocation parseInvocation(String[] argv) throws V1Failure {
        if (argv.length == 0) {
            throw usage("cli.unknown-command", "A v1 command is required.", null, null);
        }
        if (!"validate".equals(argv[0])) {
            if (argv[0] != null && argv[0].startsWith("--") && !"--".equals(argv[0])) {
                throw usage("cli.unknown-option", "Unsupported v1 option: " + argv[0], argv[0], null);
            }
            throw usage("cli.unknown-command", "Unsupported v1 command: " + argv[0], null, null);
        }
        Map<String, String> options = new LinkedHashMap<String, String>();
        for (int index = 1; index < argv.length; index++) {
            String token = argv[index];
            if (token == null || !token.startsWith("--") || "--".equals(token)) {
                throw usage("cli.unexpected-argument", "Unexpected positional argument: " + String.valueOf(token), null, null);
            }
            if (token.indexOf('=') >= 0 || !("--project".equals(token) || "--result".equals(token))) {
                throw usage("cli.unknown-option", "Unsupported option for validate: " + token, token, null);
            }
            if (options.containsKey(token)) {
                throw usage("cli.duplicate-option", "Option " + token + " must not be repeated.", token, null);
            }
            if (index + 1 >= argv.length || argv[index + 1] == null || argv[index + 1].startsWith("--")) {
                throw usage("cli.missing-option", "Option " + token + " requires a value.", token, null);
            }
            String value = argv[++index];
            if (value.length() == 0 || value.indexOf('\0') >= 0) {
                throw usage("cli.invalid-option-value", "Option " + token + " has an invalid value.", token, null);
            }
            options.put(token, value);
        }
        if (!options.containsKey("--project")) {
            throw usage("cli.missing-option", "Command validate requires --project.", "--project", null);
        }
        return new Invocation(options.get("--project"), options.containsKey("--result") ? options.get("--result") : "-");
    }

    private static ResultTransport reserveResult(String requested, OutputStream stdout) throws V1Failure {
        if ("-".equals(requested)) {
            return new ResultTransport(stdoutTarget(), stdout, null, null);
        }
        if (requested.length() == 0 || requested.indexOf('\0') >= 0 || requested.endsWith("/")) {
            throw failure("io.result-path-unsafe", "filesystem", "--result must name a normal new file.", null,
                    "--result", null, "after-input-change", details("requested_path", requested), "rejected");
        }
        Path requestedPath = Paths.get(requested).toAbsolutePath().normalize();
        Path parent = requestedPath.getParent();
        if (parent == null || !Files.isDirectory(parent, LinkOption.NOFOLLOW_LINKS)) {
            throw failure("io.result-path-unsafe", "filesystem", "The parent directory for --result must already exist.", null,
                    "--result", null, "after-input-change", details("requested_path", requested), "rejected");
        }
        try {
            Path canonicalParent = parent.toRealPath(LinkOption.NOFOLLOW_LINKS);
            Path path = canonicalParent.resolve(requestedPath.getFileName().toString());
            OutputStream output = Files.newOutputStream(path, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            return new ResultTransport(fileTarget(path), stdout, output, path);
        } catch (FileAlreadyExistsException error) {
            throw failure("io.result-path-exists", "filesystem", "The --result path already exists and will not be overwritten.",
                    requestedPath.toString(), "--result", null, "after-input-change", details("requested_path", requested), "rejected");
        } catch (IOException error) {
            throw failure("io.result-reservation-failed", "filesystem", "The v1 result file could not be reserved exclusively.",
                    requestedPath.toString(), "--result", null, "after-environment-change", details("requested_path", requested), "runtime-error");
        }
    }

    private static InputData readProject(String requested, InputStream stdin) throws V1Failure {
        if ("-".equals(requested)) {
            try {
                byte[] bytes = readAll(stdin);
                return new InputData("stdin", null, bytes);
            } catch (IOException error) {
                throw failure("io.input-read-failed", "stdin", "The v1 project input could not be read from stdin.", null,
                        "--project", "external_project", "after-environment-change", Collections.<String, Object>emptyMap(), "runtime-error");
            }
        }
        Path path = Paths.get(requested).toAbsolutePath().normalize();
        try {
            if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
                throw failure("io.input-not-found", "filesystem", "The v1 project input does not exist.", path.toString(),
                        "--project", "external_project", "after-input-change", details("requested_path", requested), "rejected");
            }
            if (Files.isSymbolicLink(path)) {
                throw failure("io.input-symlink-rejected", "filesystem", "The v1 project input must not be a symbolic link.",
                        path.toString(), "--project", "external_project", "after-input-change", details("requested_path", requested), "rejected");
            }
            if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
                throw failure("io.input-type-invalid", "filesystem",
                        "P5-C1 validate accepts a direct regular XML file or stdin; artifact-set directories wait for P5-C5.",
                        path.toString(), "--project", "external_project", "after-input-change", details("requested_path", requested), "rejected");
            }
            Path canonical = path.toRealPath(LinkOption.NOFOLLOW_LINKS);
            return new InputData("file", canonical.toString(), Files.readAllBytes(canonical));
        } catch (V1Failure failure) {
            throw failure;
        } catch (IOException error) {
            throw failure("io.input-read-failed", "filesystem", "The v1 project input could not be read.", path.toString(),
                    "--project", "external_project", "after-environment-change", details("requested_path", requested), "runtime-error");
        }
    }

    private static DecodedProject decode(byte[] bytes) throws V1Failure {
        List<Map<String, Object>> normalizations = new ArrayList<Map<String, Object>>();
        int offset = startsWithBom(bytes) ? 3 : 0;
        if (offset == 3) {
            normalizations.add(normalization("text.utf8-bom-removed", "Project", "utf8-bom", "no-bom"));
        }
        String xml;
        try {
            CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT);
            CharBuffer decoded = decoder.decode(ByteBuffer.wrap(bytes, offset, bytes.length - offset));
            xml = decoded.toString();
        } catch (CharacterCodingException error) {
            throw failure("text.invalid-utf8", "input", "Project XML must be valid UTF-8.", null, "--project",
                    "external_project", "after-input-change", Collections.<String, Object>emptyMap(), "rejected");
        }
        Matcher declaration = Pattern.compile("^\\s*<\\?xml\\s+([^?]*)\\?>", Pattern.CASE_INSENSITIVE).matcher(xml);
        if (declaration.find()) {
            Matcher encoding = Pattern.compile("\\bencoding\\s*=\\s*(['\"])(.*?)\\1", Pattern.CASE_INSENSITIVE)
                    .matcher(declaration.group(1));
            if (encoding.find() && !"UTF-8".equalsIgnoreCase(encoding.group(2))) {
                throw failure("xml.encoding-unsupported", "input", "Project XML declarations may specify only UTF-8 encoding.",
                        "Project", "--project", "external_project", "after-input-change", details("encoding", encoding.group(2)), "rejected");
            }
        }
        Document document = parseXml(xml);
        Element root = document.getDocumentElement();
        if (root == null || !"Project".equals(localName(root)) || !PROJECT_NAMESPACE.equals(root.getNamespaceURI())) {
            throw failure("xml.profile-unsupported", "input",
                    "Project XML root must be Project in the Microsoft Project namespace.", "Project", "--project",
                    "external_project", "after-input-change", Collections.<String, Object>emptyMap(), "rejected");
        }
        assertAttributes(root, "Project");
        List<Issue> issues = new ArrayList<Issue>();
        Map<String, List<Element>> project = children(root, PROJECT_CHILDREN, Collections.<String>emptySet(), "Project", issues,
                "project");
        Map<String, Object> state = state(project, issues);
        return new DecodedProject(state, issues, normalizations);
    }

    private static Document parseXml(String xml) throws V1Failure {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new DefaultHandler() {
                @Override
                public void error(SAXParseException error) throws SAXException {
                    throw error;
                }

                @Override
                public void fatalError(SAXParseException error) throws SAXException {
                    throw error;
                }
            });
            return builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        } catch (ParserConfigurationException error) {
            throw failure("internal.unexpected-error", "internal", "Secure XML parsing could not be configured.", null, null, null,
                    "not-retryable", Collections.<String, Object>emptyMap(), "runtime-error");
        } catch (SAXException error) {
            throw failure("xml.invalid", "input", "Project XML is not well-formed.", "Project", "--project", "external_project",
                    "after-input-change", Collections.<String, Object>emptyMap(), "rejected");
        } catch (IOException error) {
            throw failure("xml.invalid", "input", "Project XML could not be parsed.", "Project", "--project", "external_project",
                    "after-input-change", Collections.<String, Object>emptyMap(), "rejected");
        }
    }

    private static Map<String, Object> state(Map<String, List<Element>> project, List<Issue> issues) throws V1Failure {
        Map<String, Object> state = linked();
        state.put("kind", "miku_project_semantic_state");
        state.put("schema_version", "1");
        state.put("semantic_contract_version", "1");
        Map<String, Object> projectState = linked();
        copy(projectState, "name", text(project.get("Name")));
        copy(projectState, "start", text(project.get("StartDate")));
        copy(projectState, "finish", text(project.get("FinishDate")));
        copy(projectState, "current_date", text(project.get("CurrentDate")));
        copy(projectState, "schedule_from_start", booleanValue(text(project.get("ScheduleFromStart"))));
        copy(projectState, "calendar_uid", identity(text(project.get("CalendarUID"))));
        state.put("project", projectState);
        TaskBundle tasks = tasks(project.get("Tasks"), issues);
        state.put("tasks", tasks.tasks);
        state.put("dependencies", tasks.dependencies);
        state.put("resources", resources(project.get("Resources"), issues));
        state.put("assignments", assignments(project.get("Assignments"), issues));
        state.put("calendars", calendars(project.get("Calendars"), issues));
        return state;
    }

    private static TaskBundle tasks(List<Element> containers, List<Issue> issues) throws V1Failure {
        List<Element> elements = collection(containers, "Tasks", "Task");
        List<TaskSource> decoded = new ArrayList<TaskSource>();
        for (int index = 0; index < elements.size(); index++) {
            Element element = elements.get(index);
            String sourceUid = identity(directChildText(element, "UID"));
            Map<String, List<Element>> values = children(element, TASK_CHILDREN, setOf("PredecessorLink"),
                    "Tasks/Task[" + (index + 1) + "]", issues, taskPath(sourceUid, index));
            TaskSource source = new TaskSource();
            source.uid = identity(text(values.get("UID")));
            source.id = integer(text(values.get("ID")));
            source.name = text(values.get("Name"));
            source.level = integer(text(values.get("OutlineLevel")));
            source.outlineNumber = text(values.get("OutlineNumber"));
            source.start = text(values.get("Start"));
            source.finish = text(values.get("Finish"));
            source.duration = duration(text(values.get("Duration")));
            source.milestone = booleanValue(text(values.get("Milestone")));
            source.summary = booleanValue(text(values.get("Summary")));
            source.percent = integer(text(values.get("PercentComplete")));
            source.calendarUid = identity(text(values.get("CalendarUID")));
            source.dependencies = predecessors(values.get("PredecessorLink"), source.uid, issues);
            Long sourceId = integerValue(source.id);
            if (source.id != null && (sourceId == null || sourceId.longValue() <= 0)) {
                issues.add(invalid("S-I003", taskPath(source.uid, index) + ".id", "Task ID must be a positive integer when present."));
            }
            decoded.add(source);
        }
        List<Map<String, Object>> tasks = new ArrayList<Map<String, Object>>();
        List<Map<String, Object>> dependencies = new ArrayList<Map<String, Object>>();
        List<String> levelStack = new ArrayList<String>();
        List<Integer> sibling = new ArrayList<Integer>();
        Set<Long> ids = new HashSet<Long>();
        boolean pseudo = false;
        for (int index = 0; index < decoded.size(); index++) {
            TaskSource source = decoded.get(index);
            Long sourceId = integerValue(source.id);
            if (sourceId != null && !ids.add(sourceId)) {
                issues.add(invalid("S-I003", taskPath(source.uid, index) + ".id", "Task ID must be unique when present."));
            }
            Long level = integerValue(source.level);
            if ("0".equals(source.uid)) {
                if (index != 0 || pseudo || level == null || level.longValue() != 0 || !Boolean.TRUE.equals(source.summary)) {
                    issues.add(invalid("S-I003", "tasks[uid=0]", "The project summary pseudo task is not in its required form."));
                }
                pseudo = true;
                continue;
            }
            String parent = null;
            if (level == null || level.longValue() < 1) {
                issues.add(invalid("S-I003", taskPath(source.uid, index), "Task outline level must begin at 1 for a semantic task."));
            } else if (tasks.isEmpty() && level.longValue() != 1) {
                issues.add(invalid("S-I003", taskPath(source.uid, index), "The first semantic task must be a root task."));
            } else if (!tasks.isEmpty() && level.longValue() > levelStack.size() + 1L) {
                issues.add(invalid("S-I003", taskPath(source.uid, index), "Task outline levels may increase by at most one."));
            } else if (level.longValue() > 1) {
                int parentIndex = (int) level.longValue() - 2;
                parent = parentIndex < levelStack.size() ? levelStack.get(parentIndex) : null;
                if (parent == null) {
                    issues.add(invalid("S-I003", taskPath(source.uid, index), "Task outline parent is missing."));
                }
            }
            // A malformed outline level must still reproduce ordinary Node diagnostics, but it
            // must not turn a single adversarial number into an unbounded allocation.  The
            // decoded member count is already materialized and is a sufficient upper bound for
            // any meaningful outline depth in this input.
            boolean materializableLevel = level != null && level.longValue() >= 1
                    && level.longValue() <= decoded.size();
            int levelIndex = materializableLevel ? (int) level.longValue() : 0;
            if (levelIndex > 0) {
                while (sibling.size() > levelIndex) {
                    sibling.remove(sibling.size() - 1);
                }
                while (sibling.size() < levelIndex) {
                    sibling.add(Integer.valueOf(0));
                }
                int newValue = sibling.get(levelIndex - 1).intValue() + 1;
                sibling.set(levelIndex - 1, Integer.valueOf(newValue));
                String expected = joinOutline(sibling);
                if (source.outlineNumber != null && !source.outlineNumber.equals(expected)) {
                    issues.add(invalid("S-I003", taskPath(source.uid, index) + ".outline_number",
                            "Task OutlineNumber must match the derived sibling position."));
                }
            }
            int newSize = levelIndex == 0 ? 0 : levelIndex - 1;
            while (levelStack.size() > newSize) {
                levelStack.remove(levelStack.size() - 1);
            }
            levelStack.add(source.uid);
            Map<String, Object> task = linked();
            copy(task, "uid", source.uid);
            copy(task, "name", source.name);
            task.put("parent_uid", parent);
            copy(task, "start", source.start);
            copy(task, "finish", source.finish);
            copy(task, "duration", source.duration);
            copy(task, "milestone", source.milestone);
            copy(task, "summary", source.summary);
            copy(task, "percent_complete", source.percent);
            copy(task, "calendar_uid", source.calendarUid);
            tasks.add(task);
            for (Map<String, Object> dependency : source.dependencies) {
                dependency.put("successor_uid", source.uid);
                dependencies.add(dependency);
            }
        }
        return new TaskBundle(tasks, dependencies);
    }

    private static List<Map<String, Object>> predecessors(List<Element> elements, String successorUid, List<Issue> issues)
            throws V1Failure {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        if (elements == null) {
            return result;
        }
        for (int index = 0; index < elements.size(); index++) {
            Element element = elements.get(index);
            String predecessorUid = identity(directChildText(element, "PredecessorUID"));
            Map<String, List<Element>> values = children(element, PREDECESSOR_CHILDREN, Collections.<String>emptySet(),
                    "Task/PredecessorLink[" + (index + 1) + "]", issues,
                    dependencyPath(predecessorUid, successorUid, index));
            String predecessor = identity(text(values.get("PredecessorUID")));
            String path = dependencyPath(predecessor, successorUid, index);
            Object type = integer(text(values.get("Type")));
            String rawLag = text(values.get("LinkLag"));
            Object lagFormat = integer(text(values.get("LagFormat")));
            String lag = rawLag;
            if ("0".equals(rawLag) && Long.valueOf(3).equals(lagFormat)) {
                lag = "PT0H0M0S";
            } else if ("PT0H0M0S".equals(rawLag) && lagFormat == null) {
                lag = "PT0H0M0S";
            } else if (rawLag != null) {
                issues.add(unsupported("S-I019", path + ".lag", "Only FS dependencies with zero lag are supported."));
            }
            if (type != null && !Long.valueOf(1).equals(type)) {
                issues.add(unsupported("S-I019", path + ".type", "Only FS dependencies are supported."));
            }
            Map<String, Object> dependency = linked();
            copy(dependency, "predecessor_uid", predecessor);
            if (type != null) {
                dependency.put("type", Long.valueOf(1).equals(type) ? "FS" : type);
            }
            copy(dependency, "lag", lag);
            result.add(dependency);
        }
        return result;
    }

    private static List<Map<String, Object>> resources(List<Element> containers, List<Issue> issues) throws V1Failure {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        List<Element> elements = collection(containers, "Resources", "Resource");
        for (int index = 0; index < elements.size(); index++) {
            Element element = elements.get(index);
            String sourceUid = identity(directChildText(element, "UID"));
            Map<String, List<Element>> values = children(element, RESOURCE_CHILDREN, Collections.<String>emptySet(), "Resources/Resource", issues,
                    resourcePath(sourceUid, index));
            String uid = identity(text(values.get("UID")));
            Map<String, Object> resource = linked();
            copy(resource, "uid", uid);
            copy(resource, "name", text(values.get("Name")));
            Object type = integer(text(values.get("Type")));
            if (type != null) {
                String value = Long.valueOf(0).equals(type) ? "material"
                        : Long.valueOf(1).equals(type) ? "work" : Long.valueOf(2).equals(type) ? "cost" : null;
                if (value == null) {
                    issues.add(unsupported("S-I020", resourcePath(uid, index) + ".type",
                            "This external resource type is outside the v1 XML subset."));
                } else {
                    resource.put("type", value);
                }
            }
            copy(resource, "calendar_uid", identity(text(values.get("CalendarUID"))));
            result.add(resource);
        }
        return result;
    }

    private static List<Map<String, Object>> assignments(List<Element> containers, List<Issue> issues) throws V1Failure {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        List<Element> elements = collection(containers, "Assignments", "Assignment");
        for (int index = 0; index < elements.size(); index++) {
            Element element = elements.get(index);
            String sourceUid = identity(directChildText(element, "UID"));
            Map<String, List<Element>> values = children(element, ASSIGNMENT_CHILDREN, Collections.<String>emptySet(), "Assignments/Assignment", issues,
                    assignmentPath(sourceUid, index));
            Map<String, Object> assignment = linked();
            copy(assignment, "uid", identity(text(values.get("UID"))));
            copy(assignment, "task_uid", identity(text(values.get("TaskUID"))));
            String resource = text(values.get("ResourceUID"));
            if (!"-65535".equals(resource)) {
                copy(assignment, "resource_uid", identity(resource));
            }
            copy(assignment, "start", text(values.get("Start")));
            copy(assignment, "finish", text(values.get("Finish")));
            copy(assignment, "units", units(text(values.get("Units"))));
            copy(assignment, "work", duration(text(values.get("Work"))));
            result.add(assignment);
        }
        return result;
    }

    private static List<Map<String, Object>> calendars(List<Element> containers, List<Issue> issues) throws V1Failure {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        List<Element> elements = collection(containers, "Calendars", "Calendar");
        for (int index = 0; index < elements.size(); index++) {
            Element element = elements.get(index);
            String sourceUid = identity(directChildText(element, "UID"));
            Map<String, List<Element>> values = children(element, CALENDAR_CHILDREN, Collections.<String>emptySet(), "Calendars/Calendar", issues,
                    calendarPath(sourceUid, index));
            Map<String, Object> calendar = linked();
            copy(calendar, "uid", identity(text(values.get("UID"))));
            copy(calendar, "name", text(values.get("Name")));
            copy(calendar, "is_base_calendar", booleanValue(text(values.get("IsBaseCalendar"))));
            result.add(calendar);
        }
        return result;
    }

    private static List<Element> collection(List<Element> containers, String containerName, String memberName) throws V1Failure {
        if (containers == null || containers.isEmpty()) {
            return Collections.emptyList();
        }
        List<Element> members = new ArrayList<Element>();
        for (Element container : containers) {
            Map<String, List<Element>> values = children(container, setOf(memberName), setOf(memberName), containerName,
                    new ArrayList<Issue>(), null);
            members.addAll(values.get(memberName) == null ? Collections.<Element>emptyList() : values.get(memberName));
        }
        if (members.isEmpty()) {
            throw failure("xml.invalid", "input", containerName + " must not be empty when present.", containerName, "--project",
                    "external_project", "after-input-change", Collections.<String, Object>emptyMap(), "rejected");
        }
        return members;
    }

    private static Map<String, List<Element>> children(Element parent, Set<String> allowed, Set<String> repeating, String path,
            List<Issue> issues, String unknownDomain) throws V1Failure {
        assertNamespace(parent, path);
        assertAttributes(parent, path);
        Map<String, List<Element>> values = new LinkedHashMap<String, List<Element>>();
        List<String> unknownNames = new ArrayList<String>();
        NodeList nodes = parent.getChildNodes();
        for (int index = 0; index < nodes.getLength(); index++) {
            Node node = nodes.item(index);
            if (node.getNodeType() == Node.TEXT_NODE || node.getNodeType() == Node.CDATA_SECTION_NODE) {
                if (!node.getNodeValue().trim().isEmpty()) {
                    throw failure("xml.invalid", "input", path + " contains text where child elements are required.", path, "--project",
                            "external_project", "after-input-change", Collections.<String, Object>emptyMap(), "rejected");
                }
                continue;
            }
            if (node.getNodeType() == Node.COMMENT_NODE || node.getNodeType() == Node.PROCESSING_INSTRUCTION_NODE) {
                continue;
            }
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                throw failure("xml.invalid", "input", path + " contains an invalid XML node.", path, "--project", "external_project",
                        "after-input-change", Collections.<String, Object>emptyMap(), "rejected");
            }
            Element child = (Element) node;
            assertNamespace(child, path);
            assertAttributes(child, path + "/" + localName(child));
            String name = localName(child);
            if (!allowed.contains(name)) {
                if (unknownDomain == null) {
                    throw failure("xml.profile-unsupported", "input", path + " contains an unsupported member.", path, "--project",
                            "external_project", "after-input-change", Collections.<String, Object>emptyMap(), "rejected");
                }
                unknownNames.add(name);
                continue;
            }
            List<Element> occurrences = values.get(name);
            if (occurrences == null) {
                occurrences = new ArrayList<Element>();
                values.put(name, occurrences);
            }
            occurrences.add(child);
            if (!repeating.contains(name) && occurrences.size() > 1) {
                throw failure("xml.invalid", "input", path + "/" + name + " must not be repeated.", path + "/" + name,
                        "--project", "external_project", "after-input-change", Collections.<String, Object>emptyMap(), "rejected");
            }
        }
        for (String name : unknownNames) {
            issues.add(unsupported("S-I020", unknownDomain + "." + snake(name), name + " is outside the v1 XML subset."));
        }
        return values;
    }

    private static void assertNamespace(Element element, String path) throws V1Failure {
        if (!PROJECT_NAMESPACE.equals(element.getNamespaceURI())) {
            throw failure("xml.profile-unsupported", "input", path + " uses a namespace outside the v1 XML subset.", path,
                    "--project", "external_project", "after-input-change", Collections.<String, Object>emptyMap(), "rejected");
        }
    }

    private static void assertAttributes(Element element, String path) throws V1Failure {
        NamedNodeMap attributes = element.getAttributes();
        for (int index = 0; index < attributes.getLength(); index++) {
            Node attribute = attributes.item(index);
            if (!"http://www.w3.org/2000/xmlns/".equals(attribute.getNamespaceURI()) && !"xmlns".equals(attribute.getNodeName())) {
                throw failure("xml.profile-unsupported", "input", path + " contains an unsupported attribute.", path, "--project",
                        "external_project", "after-input-change", details("attribute", attribute.getNodeName()), "rejected");
            }
        }
    }

    private static String text(List<Element> values) throws V1Failure {
        if (values == null || values.isEmpty()) {
            return null;
        }
        Element element = values.get(0);
        NodeList nodes = element.getChildNodes();
        for (int index = 0; index < nodes.getLength(); index++) {
            Node node = nodes.item(index);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                throw failure("xml.invalid", "input", localName(element) + " must contain text only.", localName(element), "--project",
                        "external_project", "after-input-change", Collections.<String, Object>emptyMap(), "rejected");
            }
        }
        return element.getTextContent();
    }

    /** Reads one direct leaf for diagnostic identity selection; validation still reads the checked child map. */
    private static String directChildText(Element parent, String wantedName) {
        NodeList nodes = parent.getChildNodes();
        for (int index = 0; index < nodes.getLength(); index++) {
            Node node = nodes.item(index);
            if (node.getNodeType() == Node.ELEMENT_NODE && wantedName.equals(localName((Element) node))) {
                return ((Element) node).getTextContent();
            }
        }
        return null;
    }

    private static Validation validate(Map<String, Object> state, List<Issue> adapterIssues) {
        List<Issue> issues = new ArrayList<Issue>(adapterIssues);
        Map<String, Object> project = object(state.get("project"));
        requiredText(project, "name", "project", issues);
        requiredDate(project, "start", "project", issues);
        requiredDate(project, "finish", "project", issues);
        if (date((String) project.get("start")) && date((String) project.get("finish"))
                && ((String) project.get("start")).compareTo((String) project.get("finish")) > 0) {
            issues.add(invalid("S-I006", "project.start", "Project start must not be after project finish."));
        }
        optionalDate(project, "current_date", "project", issues);
        optionalBoolean(project, "schedule_from_start", "project", issues);
        optionalIdentity(project, "calendar_uid", "project", issues);
        List<Map<String, Object>> tasks = maps(state.get("tasks"));
        Set<String> taskUids = validateTasks(tasks, issues);
        validateDependencies(maps(state.get("dependencies")), taskUids, issues);
        Set<String> resourceUids = validateUids(maps(state.get("resources")), "resources", "S-I005", issues);
        validateResources(maps(state.get("resources")), issues);
        validateAssignments(maps(state.get("assignments")), taskUids, resourceUids, issues);
        Set<String> calendarUids = validateUids(maps(state.get("calendars")), "calendars", "S-I005", issues);
        validateCalendars(maps(state.get("calendars")), issues);
        validateCalendarReferences(project, tasks, maps(state.get("resources")), calendarUids, issues);
        Collections.sort(issues, ISSUE_ORDER);
        return new Validation(issues.isEmpty(), issues);
    }

    private static Set<String> validateTasks(List<Map<String, Object>> tasks, List<Issue> issues) {
        Set<String> uids = new HashSet<String>();
        Map<String, Integer> children = new HashMap<String, Integer>();
        List<String> active = new ArrayList<String>();
        for (int index = 0; index < tasks.size(); index++) {
            Map<String, Object> task = tasks.get(index);
            String uid = string(task.get("uid"));
            String path = taskPath(uid, index);
            if (!identityValid(uid) || !uids.add(uid)) {
                issues.add(invalid("S-I001", path + ".uid", "Task UID must be a unique v1 identity token."));
            } else {
                children.put(uid, Integer.valueOf(0));
            }
            requiredText(task, "name", path, issues);
            requiredDate(task, "start", path, issues);
            requiredDate(task, "finish", path, issues);
            if (date(string(task.get("start"))) && date(string(task.get("finish")))
                    && string(task.get("start")).compareTo(string(task.get("finish"))) > 0) {
                issues.add(invalid("S-I007", path + ".start", "Task start must not be after task finish."));
            }
            requiredDuration(task, "duration", path, issues);
            requiredBoolean(task, "milestone", path, issues);
            requiredBoolean(task, "summary", path, issues);
            Object percent = task.get("percent_complete");
            if (!task.containsKey("percent_complete")) {
                issues.add(invalid("S-I008", path + ".percent_complete", "percent_complete is required."));
            } else if (!(percent instanceof Long)) {
                issues.add(invalid("S-I013", path + ".percent_complete", "percent_complete must be an integer."));
            } else if (((Long) percent).longValue() < 0) {
                issues.add(invalid("S-I011", path + ".percent_complete", "percent_complete must not be negative."));
            } else if (((Long) percent).longValue() > 100) {
                issues.add(invalid("S-I012", path + ".percent_complete", "percent_complete must not exceed 100."));
            }
            optionalIdentity(task, "calendar_uid", path, issues);
            Object parent = task.get("parent_uid");
            if (parent == null) {
                active.clear();
            } else if (!(parent instanceof String) || !identityValid((String) parent) || parent.equals(uid)) {
                issues.add(invalid("S-I003", path + ".parent_uid", "Task parent must be a preceding active ancestor."));
                active.clear();
            } else {
                int parentIndex = active.lastIndexOf(parent);
                if (parentIndex < 0 || !uids.contains(parent)) {
                    issues.add(invalid("S-I003", path + ".parent_uid", "Task parent must be a preceding active ancestor."));
                    active.clear();
                } else {
                    while (active.size() > parentIndex + 1) {
                        active.remove(active.size() - 1);
                    }
                    children.put((String) parent, Integer.valueOf(children.get(parent).intValue() + 1));
                }
            }
            if (identityValid(uid)) {
                active.add(uid);
            }
        }
        for (int index = 0; index < tasks.size(); index++) {
            Map<String, Object> task = tasks.get(index);
            String uid = string(task.get("uid"));
            if (identityValid(uid) && task.get("summary") instanceof Boolean
                    && ((Boolean) task.get("summary")).booleanValue() != (children.get(uid).intValue() > 0)) {
                issues.add(invalid("S-I004", taskPath(uid, index) + ".summary", "Task summary must exactly match whether it has children."));
            }
            String start = string(task.get("start"));
            String finish = string(task.get("finish"));
            if (Boolean.TRUE.equals(task.get("milestone"))
                    && (!same(start, finish) || !"PT0H0M0S".equals(task.get("duration")))) {
                issues.add(invalid("S-I010", taskPath(uid, index), "A milestone must have equal start/finish and zero duration."));
            }
        }
        return uids;
    }

    private static void validateDependencies(List<Map<String, Object>> dependencies, Set<String> taskUids, List<Issue> issues) {
        Set<String> tuples = new HashSet<String>();
        Map<String, Set<String>> adjacency = new HashMap<String, Set<String>>();
        for (int index = 0; index < dependencies.size(); index++) {
            Map<String, Object> dependency = dependencies.get(index);
            String predecessor = string(dependency.get("predecessor_uid"));
            String successor = string(dependency.get("successor_uid"));
            String path = dependencyPath(predecessor, successor, index);
            if (!identityValid(predecessor) || !identityValid(successor)) {
                issues.add(invalid("S-I008", path, "Dependency endpoints are required."));
                continue;
            }
            if (!taskUids.contains(predecessor) || !taskUids.contains(successor)) {
                issues.add(invalid("S-I014", path, "Dependency endpoints must reference existing tasks."));
            }
            if (predecessor != null && predecessor.equals(successor)) {
                issues.add(invalid("S-I015", path, "A dependency cannot reference the same task twice."));
            }
            if (!"FS".equals(dependency.get("type")) || !"PT0H0M0S".equals(dependency.get("lag"))) {
                issues.add(unsupported("S-I019", path, "Only FS dependencies with zero lag are supported."));
            }
            String tuple = String.valueOf(predecessor) + "\u0000" + String.valueOf(successor) + "\u0000" + dependency.get("type") + "\u0000" + dependency.get("lag");
            if (!tuples.add(tuple)) {
                issues.add(invalid("S-I025", path, "Duplicate dependency tuples are not allowed."));
            }
            if (taskUids.contains(predecessor) && taskUids.contains(successor) && !predecessor.equals(successor)) {
                Set<String> successors = adjacency.get(predecessor);
                if (successors == null) {
                    successors = new HashSet<String>();
                    adjacency.put(predecessor, successors);
                }
                successors.add(successor);
            }
        }
        if (hasCycle(adjacency)) {
            issues.add(invalid("S-I016", "dependencies", "Dependency graph must be acyclic."));
        }
    }

    private static Set<String> validateUids(List<Map<String, Object>> values, String domain, String ruleId, List<Issue> issues) {
        Set<String> uids = new HashSet<String>();
        for (int index = 0; index < values.size(); index++) {
            String uid = string(values.get(index).get("uid"));
            if (!identityValid(uid) || !uids.add(uid)) {
                issues.add(invalid(ruleId, memberPath(domain, uid, index) + ".uid", domain + " UID must be unique and valid."));
            }
        }
        return uids;
    }

    private static void validateResources(List<Map<String, Object>> resources, List<Issue> issues) {
        for (int index = 0; index < resources.size(); index++) {
            Map<String, Object> value = resources.get(index);
            String path = resourcePath(string(value.get("uid")), index);
            optionalText(value, "name", path, issues);
            if (value.containsKey("type") && !("work".equals(value.get("type")) || "material".equals(value.get("type")) || "cost".equals(value.get("type")))) {
                issues.add(invalid("S-I024", path + ".type", "Resource type must be work, material, or cost."));
            }
        }
    }

    private static void validateAssignments(List<Map<String, Object>> assignments, Set<String> tasks, Set<String> resources,
            List<Issue> issues) {
        Set<String> uids = validateUids(assignments, "assignments", "S-I005", issues);
        for (int index = 0; index < assignments.size(); index++) {
            Map<String, Object> value = assignments.get(index);
            String path = assignmentPath(string(value.get("uid")), index);
            String taskUid = string(value.get("task_uid"));
            if (!identityValid(taskUid)) {
                issues.add(invalid("S-I008", path + ".task_uid", "Assignment task UID is required."));
            } else if (!tasks.contains(taskUid)) {
                issues.add(invalid("S-I017", path + ".task_uid", "Assignment must reference an existing task."));
            }
            if (value.containsKey("resource_uid")) {
                String resourceUid = string(value.get("resource_uid"));
                if (!identityValid(resourceUid) || !resources.contains(resourceUid)) {
                    issues.add(invalid("S-I017", path + ".resource_uid", "Assignment resource must reference an existing resource."));
                }
            }
            optionalDate(value, "start", path, issues);
            optionalDate(value, "finish", path, issues);
            if (date(string(value.get("start"))) && date(string(value.get("finish")))
                    && string(value.get("start")).compareTo(string(value.get("finish"))) > 0) {
                issues.add(invalid("S-I017", path + ".start", "Assignment start must not be after assignment finish."));
            }
            if (value.containsKey("units") && !unitsValid(string(value.get("units")))) {
                issues.add(invalid("S-I024", path + ".units", "Assignment units must be a non-negative decimal string."));
            }
            if (value.containsKey("work") && !durationValid(string(value.get("work")))) {
                issues.add(invalid("S-I009", path + ".work", "work must be a non-negative working duration."));
            }
        }
    }

    private static void validateCalendars(List<Map<String, Object>> calendars, List<Issue> issues) {
        for (int index = 0; index < calendars.size(); index++) {
            Map<String, Object> value = calendars.get(index);
            String path = calendarPath(string(value.get("uid")), index);
            optionalText(value, "name", path, issues);
            optionalBoolean(value, "is_base_calendar", path, issues);
        }
    }

    private static void validateCalendarReferences(Map<String, Object> project, List<Map<String, Object>> tasks,
            List<Map<String, Object>> resources, Set<String> calendars, List<Issue> issues) {
        calendarReference(project, "project", calendars, issues);
        for (int index = 0; index < tasks.size(); index++) {
            calendarReference(tasks.get(index), taskPath(string(tasks.get(index).get("uid")), index), calendars, issues);
        }
        for (int index = 0; index < resources.size(); index++) {
            calendarReference(resources.get(index), resourcePath(string(resources.get(index).get("uid")), index), calendars, issues);
        }
    }

    private static void calendarReference(Map<String, Object> value, String path, Set<String> calendars, List<Issue> issues) {
        if (value.containsKey("calendar_uid") && (!identityValid(string(value.get("calendar_uid"))) || !calendars.contains(value.get("calendar_uid")))) {
            issues.add(invalid("S-I018", path + ".calendar_uid", "Calendar reference must identify an existing calendar."));
        }
    }

    private static void requiredText(Map<String, Object> value, String key, String path, List<Issue> issues) {
        if (!value.containsKey(key)) {
            issues.add(invalid("S-I008", path + "." + key, key + " is required."));
        } else if (!textValid(string(value.get(key)))) {
            issues.add(invalid("S-I024", path + "." + key, key + " must be non-empty text."));
        }
    }

    private static void optionalText(Map<String, Object> value, String key, String path, List<Issue> issues) {
        if (value.containsKey(key) && !textValid(string(value.get(key)))) {
            issues.add(invalid("S-I024", path + "." + key, key + " must be non-empty text when present."));
        }
    }

    private static void requiredDate(Map<String, Object> value, String key, String path, List<Issue> issues) {
        if (!value.containsKey(key)) {
            issues.add(invalid("S-I008", path + "." + key, key + " is required."));
        } else if (!date(string(value.get(key)))) {
            issues.add(invalid("S-I024", path + "." + key, key + " must be a valid local civil datetime."));
        }
    }

    private static void optionalDate(Map<String, Object> value, String key, String path, List<Issue> issues) {
        if (value.containsKey(key) && !date(string(value.get(key)))) {
            issues.add(invalid("S-I024", path + "." + key, key + " must be a valid local civil datetime when present."));
        }
    }

    private static void requiredBoolean(Map<String, Object> value, String key, String path, List<Issue> issues) {
        if (!value.containsKey(key)) {
            issues.add(invalid("S-I008", path + "." + key, key + " is required."));
        } else if (!(value.get(key) instanceof Boolean)) {
            issues.add(invalid("S-I024", path + "." + key, key + " must be boolean."));
        }
    }

    private static void optionalBoolean(Map<String, Object> value, String key, String path, List<Issue> issues) {
        if (value.containsKey(key) && !(value.get(key) instanceof Boolean)) {
            issues.add(invalid("S-I024", path + "." + key, key + " must be boolean when present."));
        }
    }

    private static void optionalIdentity(Map<String, Object> value, String key, String path, List<Issue> issues) {
        if (value.containsKey(key) && !identityValid(string(value.get(key)))) {
            issues.add(invalid("S-I024", path + "." + key, key + " must be a non-empty identity token when present."));
        }
    }

    private static void requiredDuration(Map<String, Object> value, String key, String path, List<Issue> issues) {
        if (!value.containsKey(key)) {
            issues.add(invalid("S-I008", path + "." + key, key + " is required."));
        } else if (!durationValid(string(value.get(key)))) {
            issues.add(invalid("S-I009", path + "." + key, key + " must be a non-negative working duration."));
        }
    }

    private static Map<String, Object> result(String command, String status, RuntimeBinding runtime, Map<String, Object> io,
            Map<String, Object> observations, List<Map<String, Object>> diagnostics, Map<String, Object> data) {
        Map<String, Object> result = linked();
        result.put("kind", "miku_project_cli_result");
        result.put("schema_version", "1");
        result.put("contract", contract());
        result.put("runtime", runtime.asMap());
        result.put("command", command);
        result.put("side_effect_class", "cli".equals(command) ? "none" : "read-only");
        result.put("status", status);
        result.put("exit_code", exitCode(status));
        result.put("io", io);
        result.put("effects", effects());
        result.put("observations", observations);
        result.put("next_action", nextAction(status, diagnostics));
        result.put("diagnostics", diagnostics);
        result.put("data", data);
        return result;
    }

    private static Map<String, Object> errorResult(String command, V1Failure failure, RuntimeBinding runtime,
            Map<String, Object> io, Map<String, Object> data) {
        return result(command, failure.status, runtime, io, observations(Collections.<Map<String, Object>>emptyList(),
                Collections.<Map<String, Object>>emptyList()), Collections.singletonList(diagnostic(failure.code, failure.message,
                failure.scope, failure.path, failure.option, failure.role, failure.ruleId, failure.retryability, failure.details)), data);
    }

    private static Map<String, Object> contract() {
        Map<String, Object> value = linked();
        value.put("product", "miku-project");
        value.put("product_contract_version", "1");
        value.put("artifact_schema", "miku_project_artifacts/v1");
        value.put("result_schema", "miku_project_cli_result/v1");
        value.put("diagnostic_schema", "miku_project_cli_diagnostic/v1");
        value.put("diagnostic_catalog_version", "1");
        return value;
    }

    private static Map<String, Object> emptyIo(Map<String, Object> target) {
        Map<String, Object> io = linked();
        io.put("stdin_option", null);
        io.put("inputs", new ArrayList<Map<String, Object>>());
        io.put("result", target);
        io.put("destination", null);
        return io;
    }

    private static Map<String, Object> projectIo(InputData input, Map<String, Object> target) {
        Map<String, Object> inputValue = linked();
        inputValue.put("role", "project");
        inputValue.put("option", "--project");
        inputValue.put("source", input.source);
        inputValue.put("path", input.path);
        inputValue.put("digest", digest(input.bytes));
        Map<String, Object> io = linked();
        io.put("stdin_option", "stdin".equals(input.source) ? "--project" : null);
        io.put("inputs", Collections.singletonList(inputValue));
        io.put("result", target);
        io.put("destination", null);
        return io;
    }

    private static Map<String, Object> unreadProjectIo(String requested, Map<String, Object> target) {
        Map<String, Object> inputValue = linked();
        inputValue.put("role", "project");
        inputValue.put("option", "--project");
        boolean stdin = "-".equals(requested);
        inputValue.put("source", stdin ? "stdin" : "file");
        inputValue.put("path", stdin ? null : Paths.get(requested).toAbsolutePath().normalize().toString());
        inputValue.put("digest", null);
        Map<String, Object> io = linked();
        io.put("stdin_option", stdin ? "--project" : null);
        io.put("inputs", Collections.singletonList(inputValue));
        io.put("result", target);
        io.put("destination", null);
        return io;
    }

    private static Map<String, Object> effects() {
        Map<String, Object> cleanup = linked();
        cleanup.put("status", "not-needed");
        cleanup.put("path", null);
        Map<String, Object> value = linked();
        value.put("project_input_modified", Boolean.FALSE);
        value.put("project_artifact", null);
        value.put("cleanup", cleanup);
        return value;
    }

    private static Map<String, Object> observations(List<Map<String, Object>> normalizations, List<Map<String, Object>> unsupported) {
        Map<String, Object> value = linked();
        value.put("normalizations", normalizations);
        value.put("losses", new ArrayList<Map<String, Object>>());
        value.put("unsupported", unsupported);
        return value;
    }

    private static Map<String, Object> validationData(boolean valid, String profile, Map<String, Object> stateDigest) {
        Map<String, Object> validation = linked();
        validation.put("valid", Boolean.valueOf(valid));
        validation.put("format_profile", profile);
        validation.put("state_digest", stateDigest);
        Map<String, Object> data = linked();
        data.put("validation", validation);
        return data;
    }

    private static Map<String, Object> nextAction(String status, List<Map<String, Object>> diagnostics) {
        Map<String, Object> value = linked();
        if ("succeeded".equals(status)) {
            value.put("action", "complete");
            value.put("command", null);
            value.put("source_retryability", null);
            return value;
        }
        String retryability = (String) diagnostics.get(0).get("retryability");
        if ("after-input-change".equals(retryability)) {
            value.put("action", "revise-invocation-or-input");
            value.put("command", null);
        } else if ("after-environment-change".equals(retryability)) {
            value.put("action", "repair-environment");
            value.put("command", null);
        } else {
            value.put("action", "abort-and-investigate");
            value.put("command", null);
        }
        value.put("source_retryability", retryability);
        return value;
    }

    private static Map<String, Object> diagnostic(String code, String message, String scope, String path, String option,
            String role, String ruleId, String retryability, Map<String, Object> details) {
        Map<String, Object> location = linked();
        location.put("scope", scope);
        location.put("path", path);
        location.put("option", option);
        location.put("artifact_role", role);
        location.put("rule_id", ruleId);
        Map<String, Object> result = linked();
        result.put("kind", "miku_project_cli_diagnostic");
        result.put("schema_version", "1");
        result.put("code", code);
        result.put("severity", "error");
        result.put("category", category(code));
        result.put("message", message);
        result.put("location", location);
        result.put("retryability", retryability);
        result.put("details", details);
        return result;
    }

    private static Map<String, Object> normalization(String code, String path, Object before, Object after) {
        Map<String, Object> value = linked();
        value.put("code", code);
        value.put("path", path);
        value.put("before", before);
        value.put("after", after);
        return value;
    }

    private static Map<String, Object> observation(String code, String path, String description) {
        Map<String, Object> value = linked();
        value.put("code", code);
        value.put("path", path);
        value.put("description", description);
        return value;
    }

    private static Map<String, Object> stdoutTarget() {
        Map<String, Object> value = linked();
        value.put("target", "stdout");
        value.put("path", null);
        return value;
    }

    private static Map<String, Object> fileTarget(Path path) {
        Map<String, Object> value = linked();
        value.put("target", "file");
        value.put("path", path.toString());
        return value;
    }

    private static int write(OutputStream stdout, Map<String, Object> result) {
        try {
            stdout.write((canonicalJson(result) + "\n").getBytes(StandardCharsets.UTF_8));
            stdout.flush();
            return ((Integer) result.get("exit_code")).intValue();
        } catch (IOException error) {
            return 3;
        }
    }

    private static final class ResultTransport {
        final Map<String, Object> target;
        final OutputStream stdout;
        final OutputStream file;
        final Path path;

        ResultTransport(Map<String, Object> target, OutputStream stdout, OutputStream file, Path path) {
            this.target = target;
            this.stdout = stdout;
            this.file = file;
            this.path = path;
        }

        int write(Map<String, Object> result) {
            if (file == null) {
                return MikuProjectV1Validate.write(stdout, result);
            }
            try {
                file.write((canonicalJson(result) + "\n").getBytes(StandardCharsets.UTF_8));
                file.close();
                return ((Integer) result.get("exit_code")).intValue();
            } catch (IOException error) {
                try { file.close(); } catch (IOException ignored) { }
                try { Files.deleteIfExists(path); } catch (IOException ignored) { }
                return 3;
            }
        }
    }

    private static Map<String, Object> digest(byte[] bytes) {
        Map<String, Object> value = linked();
        value.put("algorithm", "sha-256");
        value.put("value", sha256(bytes));
        return value;
    }

    private static Map<String, Object> digestSemanticState(Map<String, Object> state) {
        return digest(canonicalJson(canonicalSemanticState(state)).getBytes(StandardCharsets.UTF_8));
    }

    private static Map<String, Object> canonicalSemanticState(Map<String, Object> state) {
        Map<String, Object> result = deepMap(state);
        for (String collection : Arrays.asList("dependencies", "resources", "assignments", "calendars")) {
            List<Map<String, Object>> values = maps(result.get(collection));
            Collections.sort(values, semanticCollectionOrder(collection));
            result.put(collection, values);
        }
        return result;
    }

    private static Comparator<Map<String, Object>> semanticCollectionOrder(final String collection) {
        return new Comparator<Map<String, Object>>() {
            @Override public int compare(Map<String, Object> left, Map<String, Object> right) {
                if ("dependencies".equals(collection)) {
                    for (String key : Arrays.asList("predecessor_uid", "successor_uid", "type", "lag")) {
                        int compared = compareUnicode(string(left.get(key)), string(right.get(key)));
                        if (compared != 0) return compared;
                    }
                    return 0;
                }
                return compareUnicode(string(left.get("uid")), string(right.get("uid")));
            }
        };
    }

    private static String canonicalJson(Object value) {
        StringBuilder builder = new StringBuilder();
        appendJson(builder, value);
        return builder.toString();
    }

    @SuppressWarnings("unchecked")
    private static void appendJson(StringBuilder builder, Object value) {
        if (value == null) { builder.append("null"); return; }
        if (value instanceof Boolean || value instanceof Integer || value instanceof Long) { builder.append(value); return; }
        if (value instanceof String) { quote(builder, (String) value); return; }
        if (value instanceof List<?>) {
            builder.append('[');
            List<Object> values = (List<Object>) value;
            for (int index = 0; index < values.size(); index++) { if (index > 0) builder.append(','); appendJson(builder, values.get(index)); }
            builder.append(']'); return;
        }
        if (value instanceof Map<?, ?>) {
            List<String> keys = new ArrayList<String>();
            for (Object key : ((Map<?, ?>) value).keySet()) keys.add((String) key);
            Collections.sort(keys, UNICODE_ORDER);
            builder.append('{');
            for (int index = 0; index < keys.size(); index++) {
                if (index > 0) builder.append(','); quote(builder, keys.get(index)); builder.append(':'); appendJson(builder, ((Map<String, Object>) value).get(keys.get(index)));
            }
            builder.append('}'); return;
        }
        throw new IllegalArgumentException("Unsupported canonical JSON value: " + value.getClass());
    }

    private static void quote(StringBuilder builder, String value) {
        builder.append('"');
        for (int index = 0; index < value.length(); index++) {
            char c = value.charAt(index);
            switch (c) {
            case '"': builder.append("\\\""); break;
            case '\\': builder.append("\\\\"); break;
            case '\b': builder.append("\\b"); break;
            case '\t': builder.append("\\t"); break;
            case '\n': builder.append("\\n"); break;
            case '\f': builder.append("\\f"); break;
            case '\r': builder.append("\\r"); break;
            default:
                if (c <= 0x1f) builder.append(String.format("\\u%04x", Integer.valueOf(c))); else builder.append(c);
            }
        }
        builder.append('"');
    }

    private static final Comparator<String> UNICODE_ORDER = new Comparator<String>() {
        @Override public int compare(String left, String right) { return compareUnicode(left, right); }
    };
    private static final Comparator<Issue> ISSUE_ORDER = new Comparator<Issue>() {
        @Override public int compare(Issue left, Issue right) {
            int value = compareUnicode(left.code, right.code);
            if (value != 0) return value;
            value = compareUnicode(left.path, right.path);
            return value != 0 ? value : compareUnicode(left.ruleId, right.ruleId);
        }
    };

    private static int compareUnicode(String left, String right) {
        if (left == null && right == null) return 0;
        if (left == null) return -1;
        if (right == null) return 1;
        int leftIndex = 0, rightIndex = 0;
        while (leftIndex < left.length() && rightIndex < right.length()) {
            int leftCodePoint = left.codePointAt(leftIndex), rightCodePoint = right.codePointAt(rightIndex);
            if (leftCodePoint != rightCodePoint) return leftCodePoint < rightCodePoint ? -1 : 1;
            leftIndex += Character.charCount(leftCodePoint); rightIndex += Character.charCount(rightCodePoint);
        }
        return leftIndex == left.length() && rightIndex == right.length() ? 0 : leftIndex == left.length() ? -1 : 1;
    }

    private static boolean startsWithBom(byte[] bytes) { return bytes.length >= 3 && bytes[0] == (byte) 0xef && bytes[1] == (byte) 0xbb && bytes[2] == (byte) 0xbf; }
    private static String localName(Element element) { return element.getLocalName() == null ? element.getNodeName() : element.getLocalName(); }
    private static String snake(String value) { return value.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(); }
    private static String identity(String value) { return value; }
    /** Mirrors Node Number.isSafeInteger: malformed or unsafe lexical numbers remain semantic input. */
    private static Object integer(String value) { try { if (value == null || !INTEGER.matcher(value).matches()) return value; long parsed=Long.parseLong(value); return parsed <= MAX_SAFE_INTEGER ? Long.valueOf(parsed) : value; } catch (NumberFormatException error) { return value; } }
    /** Mirrors the Node adapter: malformed lexical booleans remain visible to semantic validation. */
    private static Object booleanValue(String value) { return "0".equals(value) ? Boolean.FALSE : "1".equals(value) ? Boolean.TRUE : value; }
    private static Long integerValue(Object value) { return value instanceof Long ? (Long) value : null; }
    private static String duration(String value) { if (value == null) return null; Matcher matcher = Pattern.compile("PT([0-9]+)H([0-9]+)M([0-9]+)S").matcher(value); if (!matcher.matches()) return value; try { long hours=Long.parseLong(matcher.group(1)); int minutes=Integer.parseInt(matcher.group(2)), seconds=Integer.parseInt(matcher.group(3)); if (hours > MAX_SAFE_INTEGER || minutes > 59 || seconds > 59) return "invalid-duration"; return "PT" + hours + "H" + minutes + "M" + seconds + "S"; } catch (NumberFormatException error) { return "invalid-duration"; } }
    private static String units(String value) { if (value == null || !UNITS.matcher(value).matches()) return value; try { return new BigDecimal(value).stripTrailingZeros().toPlainString(); } catch (NumberFormatException error) { return value; } }
    private static boolean identityValid(String value) { return value != null && IDENTITY.matcher(value).matches(); }
    private static boolean textValid(String value) { return value != null && value.length() > 0; }
    private static boolean durationValid(String value) { return value != null && DURATION.matcher(value).matches(); }
    private static boolean unitsValid(String value) { return value != null && UNITS.matcher(value).matches(); }
    private static boolean date(String value) { if (value == null) return false; Matcher matcher = DATETIME.matcher(value); if (!matcher.matches()) return false; try { int year=Integer.parseInt(matcher.group(1)), month=Integer.parseInt(matcher.group(2)), day=Integer.parseInt(matcher.group(3)), hour=Integer.parseInt(matcher.group(4)), minute=Integer.parseInt(matcher.group(5)), second=Integer.parseInt(matcher.group(6)); return month>=1 && month<=12 && day>=1 && day<=days(year,month) && hour<=23 && minute<=59 && second<=59; } catch (NumberFormatException error) { return false; } }
    private static int days(int year, int month) { if (month == 2) return year % 4 == 0 && (year % 100 != 0 || year % 400 == 0) ? 29 : 28; return month == 4 || month == 6 || month == 9 || month == 11 ? 30 : 31; }
    private static String taskPath(String uid, int index) { return identityValid(uid) ? "tasks[uid=" + uid + "]" : "tasks[" + index + "]"; }
    private static String dependencyPath(String predecessor, String successor, int index) { return identityValid(predecessor) && identityValid(successor) ? "dependencies[predecessor_uid=" + predecessor + ",successor_uid=" + successor + "]" : "dependencies[" + index + "]"; }
    private static String memberPath(String domain, String uid, int index) { return identityValid(uid) ? domain + "[uid=" + uid + "]" : domain + "[" + index + "]"; }
    private static String resourcePath(String uid, int index) { return memberPath("resources", uid, index); }
    private static String assignmentPath(String uid, int index) { return memberPath("assignments", uid, index); }
    private static String calendarPath(String uid, int index) { return memberPath("calendars", uid, index); }
    private static String joinOutline(List<Integer> values) { StringBuilder builder = new StringBuilder(); for (int index=0; index<values.size(); index++) { if(index>0) builder.append('.'); builder.append(values.get(index)); } return builder.toString(); }
    private static void copy(Map<String, Object> target, String key, Object value) { if (value != null) target.put(key, value); }
    private static String string(Object value) { return value instanceof String ? (String) value : null; }
    private static boolean same(String left, String right) { return left == null ? right == null : left.equals(right); }
    private static boolean hasCycle(Map<String, Set<String>> adjacency) { Set<String> visited=new HashSet<String>(), active=new HashSet<String>(); for (String uid:adjacency.keySet()) if (hasCycle(uid, adjacency, visited, active)) return true; return false; }
    private static boolean hasCycle(String uid, Map<String, Set<String>> adjacency, Set<String> visited, Set<String> active) { if (active.contains(uid)) return true; if (!visited.add(uid)) return false; active.add(uid); Set<String> successors=adjacency.get(uid); if (successors != null) for (String successor:successors) if (hasCycle(successor, adjacency, visited, active)) return true; active.remove(uid); return false; }
    @SuppressWarnings("unchecked") private static Map<String, Object> object(Object value) { return value instanceof Map<?, ?> ? (Map<String, Object>) value : linked(); }
    @SuppressWarnings("unchecked") private static List<Map<String, Object>> maps(Object value) { return value instanceof List<?> ? (List<Map<String, Object>>) value : new ArrayList<Map<String, Object>>(); }
    @SuppressWarnings("unchecked") private static Map<String, Object> deepMap(Map<String, Object> source) { Map<String,Object> copy=linked(); for(Map.Entry<String,Object> entry:source.entrySet()) { Object value=entry.getValue(); if(value instanceof Map<?,?>) copy.put(entry.getKey(), deepMap((Map<String,Object>) value)); else if(value instanceof List<?>) { List<Object> list=new ArrayList<Object>(); for(Object item:(List<Object>)value) list.add(item instanceof Map<?,?> ? deepMap((Map<String,Object>)item):item); copy.put(entry.getKey(),list); } else copy.put(entry.getKey(),value); } return copy; }
    private static Map<String, Object> linked() { return new LinkedHashMap<String, Object>(); }
    private static Set<String> setOf(String... values) { return new HashSet<String>(Arrays.asList(values)); }
    private static Map<String, Object> details(String key, Object value) { Map<String,Object> details=linked(); details.put(key,value); return details; }
    private static String category(String code) {
        String prefix = code.substring(0, code.indexOf('.'));
        if ("cli".equals(prefix)) return "usage";
        if ("text".equals(prefix)) return "encoding";
        return prefix;
    }
    private static int exitCode(String status) { return "succeeded".equals(status) ? 0 : "rejected".equals(status) ? 1 : "usage-error".equals(status) ? 2 : 3; }
    private static String sha256(byte[] bytes) { try { byte[] digest=MessageDigest.getInstance("SHA-256").digest(bytes); StringBuilder builder=new StringBuilder(); for(byte value:digest) builder.append(String.format("%02x", Integer.valueOf(value & 0xff))); return builder.toString(); } catch (NoSuchAlgorithmException error) { throw new IllegalStateException(error); } }
    private static byte[] readAll(InputStream input) throws IOException { ByteArrayOutputStream output=new ByteArrayOutputStream(); byte[] buffer=new byte[8192]; for(int read;(read=input.read(buffer))!=-1;) output.write(buffer,0,read); return output.toByteArray(); }
    private static Issue invalid(String ruleId, String path, String message) { return new Issue("semantic.invalid", ruleId, path, message); }
    private static Issue unsupported(String ruleId, String path, String message) { return new Issue("semantic.unsupported", ruleId, path, message); }
    private static V1Failure usage(String code, String message, String option, String path) { return failure(code,option == null ? "command" : "option",message,path,option,null,"after-input-change",Collections.<String,Object>emptyMap(),"usage-error"); }
    private static V1Failure failure(String code,String scope,String message,String path,String option,String role,String retryability,Map<String,Object> details,String status) { return new V1Failure(code,scope,message,path,option,role,null,retryability,details,status); }

    private static final class Invocation { final String project, result; Invocation(String project,String result){this.project=project;this.result=result;} }
    private static final class InputData { final String source,path; final byte[] bytes; InputData(String source,String path,byte[] bytes){this.source=source;this.path=path;this.bytes=bytes;} }
    private static final class TaskBundle { final List<Map<String,Object>> tasks,dependencies; TaskBundle(List<Map<String,Object>> tasks,List<Map<String,Object>> dependencies){this.tasks=tasks;this.dependencies=dependencies;} }
    private static final class TaskSource { String uid,name,outlineNumber,start,finish,duration,calendarUid; Object id,level,percent,milestone,summary; List<Map<String,Object>> dependencies; }
    private static final class DecodedProject { final Map<String,Object> state; final List<Issue> issues; final List<Map<String,Object>> normalizations; DecodedProject(Map<String,Object> state,List<Issue> issues,List<Map<String,Object>> normalizations){this.state=state;this.issues=issues;this.normalizations=normalizations;} }
    private static final class Validation { final boolean valid; final List<Issue> issues; Validation(boolean valid,List<Issue> issues){this.valid=valid;this.issues=issues;} }
    private static final class Issue { final String code,ruleId,path,message; final Map<String,Object> details; Issue(String code,String ruleId,String path,String message){this.code=code;this.ruleId=ruleId;this.path=path;this.message=message;this.details=details("rule_id",ruleId);} }
    private static final class V1Failure extends Exception { final String code,scope,message,path,option,role,ruleId,retryability,status; final Map<String,Object> details; V1Failure(String code,String scope,String message,String path,String option,String role,String ruleId,String retryability,Map<String,Object> details,String status){this.code=code;this.scope=scope;this.message=message;this.path=path;this.option=option;this.role=role;this.ruleId=ruleId;this.retryability=retryability;this.details=details;this.status=status;} }
    private static final class RuntimeBinding { final boolean verified; final String version,artifactDigest,manifestDigest; RuntimeBinding(boolean verified,String version,String artifactDigest,String manifestDigest){this.verified=verified;this.version=version;this.artifactDigest=artifactDigest;this.manifestDigest=manifestDigest;} static RuntimeBinding verifiedOrUnverified(VerifiedRuntime runtime){return runtime!=null && validRuntime(runtime) ? new RuntimeBinding(true,runtime.version,runtime.artifactDigest,runtime.manifestDigest):unverified(runtime);} static RuntimeBinding unverified(VerifiedRuntime runtime){return new RuntimeBinding(false,runtime==null||runtime.version==null||runtime.version.length()==0?"unknown":runtime.version,null,null);} static boolean validRuntime(VerifiedRuntime value){return value.version!=null&&!value.version.isEmpty()&&hex(value.artifactDigest)&&hex(value.manifestDigest);} Map<String,Object> asMap(){Map<String,Object> value=linked();value.put("binding_status",verified?"verified":"unverified");value.put("family","java");value.put("version",version);value.put("artifact_digest",verified?digestFromValue(artifactDigest):null);value.put("manifest_digest",verified?digestFromValue(manifestDigest):null);value.put("capability_profile",verified?CAPABILITY:null);value.put("fixture_suite_version",verified?FIXTURE_SUITE_VERSION:null);return value;} static boolean hex(String value){return value!=null&&value.matches("[0-9a-f]{64}");} static Map<String,Object> digestFromValue(String raw){Map<String,Object> value=linked();value.put("algorithm","sha-256");value.put("value",raw);return value;} }
}
